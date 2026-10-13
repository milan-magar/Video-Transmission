package com.stream.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class StreamServer {
    public static final String ENDPOINT = "/upload";
    public static final int MAX_FRAME_BYTES = 4 * 1024 * 1024; // 4 MB

    private final int port;
    private final VideoWindow window;
    private HttpServer server;
    private final AtomicLong frameCount = new AtomicLong(0);
    private final AtomicLong lastLogTime = new AtomicLong(System.currentTimeMillis());

    public StreamServer(int port, VideoWindow window) {
        this.port = port;
        this.window = window;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(ENDPOINT, new UploadHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("[StreamServer] Listening on port " + port + ENDPOINT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[StreamServer] Stopped.");
        }
    }

    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String remote = exchange.getRemoteAddress().getAddress().getHostAddress();

            if (!"POST".equalsIgnoreCase(method)) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            byte[] data;
            try (InputStream is = exchange.getRequestBody();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                long total = 0;
                while ((read = is.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_FRAME_BYTES) {
                        sendResponse(exchange, 413, "Payload Too Large");
                        return;
                    }
                    baos.write(buffer, 0, read);
                }
                data = baos.toByteArray();
            }

            if (data.length == 0) {
                sendResponse(exchange, 400, "Empty body");
                return;
            }

            BufferedImage image = null;
            try {
                image = ImageIO.read(new ByteArrayInputStream(data));
            } catch (Exception ignored) {}

            if (image == null) {
                sendResponse(exchange, 400, "Invalid JPEG");
                return;
            }

            window.updateImage(image);

            long now = System.currentTimeMillis();
            long last = lastLogTime.get();
            if (now - last > 1000) {
                if (lastLogTime.compareAndSet(last, now)) {
                    long count = frameCount.incrementAndGet();
                    System.out.printf("[StreamServer] Frame #%d from %s (%d bytes, %dx%d)%n",
                            count, remote, data.length, image.getWidth(), image.getHeight());
                }
            } else {
                frameCount.incrementAndGet();
            }

            sendResponse(exchange, 200, "OK");
        }

        private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes("UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}