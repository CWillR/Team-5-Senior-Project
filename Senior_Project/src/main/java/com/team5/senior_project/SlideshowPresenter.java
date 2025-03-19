package com.team5.senior_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SlideshowPresenter extends JFrame {
    private JLabel imageLabel;
    private JLabel pausedLabel; // Label to indicate paused state
    private File[] imageFiles;
    private int currentIndex = 0;
    private Timer timer;
    private int duration; // Duration for each slide in milliseconds
    private boolean loop; // If true, slideshow loops; if false, window closes when finished
    private boolean paused = false; // Whether the slideshow is paused

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
        initKeyBindings();
        startSlideshow();
    }
    
    private void initComponents() {
        // Create a main panel with an OverlayLayout so components overlap
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new OverlayLayout(mainPanel));
        
        // Create the image label that fills the panel.
        imageLabel = new JLabel();
        imageLabel.setAlignmentX(0.5f);
        imageLabel.setAlignmentY(0.5f);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        
        // Create the paused label, centered, with a larger font.
        pausedLabel = new JLabel("Paused");
        pausedLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        pausedLabel.setForeground(Color.WHITE);
        pausedLabel.setOpaque(false); // Transparent background
        pausedLabel.setAlignmentX(0.5f);
        pausedLabel.setAlignmentY(0.5f);
        pausedLabel.setVisible(false); // Initially hidden
        
        // Add components to the main panel. The later added component is on top.
        mainPanel.add(pausedLabel);
        mainPanel.add(imageLabel);
        
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("Slideshow Presenter");
        setSize(800, 600);
        setLocationRelativeTo(null);
    }
    
    // Set up key bindings for arrow keys and spacebar.
    private void initKeyBindings() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "nextImage");
        actionMap.put("nextImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextImage();
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "previousImage");
        actionMap.put("previousImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                previousImage();
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "togglePause");
        actionMap.put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePause();
            }
        });
    }
    
    // Starts the timer to change images at the specified duration.
    private void startSlideshow() {
        if (imageFiles == null || imageFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "No images to display.");
            return;
        }
        showImage(currentIndex);
        timer = new Timer(duration, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                advanceSlide();
            }
        });
        timer.start();
    }
    
    // Advances to the next slide.
    private void advanceSlide() {
        currentIndex++;
        if (currentIndex < imageFiles.length) {
            showImage(currentIndex);
        } else {
            if (loop) {
                currentIndex = 0;
                showImage(currentIndex);
            } else {
                timer.stop();
                dispose();
            }
        }
    }
    
    // Shows the previous image.
    private void previousImage() {
        resetTimer();
        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = loop ? imageFiles.length - 1 : 0;
        }
        showImage(currentIndex);
    }
    
    // Shows the next image.
    private void nextImage() {
        resetTimer();
        currentIndex++;
        if (currentIndex >= imageFiles.length) {
            currentIndex = loop ? 0 : imageFiles.length - 1;
        }
        showImage(currentIndex);
    }
    
    // Toggles pause/resume state and updates the paused label.
    private void togglePause() {
        if (paused) {
            timer.start();
            pausedLabel.setVisible(false);
        } else {
            timer.stop();
            pausedLabel.setVisible(true);
        }
        paused = !paused;
    }
    
    // Resets the timer to restart the duration for the current slide.
    private void resetTimer() {
        if (timer != null) {
            timer.restart();
        }
    }
    
    // Displays the image at the given index, scaling it to fit the panel.
    private void showImage(int index) {
        File imageFile = imageFiles[index];
        ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
        Image image = icon.getImage();
        
        // Use the current content pane dimensions for scaling.
        int panelWidth = getContentPane().getWidth();
        int panelHeight = getContentPane().getHeight();
        if (panelWidth <= 0 || panelHeight <= 0) {
            panelWidth = getWidth();
            panelHeight = getHeight();
        }
        
        double widthRatio = (double) panelWidth / image.getWidth(null);
        double heightRatio = (double) panelHeight / image.getHeight(null);
        double scaleRatio = Math.min(widthRatio, heightRatio);
        int newWidth = (int) (image.getWidth(null) * scaleRatio);
        int newHeight = (int) (image.getHeight(null) * scaleRatio);
        
        Image resizedImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(resizedImage));
    }
}
