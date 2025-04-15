/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team5.senior_project;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;

public class Transition {
    private Timer timer;
    private int DURATION = 2500;
    private final int UPDATE = 40;

    public enum Direction {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }

    public void doTransition(BufferedImage prevImage, BufferedImage nextImage, JLabel panel, TransitionType type, int duration) {
        // First, create scaled versions that fit the label’s container with a black background.
        int containerWidth = panel.getWidth();
        int containerHeight = panel.getHeight();
        BufferedImage scaledPrev = getScaledImage(prevImage, containerWidth, containerHeight);
        BufferedImage scaledNext = getScaledImage(nextImage, containerWidth, containerHeight);

        // Set the duration for the transition.
        if (duration > 0) {
            DURATION = duration;
        } else {
            DURATION = 2500; // Default duration
        }

        switch (type) {
            case INSTANT:
                panel.setIcon(new ImageIcon(scaledNext));
                break;
            case CROSS_FADE:
                doCrossFadeTrans(scaledPrev, scaledNext, panel);
                break;
            case WIPE_UP:
                doWipeTrans(scaledPrev, scaledNext, panel, Direction.UP);
                break;
            case WIPE_RIGHT:
                doWipeTrans(scaledPrev, scaledNext, panel, Direction.RIGHT);
                break;
            case WIPE_DOWN:
                doWipeTrans(scaledPrev, scaledNext, panel, Direction.DOWN);
                break;
            case WIPE_LEFT:
                doWipeTrans(scaledPrev, scaledNext, panel, Direction.LEFT);
                break;
        }
    }

    // Helper method to scale an image to the container dimensions while preserving the aspect ratio.
    // It creates a new image of the container size, fills it with black, and centers the scaled image.
    private BufferedImage getScaledImage(BufferedImage src, int containerWidth, int containerHeight) {
        int imgWidth = src.getWidth();
        int imgHeight = src.getHeight();
        double scale = Math.min((double) containerWidth / imgWidth, (double) containerHeight / imgHeight);
        int newWidth = (int) (imgWidth * scale);
        int newHeight = (int) (imgHeight * scale);

        BufferedImage scaled = new BufferedImage(containerWidth, containerHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        // Fill background with black
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, containerWidth, containerHeight);
        // Center the scaled image
        int x = (containerWidth - newWidth) / 2;
        int y = (containerHeight - newHeight) / 2;
        g2d.drawImage(src, x, y, newWidth, newHeight, null);
        g2d.dispose();
        return scaled;
    }

    // Updated crossfade transition using scaled images.
    private void doCrossFadeTrans(BufferedImage prevImage, BufferedImage nextImage, JLabel imageLabel) {
        System.out.println("CrossFade transition triggered");
        long startTime = System.currentTimeMillis();

        timer = new Timer(UPDATE, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                float elapsed = System.currentTimeMillis() - startTime;
                float alpha = Math.min(elapsed / DURATION, 1.0f);

                BufferedImage currentImage = new BufferedImage(imageLabel.getWidth(), imageLabel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = currentImage.createGraphics();

                    // Draw the previous (scaled) image with inverse transparency.
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - alpha));
                g2d.drawImage(prevImage, 0, 0, null);

                    // Draw the next (scaled) image with transparency.
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.drawImage(nextImage, 0, 0, null);

                g2d.dispose();
                imageLabel.setIcon(new ImageIcon(currentImage));

                if (alpha >= 1.0f) {
                    timer.stop();
                    imageLabel.setIcon(new ImageIcon(nextImage));
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    imageLabel.setVerticalAlignment(SwingConstants.CENTER);
                    long endTime = System.currentTimeMillis();
                    System.out.println("CrossFade transition finished. Actual duration: " + (endTime - startTime) + " ms");
                }
            }
        });
        timer.start();
    }

    // Updated wipe transition using scaled images.
    private void doWipeTrans(BufferedImage prevImage, BufferedImage nextImage, JLabel imageLabel, Direction dir) {
        System.out.println("Wipe transition triggered in the " + dir + " direction");
        long startTime = System.currentTimeMillis();

        timer = new Timer(UPDATE, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                float elapsed = System.currentTimeMillis() - startTime;
                float progress = Math.min(elapsed / DURATION, 1.0f);

                int labelWidth = imageLabel.getWidth();
                int labelHeight = imageLabel.getHeight();

                BufferedImage currentImage = new BufferedImage(labelWidth, labelHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = currentImage.createGraphics();
                g2d.drawImage(prevImage, 0, 0, null);

                int offsetX = 0;
                int offsetY = 0;

                switch (dir) {
                    case LEFT:
                        offsetX = labelWidth - (int) (labelWidth * progress);
                        break;
                    case RIGHT:
                        offsetX = -labelWidth + (int) (labelWidth * progress);
                        break;
                    case UP:
                        offsetY = labelHeight - (int) (labelHeight * progress);
                        break;
                    case DOWN:
                        offsetY = -labelHeight + (int) (labelHeight * progress);
                        break;
                }

                g2d.drawImage(nextImage, offsetX, offsetY, null);
                g2d.dispose();
                imageLabel.setIcon(new ImageIcon(currentImage));

                if (progress >= 1.0f) {
                    timer.stop();
                    imageLabel.setIcon(new ImageIcon(nextImage));
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    imageLabel.setVerticalAlignment(SwingConstants.CENTER);
                    long endTime = System.currentTimeMillis();
                    System.out.println("Wipe transition finished. Actual duration: " + (endTime - startTime) + " ms");
                }
            }
        });
        timer.start();
    }

    // Helper function: Converts an Image to a BufferedImage.
    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimg.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimg;
    }
}
