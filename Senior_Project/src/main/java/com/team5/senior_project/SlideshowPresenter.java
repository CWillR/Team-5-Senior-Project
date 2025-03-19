package com.team5.senior_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SlideshowPresenter extends JFrame {
    private JLabel imageLabel;
    private File[] imageFiles;
    private int currentIndex = 0;
    private Timer timer;
    private int duration; // Duration for each slide in milliseconds
    private boolean loop; // If true, slideshow loops; if false, window closes when finished

    /**
     * Constructor that accepts the images to display, the duration (in milliseconds) 
     * for each slide, and a flag indicating whether to loop the slideshow.
     *
     * @param images   Array of image files to display.
     * @param duration Duration in milliseconds for each slide.
     * @param loop     If true, the slideshow will loop indefinitely; if false, the presenter closes when done.
     */
    public SlideshowPresenter(File[] images, int duration, boolean loop) {
        this.imageFiles = images;
        this.duration = duration;
        this.loop = loop;
        initComponents();
        startSlideshow();
    }
    
    private void initComponents() {
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        add(imageLabel, BorderLayout.CENTER);
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("Slideshow Presenter");
        setSize(800, 600);  // Adjust the size as needed
        setLocationRelativeTo(null);
    }
    
    // Starts the timer to change images at the specified duration
    private void startSlideshow() {
        if (imageFiles == null || imageFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "No images to display.");
            return;
        }
        showImage(currentIndex);
        timer = new Timer(duration, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentIndex++;
                if (currentIndex < imageFiles.length) {
                    showImage(currentIndex);
                } else {
                    if (loop) {
                        currentIndex = 0;
                        showImage(currentIndex);
                    } else {
                        timer.stop(); // Stop the slideshow
                        dispose();    // Close the presenter window
                    }
                }
            }
        });
        timer.start();
    }
    
    // Displays the image at the given index, scaling it to fit the label
    private void showImage(int index) {
        File imageFile = imageFiles[index];
        ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
        Image image = icon.getImage();
        
        // Get label dimensions
        int labelWidth = imageLabel.getWidth();
        int labelHeight = imageLabel.getHeight();
        if (labelWidth <= 0 || labelHeight <= 0) {
            // If the label isn’t laid out yet, fallback to frame dimensions.
            labelWidth = getWidth();
            labelHeight = getHeight();
        }
        
        // Calculate scaling while preserving the image's aspect ratio
        double widthRatio = (double) labelWidth / image.getWidth(null);
        double heightRatio = (double) labelHeight / image.getHeight(null);
        double scaleRatio = Math.min(widthRatio, heightRatio);
        int newWidth = (int) (image.getWidth(null) * scaleRatio);
        int newHeight = (int) (image.getHeight(null) * scaleRatio);
        
        Image resizedImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(resizedImage));
    }
}
