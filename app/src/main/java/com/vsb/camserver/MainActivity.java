package com.vsb.camserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private SocketServer server;
    private Handler handler;
    private long totalSize;
    private MainActivity self;

    TextView v1, v2, v3, v4;
    EditText e1;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        this.self = this;

        Button serverStart = findViewById(R.id.button1);
        Button serverStop = findViewById(R.id.button2);
        Button cameraOpen = findViewById(R.id.cameraActivityBtn);

        v1 = findViewById(R.id.textView);
        v2 = findViewById(R.id.textView2);
        v3 = findViewById(R.id.textView3);
        v4 = findViewById(R.id.textView10);

        e1 = findViewById(R.id.maxThreads);

        this.handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Bundle b = message.getData();

                String path = b.getString("path", "");
                String type = b.getString("type", "");
                long size = b.getLong("size", 0);

                totalSize += size;
                v1.setText(path);
                v2.setText(Long.toString(size));
                v3.setText(Long.toString(totalSize));
                v4.setText(type);
            }
        };

        serverStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int perms = ContextCompat.checkSelfPermission(self, Manifest.permission.READ_EXTERNAL_STORAGE);
                if (perms != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(self, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 1);
                } else {
                    if (e1.getText().toString().isEmpty()) {
                        Toast.makeText(self, R.string.thread_count_empty, Toast.LENGTH_SHORT).show();

                        server = new SocketServer(handler, 1);
                        server.start();
                    } else {
                        Toast.makeText(self, R.string.thread_count_ok, Toast.LENGTH_SHORT).show();

                        server = new SocketServer(handler, Integer.parseInt(e1.getText().toString()));
                        server.start();
                    }
                }
            }
        });

        serverStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (server != null && server.getServerState()) {
                    Toast.makeText(self, R.string.server_stopped, Toast.LENGTH_SHORT).show();

                    try {
                        server.close();
                        server.join();
                    } catch (InterruptedException e) {
                        Logger.LOGGER.add("Could not join threads: " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        });

        cameraOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), CameraActivity.class));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int rc, String[] perms, int[] res) {
        if (rc == 1) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
                if (e1.getText().toString().isEmpty()) {
                    Toast.makeText(self, R.string.thread_count_empty, Toast.LENGTH_SHORT).show();

                    server = new SocketServer(handler, 1);
                    server.start();
                } else {
                    Toast.makeText(self, R.string.thread_count_ok, Toast.LENGTH_SHORT).show();

                    server = new SocketServer(handler, Integer.parseInt(e1.getText().toString()));
                    server.start();
                }
            }
        }
    }
}
