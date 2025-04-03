/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.team5.senior_project;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileView;
import net.coobird.thumbnailator.Thumbnails;
import java.awt.image.BufferedImage;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 *
 * @author Team 5
 */
public class SlideshowCreator extends javax.swing.JFrame {

    private SettingsPanel settingsPanel;
    private List<File> imageFiles = new ArrayList<>(); // image list
    private final List<File> audioFiles = new ArrayList<>(); // audio list
    private int audioIndex = 0; // tracks current audio track
    private final Preferences prefs = Preferences.userNodeForPackage(SlideshowCreator.class);
    private File currentSlideshowFile = null;
    private TimelinePanel timelinePanelObject; // Declare it
    private final Transition transitionManager = new Transition();
    private String currentSlideshowName = null; // Class-level variable
    private AudioTimelinePanel audioTimelinePanel;
    private final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(4);
    private Clip audioClip; // audio playing is stored here
    private boolean isPlaying = false; // flag for tracking audio
    private boolean stopRequested = false; // flag to track for audio stop

    /**
     * Creates new form SlideshowCreator
     */
    public SlideshowCreator() {
        initComponents();
        applySavedTheme(); // Apply saved theme when starting

        // Set up the file explorer and large file view panels
        FileExplorerPanel fileExplorerPanel = new FileExplorerPanel();
        fileExplorerHolder.removeAll();
        fileExplorerHolder.setLayout(new BorderLayout());
        fileExplorerHolder.add(fileExplorerPanel, BorderLayout.CENTER);
        fileExplorerHolder.revalidate();
        fileExplorerHolder.repaint();

        // Use default folder for initial large view.
        File defaultFolder = new File(System.getProperty("user.home"));
        LargeFileViewPanel largeFileViewPanel = new LargeFileViewPanel(defaultFolder);
        largeFileViewHolder.removeAll();
        largeFileViewHolder.setLayout(new BorderLayout());
        largeFileViewHolder.add(largeFileViewPanel, BorderLayout.CENTER);
        largeFileViewHolder.revalidate();
        largeFileViewHolder.repaint();
        
        settingsPanel = new SettingsPanel();
        // Remove or replace the existing settingsHolder tab:
        settingsHolder.removeAll();
        // After initComponents(), add this code:
        settingsPanel = new SettingsPanel();
        // Remove the existing settingsHolder tab if it exists:
        int settingsTabIndex = jTabbedPane1.indexOfComponent(settingsHolder);
        if (settingsTabIndex != -1) {
            jTabbedPane1.removeTabAt(settingsTabIndex);
        }
        // Add your SettingsPanel as a new tab:
        jTabbedPane1.addTab("Settings", settingsPanel);

        fileExplorerPanel.getFileTree().addTreeSelectionListener(e -> {
            File selectedDir = fileExplorerPanel.getSelectedDirectory();
            if (selectedDir != null) {
                largeFileViewPanel.updateFolder(selectedDir);
            }
        });

        // Initialize the timeline panel
        timelinePanelObject = new TimelinePanel(); // Initialize it
        timelinePanelObject.getImageList().setTransferHandler(new TimelinePanel.ListItemTransferHandler());
        TimelinePanel.setLayout(new BorderLayout()); // Ensure layout is set
        TimelinePanel.add(timelinePanelObject, BorderLayout.CENTER);
        TimelinePanel.revalidate();
        TimelinePanel.repaint();
        // Set the timeline change listener so that any reordering refreshes the main image display.
        timelinePanelObject.setTimelineChangeListener(() -> {
            updateImage();
        });

        // Add selection listener for image changes
        timelinePanelObject.getImageList().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Ensure it's not firing multiple times unnecessarily
                Slide selectedItem = timelinePanelObject.getImageList().getSelectedValue();
                if (selectedItem != null) {
                    updateImage(selectedItem.getImageFile());
                    updateTransitionBox();  // see next step for updating transitions
                }
            }
        });
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                thumbnailExecutor.shutdown();
                System.out.println("Thumbnail executor shutdown.");
            }
        });
    }

    // Calculate total slideshow duration (assuming each image is shown for 5 seconds)
    private int calculateTotalSlideshowDuration() {
        int numImages = timelinePanelObject.getImages().size();
        SlideshowSettings settings = settingsPanel.getSlideshowSettings();

        int estimatedDuration = numImages * settings.duration;

        // Ensure a reasonable minimum duration (e.g., 15 seconds)
        return estimatedDuration / 1000;
    }

    // Update when images or audio change
    private void updateAudioTimeline() {
        if (audioTimelinePanel != null) {
            TimelinePanel.remove(audioTimelinePanel); // Remove old panel
        }
        
        SlideshowSettings settings = settingsPanel.getSlideshowSettings();
        audioTimelinePanel = new AudioTimelinePanel(audioFiles, calculateTotalSlideshowDuration(), settings.autoMode);
        TimelinePanel.add(audioTimelinePanel, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    
    // This all needs to be moved into its own class.
    // Called when an image is selected in the timeline.
    // This version updates the transitions tab (transitionBox) so that it shows the current image's transition.
    private void updateTransitionBox() {
        Slide selectedItem = timelinePanelObject.getImageList().getSelectedValue();
        if (selectedItem == null) return;
        TransitionType currentTransition = selectedItem.getTransition();
        String displayText;
        switch (currentTransition) {
            case INSTANT:
                displayText = "No Transition";
                break;
            case CROSS_FADE:
                displayText = "Cross Fade";
                break;
            case WIPE_UP:
                displayText = "Wipe Up";
                break;
            case WIPE_RIGHT:
                displayText = "Wipe Right";
                break;
            case WIPE_DOWN:
                displayText = "Wipe Down";
                break;
            case WIPE_LEFT:
                displayText = "Wipe Left";
                break;
            default:
                displayText = "No Transition";
        }
        transitionBox.setSelectedItem(displayText);
    }

    // Apply saved theme
    private void applySavedTheme() {
        SwingUtilities.invokeLater(() -> {
            String theme = prefs.get("theme", "light"); // Default to light mode
            try {
                if ("dark".equals(theme)) {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                }
                SwingUtilities.updateComponentTreeUI(this);
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(SlideshowCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    // Method to get files from a slide list.
    private List<File> getFilesFromSlideList(List<Slide> slides){
        List<File> files = new ArrayList<>();
        for(Slide slide: slides){
            files.add(new File(slide.getImagePath()));
        }
        return files;
    }

    private List<Slide> getSlides() {
        List<Slide> slides = new ArrayList<>();
        List<File> orderedImages = timelinePanelObject.getImages();
        for (File imageFile : orderedImages) {
            slides.add(new Slide(imageFile.getAbsolutePath(), imageFile, TransitionType.INSTANT)); // Default transition
        }
        return slides;
    }

    private String getAudioPath() {
        // Implement this method to get the audio path
        return null; // Example
    }

    private boolean isLoop() {
        return true;
    }

    public void updateImage() {
        if (!timelinePanelObject.getImages().isEmpty()) {
            updateImage(timelinePanelObject.getImages().get(0)); // Load first image initially
        }
    }


    private void updateImage(File selectedFile) {
        if (selectedFile == null || !selectedFile.exists()) {
            imageLabel.setIcon(null);
            return;
        }

        int labelWidth = imageLabel.getWidth();
        int labelHeight = imageLabel.getHeight();
        if (labelWidth <= 0 || labelHeight <= 0) {
            labelWidth = imageLabel.getPreferredSize().width;
            labelHeight = imageLabel.getPreferredSize().height;
        }

        ImageIcon originalIcon = new ImageIcon(selectedFile.getAbsolutePath());
        Image originalImage = originalIcon.getImage();

        double widthRatio = (double) labelWidth / originalImage.getWidth(null);
        double heightRatio = (double) labelHeight / originalImage.getHeight(null);
        double scaleRatio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalImage.getWidth(null) * scaleRatio);
        int newHeight = (int) (originalImage.getHeight(null) * scaleRatio);

        Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        ImageIcon resizedIcon = new ImageIcon(resizedImage);

        imageLabel.setIcon(resizedIcon);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        fileExplorerHolder = new javax.swing.JPanel();
        largeFileViewHolder = new javax.swing.JPanel();
        transitionsHolder = new javax.swing.JPanel();
        transitionTest = new javax.swing.JButton();
        transitionBox = new javax.swing.JComboBox<>();
        musicHolder = new javax.swing.JPanel();
        addAudioButton = new javax.swing.JButton();
        playAudioButton = new javax.swing.JButton();
        skipAudioButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        settingsHolder = new javax.swing.JPanel();
        imageContainer = new javax.swing.JPanel();
        presenterButton = new javax.swing.JButton();
        imageLabel = new javax.swing.JLabel();
        spacerPanel = new javax.swing.JPanel();
        TimelinePanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        jMenu = new javax.swing.JMenu();
        createNewSlideMenuItem = new javax.swing.JMenuItem();
        openPreviousSlideMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        ThemesButton = new javax.swing.JMenu();
        LightMode = new javax.swing.JMenuItem();
        DarkMode = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Slideshow Creator");

        jSplitPane1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jSplitPane1.setDividerLocation(500);

        javax.swing.GroupLayout fileExplorerHolderLayout = new javax.swing.GroupLayout(fileExplorerHolder);
        fileExplorerHolder.setLayout(fileExplorerHolderLayout);
        fileExplorerHolderLayout.setHorizontalGroup(
            fileExplorerHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        fileExplorerHolderLayout.setVerticalGroup(
            fileExplorerHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 470, Short.MAX_VALUE)
        );

        jSplitPane2.setLeftComponent(fileExplorerHolder);

        javax.swing.GroupLayout largeFileViewHolderLayout = new javax.swing.GroupLayout(largeFileViewHolder);
        largeFileViewHolder.setLayout(largeFileViewHolderLayout);
        largeFileViewHolderLayout.setHorizontalGroup(
            largeFileViewHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 494, Short.MAX_VALUE)
        );
        largeFileViewHolderLayout.setVerticalGroup(
            largeFileViewHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 470, Short.MAX_VALUE)
        );

        jSplitPane2.setRightComponent(largeFileViewHolder);

        jTabbedPane1.addTab("Files", jSplitPane2);

        transitionTest.setText("Preview Transition");

        transitionBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "No Transition", "Cross Fade", "Wipe Up", "Wipe Right", "Wipe Down", "Wipe Left" }));

        javax.swing.GroupLayout transitionsHolderLayout = new javax.swing.GroupLayout(transitionsHolder);
        transitionsHolder.setLayout(transitionsHolderLayout);
        transitionsHolderLayout.setHorizontalGroup(
            transitionsHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transitionsHolderLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transitionsHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(transitionTest)
                    .addComponent(transitionBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(367, Short.MAX_VALUE))
        );
        transitionsHolderLayout.setVerticalGroup(
            transitionsHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transitionsHolderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(transitionTest)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(transitionBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(413, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Transitions", transitionsHolder);

        addAudioButton.setText("Add Audio File");
        addAudioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAudioButtonActionPerformed(evt);
            }
        });

        playAudioButton.setText("Play Audio");
        playAudioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playAudioButtonActionPerformed(evt);
            }
        });

        skipAudioButton.setText("Skip Current Audio");
        skipAudioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                skipAudioButtonActionPerformed(evt);
            }
        });

        jButton1.setText("Stop Audio");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout musicHolderLayout = new javax.swing.GroupLayout(musicHolder);
        musicHolder.setLayout(musicHolderLayout);
        musicHolderLayout.setHorizontalGroup(
            musicHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(musicHolderLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(musicHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(addAudioButton)
                    .addGroup(musicHolderLayout.createSequentialGroup()
                        .addComponent(playAudioButton)
                        .addGap(18, 18, 18)
                        .addComponent(skipAudioButton)
                        .addGap(18, 18, 18)
                        .addComponent(jButton1)))
                .addContainerGap(151, Short.MAX_VALUE))
        );
        musicHolderLayout.setVerticalGroup(
            musicHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(musicHolderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addAudioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(musicHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(playAudioButton)
                    .addComponent(skipAudioButton)
                    .addComponent(jButton1))
                .addContainerGap(412, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Music", musicHolder);

        javax.swing.GroupLayout settingsHolderLayout = new javax.swing.GroupLayout(settingsHolder);
        settingsHolder.setLayout(settingsHolderLayout);
        settingsHolderLayout.setHorizontalGroup(
            settingsHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 499, Short.MAX_VALUE)
        );
        settingsHolderLayout.setVerticalGroup(
            settingsHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 470, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Settings", settingsHolder);

        jSplitPane1.setLeftComponent(jTabbedPane1);
        jTabbedPane1.getAccessibleContext().setAccessibleName("Files");

        imageContainer.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        presenterButton.setText("Open Presenter");
        presenterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                presenterButtonActionPerformed(evt);
            }
        });

        imageLabel.setBackground(new java.awt.Color(102, 102, 102));
        imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout imageContainerLayout = new javax.swing.GroupLayout(imageContainer);
        imageContainer.setLayout(imageContainerLayout);
        imageContainerLayout.setHorizontalGroup(
            imageContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, imageContainerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(imageContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(imageContainerLayout.createSequentialGroup()
                        .addGap(0, 396, Short.MAX_VALUE)
                        .addComponent(presenterButton)))
                .addContainerGap())
        );
        imageContainerLayout.setVerticalGroup(
            imageContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, imageContainerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(presenterButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 462, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(imageContainer);

        spacerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        spacerPanel.setMaximumSize(new java.awt.Dimension(32767, 16));
        spacerPanel.setMinimumSize(new java.awt.Dimension(100, 16));

        javax.swing.GroupLayout spacerPanelLayout = new javax.swing.GroupLayout(spacerPanel);
        spacerPanel.setLayout(spacerPanelLayout);
        spacerPanelLayout.setHorizontalGroup(
            spacerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        spacerPanelLayout.setVerticalGroup(
            spacerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        TimelinePanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        TimelinePanel.setMaximumSize(new java.awt.Dimension(32767, 150));
        TimelinePanel.setMinimumSize(new java.awt.Dimension(0, 150));
        TimelinePanel.setName(""); // NOI18N
        TimelinePanel.setPreferredSize(new java.awt.Dimension(0, 150));

        javax.swing.GroupLayout TimelinePanelLayout = new javax.swing.GroupLayout(TimelinePanel);
        TimelinePanel.setLayout(TimelinePanelLayout);
        TimelinePanelLayout.setHorizontalGroup(
            TimelinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        TimelinePanelLayout.setVerticalGroup(
            TimelinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 148, Short.MAX_VALUE)
        );

        menuBar.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        jMenu.setText("File");

        createNewSlideMenuItem.setText("Create New Slide");
        createNewSlideMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createNewSlideMenuItemActionPerformed(evt);
            }
        });
        jMenu.add(createNewSlideMenuItem);

        openPreviousSlideMenuItem.setText("Open Previous Slide");
        openPreviousSlideMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openPreviousSlideMenuItemActionPerformed(evt);
            }
        });
        jMenu.add(openPreviousSlideMenuItem);

        saveMenuItem.setText("Save");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        jMenu.add(saveMenuItem);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        jMenu.add(exitMenuItem);

        menuBar.add(jMenu);

        ThemesButton.setText("Themes");

        LightMode.setText("FlatLaf Light");
        LightMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LightModeActionPerformed(evt);
            }
        });
        ThemesButton.add(LightMode);

        DarkMode.setText("FlatLaf Dark");
        DarkMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DarkModeActionPerformed(evt);
            }
        });
        ThemesButton.add(DarkMode);

        menuBar.add(ThemesButton);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSplitPane1)
                    .addComponent(spacerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(TimelinePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1028, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spacerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(TimelinePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Launches presenter application
    private void presenterButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // Retrieve images from the timeline panel instead of the separate imageFiles list.
        List<File> images = timelinePanelObject.getImages();
        if (images != null && !images.isEmpty()) {
            File[] imageArray = images.toArray(new File[0]);
            // Retrieve the slideshow settings from the settings panel.
            SlideshowSettings settings = settingsPanel.getSlideshowSettings();
            
            // Extract transitions from the timeline items:
            List<Slide> SlidesList = timelinePanelObject.getSlideItems();
            TransitionType[] slideTransitions = new TransitionType[SlidesList.size()];
            for (int i = 0; i < SlidesList.size(); i++) {
                slideTransitions[i] = SlidesList.get(i).getTransition();
            }
            
            // Launch the presenter using the images and transitions from the timeline.
            new SlideshowPresenter(imageArray, audioFiles, settings.duration, settings.loop, settings.autoMode, slideTransitions)
                    .setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "No images to present.");
        }
    }
    

    // Sets UI design to FlatLightLaf (light mode version of Flat Laf)
    private void LightModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LightModeActionPerformed
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                SwingUtilities.updateComponentTreeUI(this);
                prefs.put("theme", "light"); // Save preference
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(SlideshowCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }//GEN-LAST:event_LightModeActionPerformed

    // Sets UI design to FlatDarkLaf (dark mode version of Flat Laf)
    private void DarkModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DarkModeActionPerformed
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                SwingUtilities.updateComponentTreeUI(this);
                prefs.put("theme", "dark"); // Save preference
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(SlideshowCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }//GEN-LAST:event_DarkModeActionPerformed

    // Overwrites the currently working file as long as it exists in the folder already, allowing easy updates
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        if (currentSlideshowName == null) {
            JOptionPane.showMessageDialog(this, "Please add images to create a slideshow first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        File slideshowDir = SlideShowFileManager.getSlideshowDirectory(currentSlideshowName);
        if (!slideshowDir.exists()) {
            slideshowDir.mkdirs();
        }
    
        File file = new File(slideshowDir, currentSlideshowName + ".json");
    
        boolean loop = settingsPanel.getPlaybackMode().equals("Loop Slideshow");
        String mode = settingsPanel.getSelectedMode();
        int interval;
        try {
            interval = (int) Math.round(Double.parseDouble(settingsPanel.getIntervalText().trim()));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid interval value. Using default interval of 3 seconds.");
            interval = 3;
        }
    
        List<Slide> slides = getSlides();
        List<String> transitions = getTransitionsAsStringList(); // Helper method converting imageTransitions to List<String>
    
        SlideshowSettingsSaver.saveSettingsToJson(
                file.getAbsolutePath(), 
                currentSlideshowName, 
                slides, 
                audioFiles, 
                loop, 
                mode, 
                interval, 
                transitions
        );
        updateAudioTimeline();
        JOptionPane.showMessageDialog(this, "Slideshow settings saved successfully.");
    }
    
    // Allows user to save currently created slideshow
    private void openPreviousSlideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Files", "json");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            currentSlideshowFile = file;
            loadSlideshowSettings(file);  // Add this call
            if (audioFiles != null && !audioFiles.isEmpty()) {
                updateAudioTimeline();
            }
        }
    }                                                         

    private void loadSlideshowSettings(File file) {
        try {
            // Read file contents
            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject json = new JSONObject(content);
    
            // Load basic slideshow settings
            currentSlideshowName = json.getString("name");
            boolean loop = json.getBoolean("loop");
            String mode = json.getString("mode");
            int interval = json.getInt("interval");
    
            // Update the settings panel (ensure these methods exist in SettingsPanel)
            settingsPanel.setPlaybackMode(loop ? "Loop Slideshow" : "Single Play");
            settingsPanel.setSelectedMode(mode);
            settingsPanel.setIntervalText(String.valueOf(interval));
    
            // Load audio files if available
            audioFiles.clear();
            if (json.has("audio")) {
                JSONArray audioArray = json.getJSONArray("audio");
                for (int i = 0; i < audioArray.length(); i++) {
                    String audioPath = audioArray.getString(i);
                    audioFiles.add(new File(audioPath));
                }
                updateAudioTimeline();
            }
    
            // Load slides (each with its transition)
            JSONArray slidesArray = json.getJSONArray("slides");
            List<Slide> SlidesList = new ArrayList<>();
            for (int i = 0; i < slidesArray.length(); i++) {
                JSONObject slideObj = slidesArray.getJSONObject(i);
                String imagePath = slideObj.getString("image");
                String transitionStr = slideObj.optString("transition", "INSTANT");
                TransitionType transition = TransitionType.valueOf(transitionStr);
                Slide item = new Slide(imagePath, new File(imagePath), transition);
                SlidesList.add(item);
            }
            // Update the timeline panel with the loaded timeline items.
            timelinePanelObject.setTimelineSlides(SlidesList);
    
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading slideshow settings: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    // Selects image to add to our image folder and adds it sequentially to the image index for display
    private void createNewSlideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fileChooser = createFileChooser(JFileChooser.FILES_ONLY, true);
        int returnValue = fileChooser.showOpenDialog(this);
    
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            processSelectedFiles(selectedFiles);
            
            // Optionally, after processing, automatically save the new slideshow.
            if (currentSlideshowName != null) {
                saveMenuItemActionPerformed(evt); // Or prompt the user to save
            }
        } else {
            System.out.println("No image selected.");
        }
    }                                                      

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.exit(0); // Terminate the application
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void addAudioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAudioButtonActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("WAV Audio Files", "wav"));

        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            for (File file : fileChooser.getSelectedFiles()) {
                addAudioFile(file);
            }
            updateAudioTimeline();
        }
    }//GEN-LAST:event_addAudioButtonActionPerformed

    private void playAudioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playAudioButtonActionPerformed
        if (isPlaying) {
            return;
        }
        
        if (audioFiles != null && !audioFiles.isEmpty()) {
        isPlaying = true; 
        stopRequested = false; // Reset stop request when starting

        Thread thread = new Thread(() -> {
            try {
                for (File audioFile : audioFiles) {
                    if (stopRequested) {
                        break; // Stop immediately if stop button was pressed
                    }

                    System.out.println("Playing file: " + audioFile.getAbsolutePath());
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                    audioClip = AudioSystem.getClip();
                    audioClip.open(audioStream);

                    final Object lock = new Object();
                    audioClip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    });

                    audioClip.start();

                    synchronized (lock) {
                        while (audioClip.isRunning() && !stopRequested) {
                            lock.wait(); // Wait until audio stops or stop is requested
                        }
                    }

                    audioClip.close();
                    audioStream.close();

                    if (stopRequested) {
                        break; // Exit loop if stop was requested
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Error playing audio.");
            } finally {
                isPlaying = false; // Reset flag when done
                stopRequested = false; // Reset stop request
            }
        });

        thread.start();
    } else {
        System.out.println("No audio files available.");
    }
    }//GEN-LAST:event_playAudioButtonActionPerformed

    private void skipAudioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_skipAudioButtonActionPerformed
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
        }
        if (audioClip == null) {
            isPlaying = false;
        }
    }//GEN-LAST:event_skipAudioButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if (audioClip != null && audioClip.isRunning()) {
            while (audioClip != null && audioClip.isRunning()) {
                audioClip.stop();
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    public void addAudioFile(File audioFile) {
        if (audioFile != null && audioFile.getName().toLowerCase().endsWith(".wav")) {
            audioFiles.add(audioFile);
            if (audioFiles.size() == 1) {
                audioIndex = 0; // Set to first file if it's the first one added
            }
        } else {
            System.out.println("Invalid file format. Only .wav files are supported.");
        }
    }

    // Helper method to convert imageTransitions to a List of Strings.
    private List<String> getTransitionsAsStringList() {
        List<String> transitions = new ArrayList<>();
        for (int i = 0; i < timelinePanelObject.getImageList().getModel().getSize(); i++) {
            Slide item = (Slide) timelinePanelObject.getImageList().getModel().getElementAt(i);
            transitions.add(item.getTransition().toString());
        }
        return transitions;
    }
    
    private JFileChooser createFileChooser(int selectionMode, boolean multiSelection) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(selectionMode);
        fileChooser.setMultiSelectionEnabled(multiSelection);
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
                "Image files (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(imageFilter);
        fileChooser.setFileView(createFileView(fileChooser)); // Pass the file chooser instance
        return fileChooser;
    }


    private FileView createFileView(final JFileChooser chooser) {
        return new FileView() {
            // Cache thumbnails...
            private final Map<File, Icon> thumbnailCache = new HashMap<>();
            private final Icon placeholderIcon = new ImageIcon(
                    new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB));
                    
            @Override
            public Icon getIcon(File f) {
                if (f.isFile() && isImageFile(f)) {
                    Icon cachedIcon = thumbnailCache.get(f);
                    if (cachedIcon != null) {
                        return cachedIcon;
                    } else {
                        thumbnailExecutor.submit(() -> {
                            Icon icon = getThumbnailIcon(f);
                            if (icon != null) {
                                synchronized (thumbnailCache) {
                                    thumbnailCache.put(f, icon);
                                }
                                SwingUtilities.invokeLater(() -> {
                                    chooser.repaint();
                                });
                            }
                        });
                        return placeholderIcon;
                    }
                }
                return super.getIcon(f);
            }

            private Icon getThumbnailIcon(File file) {
                try {
                    BufferedImage thumbnail = Thumbnails.of(file)
                        .size(50, 50) // Desired thumbnail size
                        .asBufferedImage();
                    return new ImageIcon(thumbnail);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null; // Handle error, maybe return a default icon
                }
            }

            private boolean isImageFile(File file) {
                String fileName = file.getName().toLowerCase();
                return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                        fileName.endsWith(".png") || fileName.endsWith(".gif");
            }
        };
    }

    private void updateImageFiles(List<File> imageList) {
        imageFiles = imageList;  // Direct assignment, no need to convert to an array
        updateImage();
        timelinePanelObject.setImages(imageList);
        if (!imageList.isEmpty()) {
            timelinePanelObject.getImageList().setSelectedIndex(0);
            timelinePanelObject.getImageList().ensureIndexIsVisible(0);
        }
        timelinePanelObject.revalidate();
        timelinePanelObject.repaint();
    }

    private void processSelectedFiles(File[] selectedFiles) {
        if (currentSlideshowName == null) {
            currentSlideshowName = JOptionPane.showInputDialog(this, "Enter Slideshow Name:", "New Slideshow", JOptionPane.PLAIN_MESSAGE);
            if (currentSlideshowName == null || currentSlideshowName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Slideshow name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
             // Check if slideshow name already exists
            File slideshowDir = SlideShowFileManager.getSlideshowDirectory(currentSlideshowName);
            if (slideshowDir.exists() && slideshowDir.isDirectory() && slideshowDir.list().length > 0) {
                int choice = JOptionPane.showConfirmDialog(this, "Slideshow '" + currentSlideshowName + "' already exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.NO_OPTION) {
                    currentSlideshowName = null; // Reset name to prompt again
                    processSelectedFiles(selectedFiles); // Recursive call to get a new name
                    return; // Exit current execution
                } else {
                    // User chose yes, clear existing images
                    clearExistingImages();
                }
            }
        }
        
        List<File> newImages = new ArrayList<>();
        File targetFolder = SlideShowFileManager.getImagesFolder(currentSlideshowName);
        
        new SwingWorker<Void, File>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (File selectedFile : selectedFiles) {
                    File targetFile = new File(targetFolder, selectedFile.getName());
                    targetFile = avoidDuplicateFileNames(targetFile, selectedFile, targetFolder);
                    copyImageFile(selectedFile, newImages, targetFile);
                    publish(targetFile);  // Publish each processed file for incremental update
                }
                return null;
            }
            
            @Override
            protected void process(List<File> chunks) {
                // Update the timeline panel incrementally
                timelinePanelObject.setImages(newImages);
                if (!newImages.isEmpty()) {
                    timelinePanelObject.getImageList().setSelectedIndex(0);
                    timelinePanelObject.getImageList().ensureIndexIsVisible(0);
                }
                timelinePanelObject.revalidate();
                timelinePanelObject.repaint();
            }

            @Override
            protected void done() {
                updateImageFiles(newImages);
                // Immediately save the slideshow settings after new images are processed.
                File slideshowDir = SlideShowFileManager.getSlideshowDirectory(currentSlideshowName);
                if (!slideshowDir.exists()) {
                    slideshowDir.mkdirs();
                }
                File file = new File(slideshowDir, currentSlideshowName + ".json");
                
                boolean loop = settingsPanel.getPlaybackMode().equals("Loop Slideshow");
                String mode = settingsPanel.getSelectedMode();
                int interval;
                try {
                    interval = (int) Math.round(Double.parseDouble(settingsPanel.getIntervalText().trim()));
                } catch (NumberFormatException ex) {
                    interval = 3; // default interval (in seconds)
                }
                
                SlideshowSettingsSaver.saveSettingsToJson(
                    file.getAbsolutePath(),
                    currentSlideshowName,
                    getSlides(),         // Your list of Slide objects
                    audioFiles,
                    loop,
                    mode,
                    interval,
                    getTransitionsAsStringList()  // Helper method that retrieves transitions from Slide objects
                );
            }
        }.execute();
    }
    
    private void clearExistingImages() {
        // Clear the imageFiles list
        imageFiles.clear();
         // Clear the images from the images folder
        File imagesFolder = SlideShowFileManager.getImagesFolder(currentSlideshowName);
        if (imagesFolder.exists() && imagesFolder.isDirectory()) {
            File[] files = imagesFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    private File avoidDuplicateFileNames(File targetFile, File selectedFile, File targetFolder) {
        int counter = 1;
        while (targetFile.exists()) {
            int dotIndex = selectedFile.getName().lastIndexOf('.');
            String nameWithoutExt = (dotIndex > 0) ? selectedFile.getName().substring(0, dotIndex) : selectedFile.getName();
            String ext = (dotIndex > 0) ? selectedFile.getName().substring(dotIndex) : "";
            targetFile = new File(targetFolder, nameWithoutExt + " (" + counter + ")" + ext);
            counter++;
        }
        return targetFile;
    }

    private void copyImageFile(File file, List<File> imageList, File targetFile) {
        Path source = file.toPath();
        Path target = targetFile.toPath();
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            imageList.add(target.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error copying image: " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".gif");
    }

    private String promptForSlideshowName() {
        return JOptionPane.showInputDialog(this, "Enter Slideshow Name:", "Save Slideshow", JOptionPane.PLAIN_MESSAGE);
    }

    // Sets the transition type for the current image
    private void transitionBoxActionPerformed(java.awt.event.ActionEvent evt) {                                              
        int selectionIndex = transitionBox.getSelectedIndex();
        Slide selectedItem = timelinePanelObject.getImageList().getSelectedValue();
        if (selectedItem != null) {
            // Assuming the enum order of TransitionType matches the combo box order:
            TransitionType newTransition = TransitionType.values()[selectionIndex];
            selectedItem.setTransition(newTransition);
            System.out.println("Transition updated to: " + newTransition);
        }
    }                                             

    private void transitionTestActionPerformed(java.awt.event.ActionEvent evt) {
        // Get the currently selected Slide from the timeline panel
        Slide selectedItem = timelinePanelObject.getImageList().getSelectedValue();
        if (selectedItem == null) {
            System.out.println("No slide selected for transition.");
            return;
        }
        ImageIcon labelIcon = (ImageIcon) imageLabel.getIcon();
        if (labelIcon == null) {
            System.out.println("No image available for transition.");
            return;
        }
        
        // Use the current image as the "next" image.
        BufferedImage nextImage = Transition.toBufferedImage(labelIcon.getImage());
        int currentIndex = timelinePanelObject.getImageList().getSelectedIndex();
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        
        // Use the ordered image list from the timeline panel
        List<File> currentImages = timelinePanelObject.getImages();
        BufferedImage prevImage = nextImage;
        if (currentImages.size() >= 2) {
            int wrapIndex = (currentIndex - 1 >= 0) ? currentIndex - 1 : currentImages.size() - 1;
            ImageIcon prevIcon = new ImageIcon(currentImages.get(wrapIndex).getAbsolutePath());
            prevImage = Transition.toBufferedImage(prevIcon.getImage());
        } else {
            try {
                Image placeholder = new ImageIcon("Placeholder.png").getImage();
                if (placeholder != null) {
                    prevImage = Transition.toBufferedImage(placeholder);
                }
            } catch (Exception e) {
                System.out.println("Error obtaining default image for transitions: " + e);
            }
        }
        
        // Instead of using imageTransitions, get the transition from the selected item
        TransitionType type = selectedItem.getTransition();
        transitionManager.doTransition(prevImage, nextImage, imageLabel, type);
    }
    
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SlideshowCreator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SlideshowCreator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SlideshowCreator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SlideshowCreator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SlideshowCreator().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem DarkMode;
    private javax.swing.JMenuItem LightMode;
    private javax.swing.JMenu ThemesButton;
    private javax.swing.JPanel TimelinePanel;
    private javax.swing.JButton addAudioButton;
    private javax.swing.JMenuItem createNewSlideMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JPanel fileExplorerHolder;
    private javax.swing.JPanel imageContainer;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JMenu jMenu;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel largeFileViewHolder;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JPanel musicHolder;
    private javax.swing.JMenuItem openPreviousSlideMenuItem;
    private javax.swing.JButton playAudioButton;
    private javax.swing.JButton presenterButton;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JPanel settingsHolder;
    private javax.swing.JButton skipAudioButton;
    private javax.swing.JPanel spacerPanel;
    private javax.swing.JComboBox<String> transitionBox;
    private javax.swing.JButton transitionTest;
    private javax.swing.JPanel transitionsHolder;
    // End of variables declaration//GEN-END:variables
}
