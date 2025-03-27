/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.team5.senior_project;

import java.awt.Dimension;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Team 5
 */
public class SlideshowPresenter extends javax.swing.JFrame {

    private List<Slide> slides = new ArrayList<>();
    private int currentSlideIndex = 0;
    private int interval = 5000; // Default interval (5 seconds)
    private boolean loop = true;
    private Timer slideTimer;
    private File[] imageFiles; // image list
    private final int[] index = {0}; // image list index
    private Timer slideShowTimer;
    private boolean manualMode = false;
    // Fields for pause functionality
    private boolean paused = false;
    private javax.swing.JLabel pausedLabel;
     
    /**
     * Creates new form SlideshowPresenter
     *//**
     * Creates new form SlideshowPresenter
     */
    public SlideshowPresenter() {
        initComponents();
        imageLabel.setPreferredSize(new Dimension(600, 400));
        updateSlide();
    }
    
    private void updateSlide() {
         if (!slides.isEmpty() && currentSlideIndex >= 0 && currentSlideIndex < slides.size()) {
            Slide currentSlide = slides.get(currentSlideIndex);
            ImageIcon icon = new ImageIcon(currentSlide.getImagePath());
            Image img = icon.getImage().getScaledInstance(imageLabel.getPreferredSize().width, imageLabel.getPreferredSize().height, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(img));
        } else {
            imageLabel.setIcon(null);
        }
    }
    /**
     * Overloaded constructor that accepts an array of image files, a slide duration (in milliseconds),
     * and a loop flag. This constructor is useful if you want to start the presenter with a preset
     * slideshow.
     * 
     * @param imageFiles Array of image files to display.
     * @param duration   Slide duration in milliseconds.
     * @param loop       If true, the slideshow will loop; if false, it stops on the last slide.
     */
    public SlideshowPresenter(File[] imageFiles, int duration, boolean loop, boolean manualMode) {
        this(); // Call the no-argument constructor to initialize GUI components, key bindings, and pausedLabel.
        this.imageFiles = imageFiles;
        this.manualMode = manualMode;
        if (imageFiles != null && imageFiles.length > 0) {
            updateImage();
            if (!manualMode) {
                slideShowTimer = new Timer(duration, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        index[0] = (index[0] + 1) % imageFiles.length;
                        updateImage();
                        if (!loop && index[0] == imageFiles.length - 1) {
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
                    index[0] = (index[0] + 1) % imageFiles.length;
                    updateImage();
                    if (!manualMode && slideShowTimer != null) {
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
                    index[0] = (index[0] - 1 + imageFiles.length) % imageFiles.length;
                    updateImage();
                    if (!manualMode && slideShowTimer != null) {
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
        if (slideShowTimer != null) {
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
    private void loadSlideshow(File file) {
        slides.clear();
        currentSlideIndex = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            JSONObject slideshowJson = new JSONObject(jsonContent.toString());

            loop = slideshowJson.getBoolean("loop");
            interval = slideshowJson.getInt("interval") * 1000; // Convert seconds to milliseconds

            JSONArray slidesArray = slideshowJson.getJSONArray("slides");
            for (int i = 0; i < slidesArray.length(); i++) {
                JSONObject slideJson = slidesArray.getJSONObject(i);
                String imagePath = slideJson.getString("image");
                slides.add(new Slide(imagePath));
            }
            updateSlide();
            startSlideshow();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading slideshow: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void startSlideshow() {
        if (slideTimer != null) {
            slideTimer.stop();
        }
        slideTimer = new Timer(interval, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentSlideIndex++;
                if (currentSlideIndex >= slides.size()) {
                    if (loop) {
                        currentSlideIndex = 0;
                    } else {
                        stopSlideshow();
                        return;
                    }
                }
                updateSlide();
            }
        });
        slideTimer.start();
    }

    
    // Loads the folder for created slideshows
    public static class SlideShowFileManager {
        private static final File savedSlidesFolder = new File(System.getProperty("user.dir"), "SavedSlideShows");

    private void stopSlideshow() {
        if (slideTimer != null) {
            slideTimer.stop();
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
        JFileChooser fileChooser = new JFileChooser(SlideShowFileManager.getSavedSlidesFolder());
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            loadSlideshow(file);
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