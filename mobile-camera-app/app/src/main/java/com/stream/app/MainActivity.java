package com.stream.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private EditText etIp, etPort;
    private Button btnStart, btnStop;
    private PreviewView previewView;

    private CameraManager cameraManager;
    private NetworkStreamer networkStreamer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etIp = findViewById(R.id.etIp);
        etPort = findViewById(R.id.etPort);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        previewView = findViewById(R.id.previewView);

        networkStreamer = new NetworkStreamer();

        btnStart.setOnClickListener(v -> startStreaming());
        btnStop.setOnClickListener(v -> stopStreaming());
    }

    private void startStreaming() {
        String ip = etIp.getText().toString().trim();
        String portStr = etPort.getText().toString().trim();
        if (ip.isEmpty()) {
            Toast.makeText(this, "Please enter laptop IP", Toast.LENGTH_SHORT).show();
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid port", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
            return;
        }

        startCameraAndStream(ip, port);
    }

    private void startCameraAndStream(String ip, int port) {
        networkStreamer.setTarget(ip, port);
        cameraManager = new CameraManager(this, previewView, networkStreamer::sendFrame);
        cameraManager.start();

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        etIp.setEnabled(false);
        etPort.setEnabled(false);
    }

    private void stopStreaming() {
        if (cameraManager != null) {
            cameraManager.stop();
            cameraManager = null;
        }
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        etIp.setEnabled(true);
        etPort.setEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStreaming();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStreaming();
    }
}