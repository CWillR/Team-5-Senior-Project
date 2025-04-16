/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.team5.senior_project;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.image.BufferedImage;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

public class SlideshowPresenter extends javax.swing.JFrame {

    private File[] imageFiles;           // Image list
    private List<File> audioFiles;         // Audio list
    private final int[] index = {0};       // Current image index (using a one-element array)
    private int currentAudioIndex = 0;
    private Timer slideShowTimer;
    private boolean autoMode;
    private boolean canLoop;
    private TransitionType[] slideTransitions;
    private boolean paused = false;
    private javax.swing.JLabel pausedLabel;
    private final Transition transitionManager = new Transition();
    private Thread audioThread;
    private boolean audioPaused = false;
    private final Object audioLock = new Object();
    private Clip currentClip;
    private boolean slideshowStopped = false;
    
    // List of transition times for each slide (in ms).
    // (Transition time for slide N is applied when transitioning into slide N.)
    private List<Integer> transitionTimes = new ArrayList<>();
    
    // Fixed display duration (in ms) for each slide.
    private int fixedDuration;

    /**
     * Creates new form SlideshowPresenter
     */
    public SlideshowPresenter() {
        initComponents();
        initKeyBindings();
        // Set up the image label.
        imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(imageLabel, BorderLayout.CENTER);
        // Configure paused overlay.
        pausedLabel = new javax.swing.JLabel("Paused", javax.swing.SwingConstants.CENTER);
        pausedLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 48));
        pausedLabel.setForeground(java.awt.Color.WHITE);
        pausedLabel.setOpaque(false);
        pausedLabel.setVisible(false);
        this.getLayeredPane().add(pausedLabel, new Integer(200));
        pausedLabel.setBounds(0, 0, getWidth(), getHeight());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                pausedLabel.setBounds(0, 0, getWidth(), getHeight());
            }
        });
    }
    
    /**
     * Overloaded constructor.
     * @param imageFiles         Array of image files.
     * @param audioFiles         List of audio files.
     * @param duration           Fixed display duration (in ms) for each slide.
     * @param loop               If true, the slideshow loops; otherwise it stops at the last slide.
     * @param autoMode           If true, slides auto-advance.
     * @param slideTransitions   Array of TransitionType enums for each slide.
     * @param transitionTimes    List of transition times (in ms) for each slide (applied when transitioning in).
     */
    public SlideshowPresenter(File[] imageFiles, List<File> audioFiles, int duration, boolean loop, boolean autoMode, TransitionType[] slideTransitions, List<Integer> transitionTimes) {
        this(); // Call default constructor.
        this.imageFiles = imageFiles;
        this.audioFiles = audioFiles;
        this.autoMode = autoMode;
        this.canLoop = loop;
        this.slideTransitions = slideTransitions;
        this.transitionTimes = transitionTimes;
        this.fixedDuration = duration;
        
        if (imageFiles != null && imageFiles.length > 0) {
            playAudioFilesSequentially();
            if (autoMode) {
                startAutoSlideCycle();
            } else {
                updateImage();  // Manual mode update.
            }
        }
    }
    
    // --------------------
    // Auto Mode Cycle Methods
    // --------------------
    
    // Starts the auto cycle: display the current slide and start a timer for fixed duration.
    private void startAutoSlideCycle() {
        showSlide();
        Timer displayTimer = new Timer(fixedDuration, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                transitionToNextSlide();
            }
        });
        displayTimer.setRepeats(false);
        displayTimer.start();
    }
    
    // Displays the current slide (scaling the image to fit).
    private void showSlide() {
        ImageIcon icon = new ImageIcon(imageFiles[index[0]].getAbsolutePath());
        int labelWidth = imageLabel.getWidth();
        int labelHeight = imageLabel.getHeight();
        if (labelWidth <= 0 || labelHeight <= 0) {
            labelWidth = imageLabel.getPreferredSize().width;
            labelHeight = imageLabel.getPreferredSize().height;
        }
        Image scaledImage = icon.getImage().getScaledInstance(labelWidth, labelHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        System.out.println("Showing slide index: " + index[0]);
    }
    
    // Initiates the transition into the next slide.
    private void transitionToNextSlide() {
        final int nextIndex = (index[0] + 1) % imageFiles.length;
        // Determine the transition type for the incoming slide.
        TransitionType nextTransition = TransitionType.INSTANT;
        if (slideTransitions != null && slideTransitions.length > nextIndex) {
            nextTransition = slideTransitions[nextIndex];
        }
        // Determine transition time: if transition is INSTANT then 0, else use list value.
        final int transTime = (nextTransition == TransitionType.INSTANT)
                ? 0 : ((transitionTimes != null && transitionTimes.size() > nextIndex)
                        ? transitionTimes.get(nextIndex) : 1000);
        // Prepare images.
        BufferedImage prevBuffered = Transition.toBufferedImage(new ImageIcon(imageFiles[index[0]].getAbsolutePath()).getImage());
        BufferedImage nextBuffered = Transition.toBufferedImage(new ImageIcon(imageFiles[nextIndex].getAbsolutePath()).getImage());
        // Call the transition method with a callback.
        transitionManager.doTransition(prevBuffered, nextBuffered, imageLabel, nextTransition, transTime, new Runnable() {
            @Override
            public void run() {
                index[0] = nextIndex;
                int totalTime = fixedDuration + transTime;
                System.out.println("Transition complete. Now showing slide index: " + index[0]
                        + " | Total time (display + transition): " + totalTime + " ms");
                startAutoSlideCycle();
            }
        });
    }
    
    // --------------------
    // Manual Mode update (called by key bindings)
    // --------------------
    
    // Immediately updates and displays the current slide.
    private void updateImage() {
        if (imageFiles != null && imageFiles.length > 0) {
            ImageIcon icon = new ImageIcon(imageFiles[index[0]].getAbsolutePath());
            Image img = icon.getImage();
            int labelWidth = imageLabel.getWidth();
            int labelHeight = imageLabel.getHeight();
            if (labelWidth <= 0 || labelHeight <= 0) {
                labelWidth = imageLabel.getPreferredSize().width;
                labelHeight = imageLabel.getPreferredSize().height;
            }
            Image scaledImage = img.getScaledInstance(labelWidth, labelHeight, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
            System.out.println("Manual update: showing slide index " + index[0]);
        }
    }
    
    // --------------------
    // Audio Methods
    // --------------------
    
    private void playAudioFilesSequentially() {
        audioThread = new Thread(() -> {
            while (currentAudioIndex < audioFiles.size() && !slideshowStopped) {
                File audioFile = audioFiles.get(currentAudioIndex);
                try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile)) {
                    synchronized (audioLock) {
                        if (currentClip != null && currentClip.isOpen()) {
                            currentClip.close();
                        }
                        currentClip = AudioSystem.getClip();
                        currentClip.open(audioStream);
                    }
                    final Object clipLock = new Object();
                    final boolean[] playing = {true};
                    currentClip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP && !audioPaused) {
                            synchronized (clipLock) {
                                playing[0] = false;
                                clipLock.notifyAll();
                            }
                        }
                    });
                    synchronized (audioLock) {
                        currentClip.start();
                    }
                    while (true) {
                        synchronized (audioLock) {
                            if (audioPaused) {
                                currentClip.stop();
                                audioLock.wait();
                                currentClip.start();
                            }
                        }
                        synchronized (clipLock) {
                            if (!playing[0]) break;
                        }
                        Thread.sleep(100);
                    }
                    currentClip.close();
                    currentAudioIndex++;
                } catch (Exception e) {
                    e.printStackTrace();
                    currentAudioIndex++; // Skip bad file.
                }
            }
        });
        audioThread.start();
    }
    
    // --------------------
    // Key Bindings
    // --------------------
    
    private void initKeyBindings() {
        InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getRootPane().getActionMap();
        // Right arrow: next image (manual update).
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextImage");
        am.put("nextImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (imageFiles != null && imageFiles.length > 0) {
                    if (canLoop) {
                        index[0] = (index[0] + 1) % imageFiles.length;
                    } else if (index[0] < imageFiles.length - 1) {
                        index[0]++;
                    }
                    updateImage();
                }
            }
        });
        // Left arrow: previous image.
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previousImage");
        am.put("previousImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (imageFiles != null && imageFiles.length > 0) {
                    if (canLoop) {
                        index[0] = (index[0] - 1 + imageFiles.length) % imageFiles.length;
                    } else if (index[0] > 0) {
                        index[0]--;
                    }
                    updateImage();
                }
            }
        });
        // Space bar: toggle pause/resume.
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "togglePause");
        am.put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePause();
            }
        });
    }
    
    private void togglePause() {
        if (autoMode) {
            if (paused) {
                pausedLabel.setVisible(false);
                paused = false;
                resumeAudio();
                startAutoSlideCycle();
            } else {
                if (slideShowTimer != null) {
                    slideShowTimer.stop();
                }
                pausedLabel.setVisible(true);
                paused = true;
                pauseAudio();
            }
        }
    }
    
    private void pauseAudio() {
        synchronized (audioLock) {
            audioPaused = true;
        }
    }
    
    private void resumeAudio() {
        synchronized (audioLock) {
            audioPaused = false;
            audioLock.notifyAll();
        }
    }
    
    private void stopAudio() {
        synchronized (audioLock) {
            slideshowStopped = true;
            audioPaused = false;
            audioLock.notifyAll();
            if (currentClip != null && currentClip.isOpen()) {
                currentClip.stop();
                currentClip.close();
            }
            currentAudioIndex = 0;
        }
    }
    
    // --------------------
    // File Loader
    // --------------------
    
    private void loadSlideshow(File loadFile) {
        List<File> loadedImages = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(loadFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                File imageFile = new File(line);
                if (imageFile.exists()) {
                    loadedImages.add(imageFile);
                } else {
                    System.err.println("Warning: File not found: " + line);
                }
            }
            if (!loadedImages.isEmpty()) {
                imageFiles = loadedImages.toArray(new File[0]);
                index[0] = 0;
                updateImage();
                System.out.println("Slideshow loaded successfully.");
                if (autoMode) {
                    startAutoSlideCycle();
                }
            } else {
                System.err.println("No valid images found in the slideshow file.");
            }
        } catch (IOException e) {
            System.err.println("Error loading slideshow: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // --------------------
    // NetBeans Generated Code
    // --------------------
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        imageLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        openSlideMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Slideshow Presenter");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jMenu1.setText("File");

        openSlideMenuItem.setText("Open Slide");
        openSlideMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openSlideMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(openSlideMenuItem);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        stopAudio();
    }//GEN-LAST:event_formWindowClosing

    private void openSlideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openSlideMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Slideshow");
        fileChooser.setCurrentDirectory(SlideShowFileManager.getSavedSlidesFolder());
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Slideshow Files (*.ssx)", "ssx"));
        int userSelection = fileChooser.showOpenDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();
            loadSlideshow(fileToLoad);
        }
    }//GEN-LAST:event_openSlideMenuItemActionPerformed

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc="Look and feel setting code (optional)">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(SlideshowPresenter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SlideshowPresenter().setVisible(true);
            }
        });
    }

    // --------------------
    // NetBeans Variable Declaration
    // --------------------
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel imageLabel;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem openSlideMenuItem;
    // End of variables declaration//GEN-END:variables
}
