/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team5.senior_project;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author Team 5
 */
public class Transition {
    private Timer timer;
    private final int DURATION = 2500;
    private final int UPDATE = 40;  
    
    
    public enum Direction {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }
    
    public void doTransition(BufferedImage prevImage, BufferedImage nextImage, JLabel panel, TransitionType type) {
        switch(type) {
            case INSTANT:
                break;
            case CROSS_FADE:
                doCrossFadeTrans(prevImage, nextImage, panel);
                break;
            case WIPE_UP:
                doWipeTrans(prevImage, nextImage, panel, Direction.UP);
                break;
            case WIPE_RIGHT:
                doWipeTrans(prevImage, nextImage, panel, Direction.RIGHT);
                break;
            case WIPE_DOWN:
                doWipeTrans(prevImage, nextImage, panel, Direction.DOWN);
                break;
            case WIPE_LEFT:
                doWipeTrans(prevImage, nextImage, panel, Direction.LEFT);
                break;
        }    
    }
    
    private void doCrossFadeTrans(BufferedImage prevImage, BufferedImage nextImage, JLabel imageLabel) {
        System.out.println("CrossFade transition triggered");
        
        timer = new Timer(UPDATE, new ActionListener(){
            float alpha = 0.0f;
            @Override
            public void actionPerformed(ActionEvent e) {
                alpha += (float)UPDATE/(float)DURATION;
                if (alpha < 1.0f) {
                    BufferedImage currentImage = new BufferedImage(imageLabel.getWidth(), imageLabel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = currentImage.createGraphics();

                    // TODO: do the scaling calculations for previous image
                    // Calculate the positional x and y offsets of the prev image
                    // in order to align it within the label
                    int x = (imageLabel.getWidth() - prevImage.getWidth()) / 2;
                    int y = (imageLabel.getHeight() - prevImage.getHeight()) / 2;
                    
                    // Draw the previous image with transparency
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - alpha));
                    g2d.drawImage(prevImage, x, y, prevImage.getWidth(), prevImage.getHeight(), null);

                    // Calculate the positional x and y offsets of the next image
                    // in order to align it within the label
                    x = (imageLabel.getWidth() - nextImage.getWidth()) / 2;
                    y = (imageLabel.getHeight() - nextImage.getHeight()) / 2;
                    
                    // Draw the next image with transparency
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2d.drawImage(nextImage, x, y, nextImage.getWidth(), nextImage.getHeight(), null);

                    g2d.dispose();

                    // Set the composite image on the JLabel
                    imageLabel.setIcon(new ImageIcon(currentImage));
                }
                // Transition is finished
                else {
                    timer.stop();
                    
                    imageLabel.setIcon(new ImageIcon(nextImage));
                    
                    // Center the image in the label.
                    imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                    imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
                    
                    System.out.println("CrossFade transition finished.");
                }
            }
        });
        timer.start();
    }
    
    // Creates a composite image 
    private void doWipeTrans(BufferedImage prevImage, BufferedImage nextImage, JLabel imageLabel, Direction dir) {
        System.out.println("Wipe transition triggered in the " + dir + " direction");
        
        timer = new Timer(UPDATE, new ActionListener(){
            float progress = 0.0f;
            @Override
            public void actionPerformed(ActionEvent e) {
                int labelWidth = imageLabel.getWidth();
                int labelHeight = imageLabel.getHeight();
                progress += (float)UPDATE/(float)DURATION;
                
                if (progress < 1.0f) {
                    BufferedImage currentImage = new BufferedImage(labelWidth, labelHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = currentImage.createGraphics();
                    
                    // Draw the prev image
                    g2d.drawImage(prevImage, 0, 0, labelWidth, labelHeight, null);

                    // Set the clipping region for the next image
                    if (dir == Direction.UP) {
                        int wipeHeight = (int) (labelHeight * progress);
                        g2d.setClip(0, labelHeight - wipeHeight, labelWidth, wipeHeight);
                    }
                    if (dir == Direction.RIGHT) { //horizontal
                        int wipeWidth = (int) (labelWidth * progress);
                        g2d.setClip(0, 0, wipeWidth, labelHeight);
                    } 
                    if (dir == Direction.DOWN) {
                        int wipeHeight = (int) (labelHeight * progress);
                        g2d.setClip(0, 0, labelWidth, wipeHeight);
                    }
                    if (dir == Direction.LEFT) {
                        int wipeWidth = (int) (labelWidth * progress);
                        g2d.setClip(labelWidth - wipeWidth, 0, wipeWidth, labelHeight);
                    }

                    // Draw the clipped next image
                    g2d.drawImage(nextImage, 0, 0, labelWidth, labelHeight, null);

                    g2d.dispose();

                    // Set the composite image on the JLabel
                    imageLabel.setIcon(new ImageIcon(currentImage));
                }
                else {
                    timer.stop();
                    
                    imageLabel.setIcon(new ImageIcon(nextImage));
                    
                    // Center the image in the label.
                    imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                    imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
                    
                    System.out.println("Wipe transition finished.");
                }
            }
        });
        timer.start();
    }
    
    // Helper function, converts an Image to a BufferedImage
    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimg.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimg;
    }
}

    