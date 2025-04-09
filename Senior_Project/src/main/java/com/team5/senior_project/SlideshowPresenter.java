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

/**
 *
 * @author Team 5
 */
public class SlideshowPresenter extends javax.swing.JFrame {

    private File[] imageFiles; // image list
    private List<File> audioFiles; // audio list
    private final int[] index = {0}; // image list index
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
    

     
    /**
     * Creates new form SlideshowPresenter
     */
    public SlideshowPresenter() {
        initComponents();
        // Set up key bindings
        initKeyBindings();
        // Override the layout so that imageLabel fills the frame
        imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(imageLabel, BorderLayout.CENTER);
        // Dynamically add the paused overlay label to the layered pane.
        pausedLabel = new javax.swing.JLabel("Paused", javax.swing.SwingConstants.CENTER);
        pausedLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 48));
        pausedLabel.setForeground(java.awt.Color.WHITE);
        pausedLabel.setOpaque(false);
        pausedLabel.setVisible(false);
        this.getLayeredPane().add(pausedLabel, new Integer(200));
        pausedLabel.setBounds(0, 0, getWidth(), getHeight());
        // Update pausedLabel bounds when the frame is resized.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                pausedLabel.setBounds(0, 0, getWidth(), getHeight());
            }
        });
    }
    
    /**
     * Overloaded constructor that accepts an array of image files, a slide duration (in milliseconds),
     * a loop flag, and an autoMode flag. 
     *
     * @param imageFiles Array of image files to display.
     * @param audioFiles List of audio files to play.
     * @param duration   Slide duration in milliseconds.
     * @param loop       If true, the slideshow will loop; if false, it stops on the last slide.
     * @param autoMode   If true, slides advance automatically; if false, user must manually change slides.
     * @param slideTransitions Array of TransitionType enums for each slide.
     */
    public SlideshowPresenter(File[] imageFiles, List<File> audioFiles, int duration, boolean loop, boolean autoMode, TransitionType[] slideTransitions) {
        this(); // Call no-argument constructor for initialization.        
        this.imageFiles = imageFiles;
        this.audioFiles = audioFiles;
        this.autoMode = autoMode; // Store the auto mode setting.
        this.canLoop = loop;      // Store the loop (can loop) setting.
        this.slideTransitions = slideTransitions; // Save transitions for use in updateImage()
        if (imageFiles != null && imageFiles.length > 0) {
            playAudioFilesSequentially();
            updateImage();
            if (autoMode) { // Only start the timer if auto mode is enabled.
                slideShowTimer = new Timer(duration, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {                        
                        index[0] = (index[0] + 1) % imageFiles.length;
                        updateImage();
                        // If looping is disabled and we are at the last image, stop the timer.
                        if (!canLoop && index[0] == imageFiles.length - 1) {
                            slideShowTimer.stop();
                        }
                    }
                });
                slideShowTimer.start();
            }            
        }
    }
    
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
                                audioLock.wait(); // wait for resume
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
                    currentAudioIndex++; // skip bad file
                }
            }
        });
        audioThread.start();
    }
        
    /**
     * Initializes key bindings for the left, right arrow keys and the space bar.
     * Right arrow advances to the next slide; left arrow goes to the previous slide.
     * Space bar toggles pause/resume. In all cases, the timer is restarted.
     */
    private void initKeyBindings() {
        InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getRootPane().getActionMap();
        
        // Right arrow binding: next image.
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextImage");
        am.put("nextImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (imageFiles != null && imageFiles.length > 0) {
                    if (canLoop) {
                        // Wrap around if looping is enabled.
                        index[0] = (index[0] + 1) % imageFiles.length;
                    } else {
                        // Only advance if not at the last image.
                        if (index[0] < imageFiles.length - 1) {
                            index[0]++;
                        }
                    }
                    updateImage();
                    if (autoMode && slideShowTimer != null) {
                        slideShowTimer.restart();
                    }
                }
            }
        });

        // Left arrow binding: previous image.
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previousImage");
        am.put("previousImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (imageFiles != null && imageFiles.length > 0) {
                    if (canLoop) {
                        // Wrap around if looping is enabled.
                        index[0] = (index[0] - 1 + imageFiles.length) % imageFiles.length;
                    } else {
                        // Only go back if not at the first image.
                        if (index[0] > 0) {
                            index[0]--;
                        }
                    }
                    updateImage();
                    if (autoMode && slideShowTimer != null) {
                        slideShowTimer.restart();
                    }
                }
            }
        });
        
        // Space bar binding: toggle pause/resume.
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "togglePause");
        am.put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePause();
            }
        });
    }
    
    /**
     * Toggles pause/resume state. When paused, the timer stops and the "Paused" overlay is shown;
     * when resumed, the timer restarts and the overlay is hidden.
     */
    private void togglePause() {
        if (autoMode && slideShowTimer != null) {
            if (paused) {
                slideShowTimer.start();
                pausedLabel.setVisible(false);
                paused = false;
                resumeAudio();
            } else {
                slideShowTimer.stop();
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
    
    // Loads built slideshow into the SlideShowPresenter JLabel
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
                index[0] = 0; // Reset index to start
                updateImage();
                System.out.println("Slideshow loaded successfully.");

                if (slideShowTimer != null && slideShowTimer.isRunning()) {
                    slideShowTimer.stop();
                }

                slideShowTimer = new Timer(8000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        index[0] = (index[0] + 1) % imageFiles.length;
                        updateImage();
                    }
                });
                slideShowTimer.start();

            } else {
                System.err.println("No valid images found in the slideshow file.");
            }

        } catch (IOException e) {
            System.err.println("Error loading slideshow: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Updates the image in the SlideShowPresenter
    private void updateImage() {
        if (imageFiles != null && imageFiles.length > 0) {
            // Retrieve the "next" image (the one corresponding to the current index).
            ImageIcon nextIcon = new ImageIcon(imageFiles[index[0]].getAbsolutePath());
            Image nextImage = nextIcon.getImage();
    
            // Determine the previous image.
            Image prevImage;
            if (index[0] == 0) {
                prevImage = new ImageIcon(imageFiles[imageFiles.length - 1].getAbsolutePath()).getImage();
            } else {
                prevImage = new ImageIcon(imageFiles[index[0] - 1].getAbsolutePath()).getImage();
            }
    
            // Get the transition type for the current slide.
            TransitionType currentTransition = TransitionType.INSTANT;
            if (slideTransitions != null && slideTransitions.length > index[0]) {
                currentTransition = slideTransitions[index[0]];
            }
    
            // Convert images to BufferedImage.
            BufferedImage prevBuffered = Transition.toBufferedImage(prevImage);
            BufferedImage nextBuffered = Transition.toBufferedImage(nextImage);
    
            // Play the animated transition.
            transitionManager.doTransition(prevBuffered, nextBuffered, imageLabel, currentTransition);
        }
    }
    
    // Loads the folder for created slideshows
    public class SlideShowFileManager {
        private static final File savedSlidesFolder = new File(System.getProperty("user.dir"), "SavedSlideShows");

        public static File getSavedSlidesFolder() {
            return savedSlidesFolder;
        }

        public static void main(String[] args) {
            System.out.println("Accessing SlideShowImages folder: " + SlideShowFileManager.getSavedSlidesFolder().getAbsolutePath());
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
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

    // Opens FileChooser for user to select a saved slideshow to load
    private void openSlideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                  
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Slideshow");
        fileChooser.setCurrentDirectory(SlideShowFileManager.getSavedSlidesFolder());
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Slideshow Files (*.ssx)", "ssx"));

        int userSelection = fileChooser.showOpenDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();
            loadSlideshow(fileToLoad);
        }
    }    
    
    

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SlideshowPresenter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SlideshowPresenter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SlideshowPresenter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel imageLabel;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem openSlideMenuItem;
    // End of variables declaration//GEN-END:variables
}