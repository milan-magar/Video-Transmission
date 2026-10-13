package com.stream.server;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class Main {

    private static final int DEFAULT_PORT = 8080;

    private Main() {}

    public static void main(String[] args) {
        final int port = parsePort(args);
        printBanner(port);

        final VideoWindow window = new VideoWindow("Live Stream Receiver  -  port " + port);
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                SwingUtilities.updateComponentTreeUI(window);
            } catch (Exception ignored) {}
            window.setVisible(true);
        });

        final StreamServer server = new StreamServer(port, window);
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("[Main] FATAL: could not start HTTP server on port " + port);
            e.printStackTrace();
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Main] Shutting down...");
            server.stop();
        }, "server-shutdown-hook"));

        System.out.println("\n[Main] Server is running. Enter one of the IPs above into the Android app.");
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) return DEFAULT_PORT;
        try {
            int p = Integer.parseInt(args[0].trim());
            if (p < 1 || p > 65535) throw new NumberFormatException();
            return p;
        } catch (NumberFormatException e) {
            System.err.println("[Main] Invalid port, using " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    private static void printBanner(int port) {
        System.out.println("==========================================================");
        System.out.println("  LAPTOP VIDEO SERVER  -  pure Java HTTP receiver");
        System.out.println("==========================================================");
        System.out.println("  Endpoint : POST http://<this-ip>:" + port + StreamServer.ENDPOINT);
        System.out.println("  Max frame: " + (StreamServer.MAX_FRAME_BYTES / 1024) + " KB");
        System.out.println("----------------------------------------------------------");
        System.out.println("  Local IPv4 addresses (pick the Wi-Fi one):");

        List<String> addresses = listLocalIpv4Addresses();
        if (addresses.isEmpty()) {
            System.out.println("    (none found)");
        } else {
            for (String addr : addresses) System.out.println("    -> " + addr);
        }
        System.out.println("==========================================================");
    }

    private static List<String> listLocalIpv4Addresses() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return result;
            for (NetworkInterface nic : Collections.list(interfaces)) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) continue;
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        result.add(address.getHostAddress() + "   (" + nic.getDisplayName() + ")");
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("[Main] Could not enumerate interfaces: " + e.getMessage());
        }
        return result;
    }
}