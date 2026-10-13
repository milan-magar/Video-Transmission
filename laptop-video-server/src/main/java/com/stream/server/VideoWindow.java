package com.stream.server;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class VideoWindow extends JFrame {
    private final ImagePanel imagePanel;

    public VideoWindow(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        imagePanel = new ImagePanel();
        add(imagePanel, BorderLayout.CENTER);
    }

    public void updateImage(BufferedImage image) {
        imagePanel.setImage(image);
    }

    private static class ImagePanel extends JPanel {
        private volatile BufferedImage image;

        ImagePanel() {
            setBackground(Color.BLACK);
            setDoubleBuffered(true);
        }

        void setImage(BufferedImage img) {
            this.image = img;
            SwingUtilities.invokeLater(this::repaint);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            BufferedImage img = this.image;
            if (img == null) {
                g.setColor(Color.GRAY);
                g.setFont(new Font("SansSerif", Font.BOLD, 20));
                String msg = "Waiting for stream...";
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(msg)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g.drawString(msg, x, y);
                return;
            }
            int panelW = getWidth();
            int panelH = getHeight();
            int imgW = img.getWidth();
            int imgH = img.getHeight();
            double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
            int drawW = (int) (imgW * scale);
            int drawH = (int) (imgH * scale);
            int x = (panelW - drawW) / 2;
            int y = (panelH - drawH) / 2;
            g.drawImage(img, x, y, drawW, drawH, null);
        }
    }
}