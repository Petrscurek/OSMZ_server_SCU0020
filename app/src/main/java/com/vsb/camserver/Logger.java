package com.vsb.camserver;

import android.util.Log;

public class Logger {

    public static final Logger LOGGER = new Logger();
    public static final String TAG = "CamServer";

    public void add (String message) {
        Log.e(TAG, message);
    }
}
