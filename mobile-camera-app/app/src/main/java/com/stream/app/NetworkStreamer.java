package com.stream.app;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkStreamer {

    private static final String TAG = "NetworkStreamer";
    private static final MediaType JPEG = MediaType.parse("image/jpeg");

    private final OkHttpClient client;
    private final AtomicBoolean sending = new AtomicBoolean(false);

    private String targetIp;
    private int targetPort;

    public NetworkStreamer() {
        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    public void setTarget(String ip, int port) {
        this.targetIp = ip;
        this.targetPort = port;
    }

    public void sendFrame(byte[] jpegBytes) {
        if (targetIp == null || targetPort <= 0) return;

        // Drop frame if a previous one is still in flight
        if (!sending.compareAndSet(false, true)) {
            return;
        }

        String url = "http://" + targetIp + ":" + targetPort + "/upload";
        RequestBody body = RequestBody.create(jpegBytes, JPEG);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, "Failed to send frame: " + e.getMessage());
                sending.set(false);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
                        Log.w(TAG, "Server returned " + response.code());
                    }
                } finally {
                    response.close();
                    sending.set(false);
                }
            }
        });
    }
}