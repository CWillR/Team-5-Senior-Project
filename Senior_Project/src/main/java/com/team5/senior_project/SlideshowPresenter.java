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

/**
 *
 * @author Team 5
 */
public class SlideshowPresenter extends javax.swing.JFrame {

    private File[] imageFiles; // image list
    private final int[] index = {0}; // image list index
    private Timer slideShowTimer;
    private boolean autoMode;
    private boolean canLoop;
    private boolean paused = false;
    private javax.swing.JLabel pausedLabel;
     
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
     * @param duration   Slide duration in milliseconds.
     * @param loop       If true, the slideshow will loop; if false, it stops on the last slide.
     * @param autoMode   If true, slides advance automatically; if false, user must manually change slides.
     */
    public SlideshowPresenter(File[] imageFiles, int duration, boolean loop, boolean autoMode) {
        this(); // Call no-argument constructor for initialization.
        this.imageFiles = imageFiles;
        this.autoMode = autoMode; // Store the auto mode setting.
        this.canLoop = loop;      // Store the loop (can loop) setting.
        if (imageFiles != null && imageFiles.length > 0) {
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
            } else {
                slideShowTimer.stop();
                pausedLabel.setVisible(true);
                paused = true;
            }
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
            ImageIcon originalIcon = new ImageIcon(imageFiles[index[0]].getAbsolutePath());
            Image originalImage = originalIcon.getImage();
            int labelWidth = imageLabel.getWidth();
            int labelHeight = imageLabel.getHeight();
            double widthRatio = (double) labelWidth / originalImage.getWidth(null);
            double heightRatio = (double) labelHeight / originalImage.getHeight(null);
            double scaleRatio = Math.min(widthRatio, heightRatio);
            int newWidth = (int) (originalImage.getWidth(null) * scaleRatio);
            int newHeight = (int) (originalImage.getHeight(null) * scaleRatio);
            Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(resizedImage));
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
                .addContainerGap(35, Short.MAX_VALUE)
                .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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