/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team5.senior_project;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class Transition {
    private Timer timer;
    // DURATION will be updated from the passed duration value.
    private int DURATION = 2500; 
    private final int UPDATE = 40; // update every 40ms

    public enum Direction {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }

    /**
     * Overloaded doTransition method that accepts a callback (Runnable).
     * The callback is invoked when the transition animation completes.
     * If the transition is INSTANT, the image is set immediately and the callback is called.
     */
    public void doTransition(BufferedImage prevImage, BufferedImage nextImage, JLabel panel, TransitionType type, int duration, Runnable onTransitionEnd) {
        // Pre-scale the images once based on the panel’s current dimensions.
        int containerWidth = panel.getWidth();
        int containerHeight = panel.getHeight();
        final BufferedImage scaledPrev = getScaledImage(prevImage, containerWidth, containerHeight);
        final BufferedImage scaledNext = getScaledImage(nextImage, containerWidth, containerHeight);
        
        // Set the duration: if valid use it; otherwise, default.
        DURATION = (duration > 0) ? duration : 2500;
        
        // For INSTANT transitions, update immediately and call callback.
        if (type == TransitionType.INSTANT) {
            panel.setIcon(new ImageIcon(scaledNext));
            panel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.setVerticalAlignment(SwingConstants.CENTER);
            if (onTransitionEnd != null) {
                onTransitionEnd.run();
            }
            return;
        }
        
        // Choose the animation method depending on the type.
        switch (type) {
            case CROSS_FADE:
                doCrossFadeTrans(scaledPrev, scaledNext, panel, onTransitionEnd);
                break;
            case WIPE_UP:
                doWipeTrans(scaledPrev, scaledNext, panel, Direction.UP, onTransitionEnd);
                break;
            case WIPE_RIGHT:
                doWipeTrans(scaledPrev, scaledNext, panel, Direction.RIGHT, onTransitionEnd);
                break;
            case WIPE_DOWN:
                doWipeTrans(scaledPrev, scaledNext, panel, Direction.DOWN, onTransitionEnd);
                break;
            case WIPE_LEFT:
                doWipeTrans(scaledPrev, scaledNext, panel, Direction.LEFT, onTransitionEnd);
                break;
            default:
                panel.setIcon(new ImageIcon(scaledNext));
                panel.setHorizontalAlignment(SwingConstants.CENTER);
                panel.setVerticalAlignment(SwingConstants.CENTER);
                if (onTransitionEnd != null) {
                    onTransitionEnd.run();
                }
                break;
        }
    }

    /**
     * Overload to support calls without a callback.
     */
    public void doTransition(BufferedImage prevImage, BufferedImage nextImage, JLabel panel, TransitionType type, int duration) {
        doTransition(prevImage, nextImage, panel, type, duration, null);
    }
    
    // Helper method: scale an image to the container size while preserving aspect ratio and center it on a black background.
    private BufferedImage getScaledImage(BufferedImage src, int containerWidth, int containerHeight) {
        int imgWidth = src.getWidth();
        int imgHeight = src.getHeight();
        double scale = Math.min((double) containerWidth / imgWidth, (double) containerHeight / imgHeight);
        int newWidth = (int) (imgWidth * scale);
        int newHeight = (int) (imgHeight * scale);
        
        BufferedImage scaled = new BufferedImage(containerWidth, containerHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        // Fill the background with black.
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, containerWidth, containerHeight);
        // Center the scaled image.
        int x = (containerWidth - newWidth) / 2;
        int y = (containerHeight - newHeight) / 2;
        g2d.drawImage(src, x, y, newWidth, newHeight, null);
        g2d.dispose();
        return scaled;
    }
    
    // Crossfade transition using the fixed scaled images.
    private void doCrossFadeTrans(BufferedImage prevImage, BufferedImage nextImage, JLabel imageLabel, Runnable onTransitionEnd) {
        System.out.println("CrossFade transition triggered");
        final long startTime = System.currentTimeMillis();
        final int width = imageLabel.getWidth();
        final int height = imageLabel.getHeight();
        
        timer = new Timer(UPDATE, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                float elapsed = System.currentTimeMillis() - startTime;
                float alpha = Math.min(elapsed / (float) DURATION, 1.0f);
                
                BufferedImage currentImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = currentImage.createGraphics();
                // Draw the previous image with inverse alpha.
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - alpha));
                g2d.drawImage(prevImage, 0, 0, width, height, null);
                // Draw the next image with alpha.
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.drawImage(nextImage, 0, 0, width, height, null);
                g2d.dispose();
                
                imageLabel.setIcon(new ImageIcon(currentImage));
                
                if (alpha >= 1.0f) {
                    timer.stop();
                    // Use the pre-scaled next image to maintain the same appearance.
                    imageLabel.setIcon(new ImageIcon(nextImage));
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    imageLabel.setVerticalAlignment(SwingConstants.CENTER);
                    long endTime = System.currentTimeMillis();
                    System.out.println("CrossFade transition finished. Actual duration: " + (endTime - startTime) + " ms");
                    if (onTransitionEnd != null) {
                        onTransitionEnd.run();
                    }
                }
            }
        });
        timer.start();
    }
    
    // Wipe transition using the fixed scaled images.
    private void doWipeTrans(BufferedImage prevImage, BufferedImage nextImage, JLabel imageLabel, Direction dir, Runnable onTransitionEnd) {
        System.out.println("Wipe transition triggered in the " + dir + " direction");
        final long startTime = System.currentTimeMillis();
        final int width = imageLabel.getWidth();
        final int height = imageLabel.getHeight();
        
        timer = new Timer(UPDATE, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                float elapsed = System.currentTimeMillis() - startTime;
                float progress = Math.min(elapsed / (float) DURATION, 1.0f);
                
                BufferedImage currentImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = currentImage.createGraphics();
                // Draw the previous image using fixed dimensions.
                g2d.drawImage(prevImage, 0, 0, width, height, null);
                
                int offsetX = 0, offsetY = 0;
                switch (dir) {
                    case LEFT:
                        offsetX = width - (int)(width * progress);
                        break;
                    case RIGHT:
                        offsetX = -width + (int)(width * progress);
                        break;
                    case UP:
                        offsetY = height - (int)(height * progress);
                        break;
                    case DOWN:
                        offsetY = -height + (int)(height * progress);
                        break;
                }
                // Draw the next image at the calculated offset on the fixed canvas.
                g2d.drawImage(nextImage, offsetX, offsetY, width, height, null);
                g2d.dispose();
                
                imageLabel.setIcon(new ImageIcon(currentImage));
                
                if (progress >= 1.0f) {
                    timer.stop();
                    // Set the final image to the pre-scaled next image.
                    imageLabel.setIcon(new ImageIcon(nextImage));
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    imageLabel.setVerticalAlignment(SwingConstants.CENTER);
                    long endTime = System.currentTimeMillis();
                    System.out.println("Wipe transition finished. Actual duration: " + (endTime - startTime) + " ms");
                    if (onTransitionEnd != null) {
                        onTransitionEnd.run();
                    }
                }
            }
        });
        timer.start();
    }
    
    // Helper method: converts an Image to a BufferedImage.
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
