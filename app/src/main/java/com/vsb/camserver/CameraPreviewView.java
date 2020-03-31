package com.vsb.camserver;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraPreviewView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder holder;
    private Camera camera;

    // Constructor
    public CameraPreviewView(Context context, Camera camera) {
        super(context);

        this.camera = camera;

        this.holder = getHolder();
        this.holder.addCallback(this);
        this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    // Overrides
    @Override
    public void surfaceCreated(SurfaceHolder h) {
        Camera.Parameters parameters = this.camera.getParameters();
        int orientation = this.getResources().getConfiguration().orientation;

        if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation", "portrait");
            this.camera.setDisplayOrientation(90);
            parameters.setRotation(90);
        } else {
            parameters.set("orientation", "landscape");
            this.camera.setDisplayOrientation(90);
            parameters.setRotation(0);
        }

        this.camera.setParameters(parameters);

        try {
            this.camera.setPreviewDisplay(h);
            this.camera.startPreview();
        } catch (IOException e) {
            Logger.LOGGER.add("Camera preview view error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
