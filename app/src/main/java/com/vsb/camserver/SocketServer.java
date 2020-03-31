package com.vsb.camserver;

import android.os.Handler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class SocketServer extends Thread {

    // Statics
    public static final int PORT = 1234;

    // Socket stuff
    private ServerSocket socket;
    private Handler handler;
    private Semaphore semaphore;
    private boolean state;

    // Constructor
    public SocketServer (Handler handler, int threads) {
        this.handler = handler;
        this.semaphore = new Semaphore(threads);
    }

    // Functions
    public void close () {
        if (this.state) {
            try {
                Logger.LOGGER.add("Stopping server");
                this.socket.close();
            } catch (IOException e) {
                Logger.LOGGER.add("Error: " + e.toString());
                e.printStackTrace();
            }

            this.state = false;
        }
    }

    public void run () {
        try {
            Logger.LOGGER.add("Creating socket");
            this.socket = new ServerSocket(PORT);
            this.state = true;

            while (this.state) {
                Logger.LOGGER.add("Avaiting connection");

                Socket socket = this.socket.accept();
                if (this.semaphore.tryAcquire()) {
                    new ClientThreads(socket, this.handler, this.semaphore).start();
                    Logger.LOGGER.add("Semaphore start");
                } else {
                    Logger.LOGGER.add("Semaphore filled");
                }
            }
        } catch (IOException e) {
            if (this.socket != null && this.socket.isClosed()) {
                Logger.LOGGER.add("Server stopped");
            } else {
                Logger.LOGGER.add("Error: " + e.toString());
                e.printStackTrace();
            }
        } finally {
            this.socket = null;
            this.state = false;
        }
    }

    public boolean getServerState () {
        return this.state;
    }

}
