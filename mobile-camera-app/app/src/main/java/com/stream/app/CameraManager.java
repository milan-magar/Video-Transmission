package com.stream.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraManager {

    public interface FrameCallback {
        void onFrame(byte[] jpegBytes);
    }

    private static final int TARGET_FPS = 15;
    private static final long FRAME_INTERVAL_MS = 1000 / TARGET_FPS;
    private static final int JPEG_QUALITY = 60;

    private final Context context;
    private final PreviewView previewView;
    private final FrameCallback frameCallback;
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();

    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private long lastFrameTime = 0;

    public CameraManager(Context context, PreviewView previewView, FrameCallback frameCallback) {
        this.context = context;
        this.previewView = previewView;
        this.frameCallback = frameCallback;
    }

    public void start() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindUseCases();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalysis.setAnalyzer(analysisExecutor, imageProxy -> {
            long now = System.currentTimeMillis();
            if (now - lastFrameTime < FRAME_INTERVAL_MS) {
                imageProxy.close();
                return;
            }
            lastFrameTime = now;

            byte[] jpeg = imageProxyToJpeg(imageProxy);
            imageProxy.close();

            if (jpeg != null && frameCallback != null) {
                frameCallback.onFrame(jpeg);
            }
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] imageProxyToJpeg(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes.length == 0) return null;

        ByteBuffer buffer = planes[0].getBuffer();
        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        int rowStride = planes[0].getRowStride();
        int pixelStride = planes[0].getPixelStride();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (rowStride == width * pixelStride) {
            bitmap.copyPixelsFromBuffer(buffer);
        } else {
            byte[] rowData = new byte[rowStride];
            byte[] pixelData = new byte[width * height * 4];
            int offset = 0;
            for (int row = 0; row < height; row++) {
                buffer.position(row * rowStride);
                buffer.get(rowData, 0, Math.min(rowStride, buffer.remaining()));
                System.arraycopy(rowData, 0, pixelData, offset, width * pixelStride);
                offset += width * pixelStride;
            }
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(pixelData));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        bitmap.recycle();
        return baos.toByteArray();
    }

    public void stop() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        analysisExecutor.shutdown();
    }
}