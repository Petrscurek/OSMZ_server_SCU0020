package com.vsb.camserver;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TimerTask;

public class CameraActivity extends Activity {

    private Camera camera;
    private FrameLayout layout;
    private CameraPreviewView view;

    private boolean ready = true;

    public static byte[] bytes;
    public static boolean streaming = true;

    // Stuff
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_camera);

        this.layout = findViewById(R.id.camera_preview);

        Button buttonStart = findViewById(R.id.startStream);
        Button buttonStop = findViewById(R.id.stopStream);
        Button buttonSnap = findViewById(R.id.takeSnapshot);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "Ready", Toast.LENGTH_SHORT).show();
            Logger.LOGGER.add("Camera ready");
        } else {
            Toast.makeText(this, "Not ready", Toast.LENGTH_SHORT).show();
            Logger.LOGGER.add("Camera not ready");
        }

        this.camera = Camera.open();
        this.view = new CameraPreviewView(this, this.camera);
        this.layout.addView(this.view);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                task.run();
            }
        });

        buttonSnap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ready) {
                    camera.takePicture(null, null, onPictureTaken);
                    ready = false;
                }
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streaming = false;
            }
        });
    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            camera.setPreviewCallback(onPreview);
        }
    };

    Camera.PictureCallback onPictureTaken = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera c) {
            File file = getMediaFile();

            if (file == null) {
                ready = true;
            } else {
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(data);
                    fos.close();

                    camera.startPreview();
                    ready = true;
                } catch (IOException e) {
                    Logger.LOGGER.add("Picture error: " + e.toString());
                    e.printStackTrace();
                }
            }
        }
    };

    Camera.PreviewCallback onPreview = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera c) {
            bytes = toImage(data, c);

            try {
                camera.startPreview();
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Logger.LOGGER.add("Preview error: " + e.toString());
                e.printStackTrace();
            }

            ready = true;
        }
    };

    private File getMediaFile() {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return null;
        } else {
            File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "snap");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File out = new File(folder, "snap.jpg");
            return out;
        }
    }

    private static byte[] toImage(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new YuvImage(data, parameters.getPreviewFormat(), size.width, size.height, null).compressToJpeg(new Rect(0, 0, size.width, size.height), 100, out);

        return out.toByteArray();
    }

}
