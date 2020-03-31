package com.vsb.camserver;


import android.icu.util.Output;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.MimeTypeMap;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

import static com.vsb.camserver.CameraActivity.bytes;
import static com.vsb.camserver.CameraActivity.streaming;

public class ClientThreads extends Thread {

    private Socket socket;
    private Handler handler;
    private Semaphore semaphore;

    private static final String CAMERA = "/camera/snapshot";
    private static final String STREAM = "/camera/stream";
    private static final String BIN = "/cgi-bin";

    public ClientThreads(Socket socket, Handler handler, Semaphore semaphore) {
        this.socket = socket;
        this.handler = handler;
        this.semaphore = semaphore;
    }

    public static String getMimeType(String url) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(url);
        if (ext != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        } else {
            return null;
        }
    }

    public void run() {
        try {
            Logger.LOGGER.add("Socket accepted");

            OutputStream os = this.socket.getOutputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            String tmp = in.readLine();
            if (tmp != null && !tmp.isEmpty()) {
                String[] tokens = tmp.split(" ");
                if (tokens.length >= 2) {
                    String type = tokens[0];
                    String uri = tokens[1];

                    if (uri.contains(BIN) && uri.length() > 9) {
                        String command = uri.substring(9);
                        String[] commands = command.split("%20");

                        if (commands.length > 0) {
                            Process process = new ProcessBuilder(Arrays.asList(commands)).start();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1);

                            int c;
                            while ((c = reader.read()) != -1) {
                                os.write(c);
                            }

                            os.flush();
                            process.destroy();
                        }
                    }

                    if (uri.contains(CAMERA)) {
                        if (bytes != null) {
                            out.flush();
                            out.write("HTTP/1.0 200 OK\nContent-Type: image/jpeg\n\n");
                            out.flush();
                            os.write(bytes);
                            os.flush();
                        }
                    }

                    if (uri.contains(STREAM)) {
                        if (bytes != null) {
                            out.flush();
                            out.write("HTTP/1.0 200 OK\nContent-Type: multipart/x-mixed-replace; boundary=\"OSMZ_boundary\"\n\n");

                            while (true) {
                                out.flush();
                                out.write("--OSMZ_boundary\nContent-Type: image/jpeg\n\n");
                                out.flush();

                                os.write(bytes);
                                os.flush();

                                if (!streaming) {
                                    bytes = null;
                                    break;
                                }
                            }

                            out.write("--OSMZ_boundary");
                            out.flush();

                            os.flush();
                        }
                    }

                    String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                    File file = new File(path + uri);

                    Bundle bundle = new Bundle();
                    Message message = new Message();

                    bundle.putString("type", type);
                    bundle.putString("path", path + uri);
                    bundle.putLong("size", file.length());

                    message.setData(bundle);

                    this.handler.sendMessage(message);

                    if (!file.exists() && !uri.contains(BIN)) {
                        out.write("HTTP/1.0 404 Not found\nContent-Type: text/html\n\n<html><body>Not found!</body></html>");
                    } else {
                        if (file.isFile()) {
                            out.write("HTTP/1.0 200 OK\nContent-Type: " + getMimeType(file.getAbsolutePath()) + "\nContent-Type: " + file.length() + "\n\n");
                            out.flush();

                            FileInputStream is = new FileInputStream(file);
                            byte[] array = new byte[(int) file.length()];

                            is.read(array);
                            os.write(array);
                        } else if (!uri.contains(BIN)) {
                            File[] files = new File(path + uri).listFiles();

                            StringBuilder builder = new StringBuilder();
                            for (File f : files) {
                                builder.append(f.getName());
                                builder.append("<br/>");
                            }

                            Logger.LOGGER.add(builder.toString());

                            out.write("HTTP/1.0 404 Not found\nContent-Type: text/html\n\n<html><body>" + builder.toString() + "</body></html>");
                        }
                    }
                }
            }

            out.flush();
            os.flush();

            this.socket.close();

            Logger.LOGGER.add("Server closed");
        } catch (IOException e) {
            if (this.socket != null && this.socket.isClosed()) {
                Logger.LOGGER.add("Server stopped");
            } else {
                Logger.LOGGER.add("Error: " + e.toString());
                e.printStackTrace();
            }
        } finally {
            this.socket = null;
            this.semaphore.release();
        }
    }
}

