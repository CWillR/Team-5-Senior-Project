/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.team5.senior_project;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileView;
import net.coobird.thumbnailator.Thumbnails;
import java.awt.image.BufferedImage;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SlideshowCreator extends javax.swing.JFrame {

    // List to hold each slide's transition
    private List<TransitionType> imageTransitions = new ArrayList<>();
    private List<File> imageFiles = new ArrayList<>(); // image list
    private final List<File> audioFiles = new ArrayList<>(); // audio list
    private int audioIndex = 0; // tracks current audio track
    private final Preferences prefs = Preferences.userNodeForPackage(SlideshowCreator.class);
    private final List<Slide> slides = new ArrayList<>();
    private File currentSlideshowFile = null;
    private TimelinePanel timelinePanelObject; // our timeline panel
    private final Transition transitionManager = new Transition();
    private String currentSlideshowName = null; // current slideshow name
    private AudioTimelinePanel audioTimelinePanel;
    private boolean autoMode = false;
    private final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(4);

    /**
     * Creates new form SlideshowCreator
     */
    public SlideshowCreator() {
        initComponents();
        applySavedTheme(); // Apply saved theme

        // Set up file explorer and large file view panels.
        FileExplorerPanel fileExplorerPanel = new FileExplorerPanel();
        fileExplorerHolder.removeAll();
        fileExplorerHolder.setLayout(new BorderLayout());
        fileExplorerHolder.add(fileExplorerPanel, BorderLayout.CENTER);
        fileExplorerHolder.revalidate();
        fileExplorerHolder.repaint();

        File defaultFolder = new File(System.getProperty("user.home"));
        LargeFileViewPanel largeFileViewPanel = new LargeFileViewPanel(defaultFolder);
        largeFileViewHolder.removeAll();
        largeFileViewHolder.setLayout(new BorderLayout());
        largeFileViewHolder.add(largeFileViewPanel, BorderLayout.CENTER);
        largeFileViewHolder.revalidate();
        largeFileViewHolder.repaint();

        fileExplorerPanel.getFileTree().addTreeSelectionListener(e -> {
            File selectedDir = fileExplorerPanel.getSelectedDirectory();
            if (selectedDir != null) {
                largeFileViewPanel.updateFolder(selectedDir);
            }
        });

        // Initialize the timeline panel.
        timelinePanelObject = new TimelinePanel();
        TimelinePanel.setLayout(new BorderLayout());
        TimelinePanel.add(timelinePanelObject, BorderLayout.CENTER);
        TimelinePanel.revalidate();
        TimelinePanel.repaint();

        // When the timeline selection changes, update the main image and transition drop‑down.
        timelinePanelObject.getImageList().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                File selectedFile = timelinePanelObject.getImageList().getSelectedValue();
                if (selectedFile != null) {
                    updateImage(selectedFile);
                    updateTransitionBox();
                }
            }
        });

        // Initially hide controls that depend on mode.
        intervalTextField.setVisible(false);
        modeSelectionLabel.setVisible(false);
        intervalText.setVisible(false);
        secondsText.setVisible(false);
        transitionComboBox.setVisible(false);
        transitionLabel.setVisible(false);
        modeComboBox.setVisible(false);

        // Mode combo box listener.
        modeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMode();
            }
        });

        // Shutdown executor on window closing.
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                thumbnailExecutor.shutdown();
                System.out.println("Thumbnail executor shutdown.");
            }
        });
    }

    // Calculates total slideshow duration.
    private int calculateTotalSlideshowDuration() {
        int numImages = timelinePanelObject.getImages().size();
        int imageDuration = 5; // seconds per image
        int estimatedDuration = numImages * imageDuration;
        return Math.max(estimatedDuration, 15);
    }

    // Updates the transition drop-down to reflect the current slide's saved transition.
    private void updateTransitionBox() {
        int currentIndex = timelinePanelObject.getImageList().getSelectedIndex();
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        // Grow the transitions list if needed.
        while (imageTransitions.size() <= currentIndex) {
            imageTransitions.add(TransitionType.INSTANT); // Default
        }
        TransitionType currentTransition = imageTransitions.get(currentIndex);
        transitionBox.setSelectedIndex(currentTransition.ordinal());
    }

    // Update the audio timeline panel.
    private void updateAudioTimeline() {
        if (audioTimelinePanel != null) {
            TimelinePanel.remove(audioTimelinePanel);
        }
        audioTimelinePanel = new AudioTimelinePanel(audioFiles, calculateTotalSlideshowDuration());
        TimelinePanel.add(audioTimelinePanel, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    // Apply saved theme.
    private void applySavedTheme() {
        SwingUtilities.invokeLater(() -> {
            String theme = prefs.get("theme", "light");
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

    // Save settings including transitions.
    private void saveSlideshowSettings(File file) {
        String filePath = file.getAbsolutePath();
        String slideshowName = file.getName().replaceFirst("[.][^.]+$", "");
        List<Slide> slides = getSlides();
        boolean loop = isLoop();
        String selectedMode = (String) modeComboBox.getSelectedItem();
        int interval = 0;
        if ("Preset Duration".equals(selectedMode)) {
            try {
                interval = Integer.parseInt(intervalTextField.getText().trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid interval input. Setting interval to 0.");
                interval = 0;
            }
        }
        // Build list of transitions as strings.
        List<String> transitionsList = new ArrayList<>();
        for (TransitionType t : imageTransitions) {
            transitionsList.add(t.toString());
        }
        SlideshowSettingsSaver.saveSettingsToJson(filePath, slideshowName, slides, audioFiles, loop, selectedMode, interval, transitionsList);
    }

    // Load settings including transitions.
    private void loadSlideshowSettings(File file) {
        modeComboBox.setVisible(true);
        modeSelectionLabel.setVisible(true);
        transitionComboBox.setVisible(true);
        transitionLabel.setVisible(true);
        try {
            currentSlideshowName = file.getParentFile().getName();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            reader.close();
            String jsonString = jsonContent.toString();
            System.out.println("JSON Content: " + jsonString);
            JSONObject json = new JSONObject(jsonString);
            JSONArray slides = json.getJSONArray("slides");
            imageFiles.clear();
            for (int i = 0; i < slides.length(); i++) {
                JSONObject slideObject = slides.getJSONObject(i);
                String imagePath = slideObject.getString("image");
                File imageFile = new File(imagePath);
                imageFiles.add(imageFile);
            }
            updateImageFiles(imageFiles);
            if (json.has("audio")) {
                JSONArray audioArray = json.getJSONArray("audio");
                for (int i = 0; i < audioArray.length(); i++) {
                    audioFiles.add(new File(audioArray.getString(i)));
                }
                updateAudioTimeline();
            }
            if (json.has("mode")) {
                String savedMode = json.getString("mode");
                modeComboBox.setSelectedItem(savedMode);
                updateMode();
                if ("Preset Duration".equals(savedMode) && json.has("interval")) {
                    int savedInterval = json.getInt("interval");
                    intervalTextField.setText(String.valueOf(savedInterval));
                    intervalTextField.setVisible(true);
                    intervalText.setVisible(true);
                    secondsText.setVisible(true);
                } else {
                    intervalTextField.setVisible(false);
                    intervalText.setVisible(false);
                    secondsText.setVisible(false);
                }
            }
            // Load transitions if available.
            if (json.has("transitions")) {
                JSONArray transitionsArray = json.getJSONArray("transitions");
                imageTransitions.clear();
                for (int i = 0; i < transitionsArray.length(); i++) {
                    String transitionStr = transitionsArray.getString(i);
                    TransitionType t;
                    try {
                        t = TransitionType.valueOf(transitionStr.toUpperCase().replace(" ", "_"));
                    } catch (Exception e) {
                        t = TransitionType.INSTANT;
                    }
                    imageTransitions.add(t);
                }
            } else {
                imageTransitions.clear();
                for (int i = 0; i < imageFiles.size(); i++) {
                    imageTransitions.add(TransitionType.INSTANT);
                }
            }
            // Finally, update the transition drop-down to match the first image.
            updateTransitionBox();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading slideshow settings.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Invalid JSON format in slideshow settings.", "Error", JOptionPane.ERROR_MESSAGE);
            System.out.println("Error parsing JSON: " + e.getMessage());
        }
    }

    // Returns a list of Slide objects for the current order.
    private List<Slide> getSlides() {
        List<Slide> slides = new ArrayList<>();
        List<File> orderedImages = timelinePanelObject.getImages();
        for (File imageFile : orderedImages) {
            slides.add(new Slide(imageFile.getAbsolutePath()));
        }
        return slides;
    }

    // Dummy implementations for audio path and loop.
    private String getAudioPath() {
        return null;
    }

    private boolean isLoop() {
        return true;
    }

    // Update timeline (if needed).
    private void updateTimeline() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Slide slide : slides) {
            listModel.addElement(slide.getImagePath());
        }
    }

    public void updateImage() {
        if (!timelinePanelObject.getImages().isEmpty()) {
            updateImage(timelinePanelObject.getImages().get(0));
        }
    }

    // Update the main image display.
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
        imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
    }

    // Update mode (manual vs preset).
    private void updateMode() {
        String selectedMode = (String) modeComboBox.getSelectedItem();
        if ("Manual Duration".equals(selectedMode)) {
            System.out.println("Manual Duration");
            autoMode = false;
            intervalText.setVisible(false);
            secondsText.setVisible(false);
            intervalTextField.setVisible(false);
            manualSlideChange();
        } else if ("Preset Duration".equals(selectedMode)) {
            System.out.println("Preset Duration");
            autoMode = true;
            intervalText.setVisible(true);
            secondsText.setVisible(true);
            intervalTextField.setVisible(true);
            intervalText.getParent().revalidate();
            intervalText.getParent().repaint();
            autoSlideChange();
        }
    }

    private void autoSlideChange() {
        System.out.println("Preset Duration");
    }

    private void manualSlideChange() {
        System.out.println("Manual Duration");
    }

    // --- AUTO-GENERATED UI CODE BELOW ---
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
        settings = new javax.swing.JPanel();
        playbackModeLabel = new javax.swing.JLabel();
        playbackModeBox = new javax.swing.JComboBox<>();
        secondsText = new javax.swing.JLabel();
        intervalTextField = new javax.swing.JTextField();
        intervalText = new javax.swing.JLabel();
        transitionComboBox = new javax.swing.JComboBox<>();
        transitionLabel = new javax.swing.JLabel();
        modeComboBox = new javax.swing.JComboBox<>();
        modeSelectionLabel = new javax.swing.JLabel();
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
        audioMenu = new javax.swing.JMenu();
        addAudioFileMenuItem = new javax.swing.JMenuItem();
        playAudioMenuItem = new javax.swing.JMenuItem();

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
        transitionTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transitionTestActionPerformed(evt);
            }
        });

        transitionBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "No Transition", "Cross Fade", "Wipe Up", "Wipe Right", "Wipe Down", "Wipe Left" }));
        transitionBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transitionBoxActionPerformed(evt);
            }
        });

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

        javax.swing.GroupLayout musicHolderLayout = new javax.swing.GroupLayout(musicHolder);
        musicHolder.setLayout(musicHolderLayout);
        musicHolderLayout.setHorizontalGroup(
            musicHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 499, Short.MAX_VALUE)
        );
        musicHolderLayout.setVerticalGroup(
            musicHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 470, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Music", musicHolder);

        playbackModeLabel.setText("Playback Mode:");

        playbackModeBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Loop Slideshow", "Play Once and End" }));
        playbackModeBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playbackModeBoxActionPerformed(evt);
            }
        });

        secondsText.setText("Seconds");

        intervalTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intervalTextFieldActionPerformed(evt);
            }
        });

        intervalText.setText("Slide interval");

        transitionComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Wipe left", "Wipe right", "Wipe up", "Wipe down", "Crossfade" }));

        transitionLabel.setText("Select transition for your Slideshow");

        modeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Manual Duration", "Preset Duration" }));

        modeSelectionLabel.setText("Select Automatic or Manual Slide Show");

        javax.swing.GroupLayout settingsLayout = new javax.swing.GroupLayout(settings);
        settings.setLayout(settingsLayout);
        settingsLayout.setHorizontalGroup(
            settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(settingsLayout.createSequentialGroup()
                        .addGroup(settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(modeSelectionLabel)
                            .addComponent(modeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(playbackModeLabel)
                            .addComponent(playbackModeBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(intervalText)
                            .addGroup(settingsLayout.createSequentialGroup()
                                .addComponent(intervalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(secondsText)))
                        .addContainerGap(286, Short.MAX_VALUE))
                    .addGroup(settingsLayout.createSequentialGroup()
                        .addGroup(settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(transitionLabel)
                            .addComponent(transitionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        settingsLayout.setVerticalGroup(
            settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modeSelectionLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(modeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(intervalText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(intervalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(secondsText))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(transitionLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(transitionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playbackModeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playbackModeBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(270, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Settings", settings);

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

        audioMenu.setText("Audio");

        addAudioFileMenuItem.setText("Add Audio File");
        addAudioFileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAudioFileMenuItemActionPerformed(evt);
            }
        });
        audioMenu.add(addAudioFileMenuItem);

        playAudioMenuItem.setText("Play Audio");
        playAudioMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playAudioMenuItemActionPerformed(evt);
            }
        });
        audioMenu.add(playAudioMenuItem);

        menuBar.add(audioMenu);

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
                    .addComponent(TimelinePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1027, Short.MAX_VALUE))
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

    // Launches presenter application.
    private void presenterButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (imageFiles != null && !imageFiles.isEmpty()) {
            File[] imageArray = imageFiles.toArray(new File[0]);
            new SlideshowPresenter(imageArray, 3000, true).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "No images to present.");
        }
    }

    private void LightModeActionPerformed(java.awt.event.ActionEvent evt) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                SwingUtilities.updateComponentTreeUI(this);
                prefs.put("theme", "light");
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(SlideshowCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private void DarkModeActionPerformed(java.awt.event.ActionEvent evt) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                SwingUtilities.updateComponentTreeUI(this);
                prefs.put("theme", "dark");
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(SlideshowCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        if (currentSlideshowName == null) {
            JOptionPane.showMessageDialog(this, "Please add images to create a slideshow first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File slideshowDir = SlideShowFileManager.getSlideshowDirectory(currentSlideshowName);
        File file = new File(slideshowDir, currentSlideshowName + ".json");
        saveSlideshowSettings(file);
    }

    private void openPreviousSlideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Files", "json");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            currentSlideshowFile = file;
            loadSlideshowSettings(file);
        }
    }

    private void createNewSlideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fileChooser = createFileChooser(JFileChooser.FILES_ONLY, true);
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            processSelectedFiles(selectedFiles);
        } else {
            System.out.println("No image selected.");
        }
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    private void addAudioFileMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
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
    }

    private void playAudioMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        if (audioFiles != null && !audioFiles.isEmpty()) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (File audioFile : audioFiles) {
                        try {
                            System.out.println("Playing file: " + audioFile.getAbsolutePath());
                            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                            Clip clip = AudioSystem.getClip();
                            clip.open(audioStream);
                            final Object lock = new Object();
                            clip.addLineListener(event -> {
                                if (event.getType() == LineEvent.Type.STOP) {
                                    synchronized (lock) {
                                        lock.notify();
                                    }
                                }
                            });
                            clip.start();
                            synchronized (lock) {
                                lock.wait();
                            }
                            clip.close();
                            audioStream.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            System.out.println("Error playing file: " + audioFile.getAbsolutePath());
                        }
                    }
                }
            });
            thread.start();
        } else {
            System.out.println("No audio files available.");
        }
    }

    private void playbackModeBoxActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void intervalTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    public void addAudioFile(File audioFile) {
        if (audioFile != null && audioFile.getName().toLowerCase().endsWith(".wav")) {
            audioFiles.add(audioFile);
            if (audioFiles.size() == 1) {
                audioIndex = 0;
            }
        } else {
            System.out.println("Invalid file format. Only .wav files are supported.");
        }
    }

    private JFileChooser createFileChooser(int selectionMode, boolean multiSelection) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(selectionMode);
        fileChooser.setMultiSelectionEnabled(multiSelection);
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
                "Image files (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(imageFilter);
        fileChooser.setFileView(createFileView(fileChooser));
        return fileChooser;
    }

    private FileView createFileView(final JFileChooser chooser) {
        return new FileView() {
            private final Map<File, Icon> thumbnailCache = new HashMap<>();
            private final Icon placeholderIcon = new ImageIcon(new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB));

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
                            .size(50, 50)
                            .asBufferedImage();
                    return new ImageIcon(thumbnail);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
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
        imageFiles = imageList;
        updateImage();
        timelinePanelObject.setImages(imageList);
        if (!imageList.isEmpty()) {
            timelinePanelObject.getImageList().setSelectedIndex(0);
            timelinePanelObject.getImageList().ensureIndexIsVisible(0);
        }
        // Grow transitions list to match images.
        while (imageTransitions.size() < imageList.size()) {
            imageTransitions.add(TransitionType.INSTANT);
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
            File slideshowDir = SlideShowFileManager.getSlideshowDirectory(currentSlideshowName);
            if (slideshowDir.exists() && slideshowDir.isDirectory() && slideshowDir.list().length > 0) {
                int choice = JOptionPane.showConfirmDialog(this, "Slideshow '" + currentSlideshowName + "' already exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.NO_OPTION) {
                    currentSlideshowName = null;
                    processSelectedFiles(selectedFiles);
                    return;
                } else {
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
                    publish(targetFile);
                }
                return null;
            }

            @Override
            protected void process(List<File> chunks) {
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
                modeComboBox.setVisible(true);
                modeSelectionLabel.setVisible(true);
                transitionComboBox.setVisible(true);
                transitionLabel.setVisible(true);
            }
        }.execute();
    }

    private void clearExistingImages() {
        imageFiles.clear();
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

    private void transitionBoxActionPerformed(java.awt.event.ActionEvent evt) {
        int selectionIndex = transitionBox.getSelectedIndex();
        TransitionType selectedTransition = TransitionType.values()[selectionIndex];

        int currentIndex = timelinePanelObject.getImageList().getSelectedIndex();
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        while (imageTransitions.size() <= currentIndex) {
            imageTransitions.add(TransitionType.INSTANT);
        }
        imageTransitions.set(currentIndex, selectedTransition);
        System.out.println("Image " + currentIndex + " transition set to " + selectedTransition);
        transitionBox.hidePopup();
    }

    private void transitionTestActionPerformed(java.awt.event.ActionEvent evt) {
        ImageIcon labelIcon = (ImageIcon) imageLabel.getIcon();
        if (labelIcon == null) {
            System.out.println("No image displayed for transition.");
            return;
        }
        BufferedImage nextImage = Transition.toBufferedImage(labelIcon.getImage());
        int currentIndex = timelinePanelObject.getImageList().getSelectedIndex();
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        int size = imageFiles.size();
        BufferedImage prevImage = nextImage;
        if (size >= 2) {
            int prevIndex = (currentIndex - 1 >= 0) ? currentIndex - 1 : size - 1;
            File prevFile = imageFiles.get(prevIndex);
            ImageIcon prevIcon = new ImageIcon(prevFile.getAbsolutePath());
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
        // For testing, use the transition from the correct combo box.
        String transitionStr = (String) transitionBox.getSelectedItem();  // Changed from transitionComboBox to transitionBox
        TransitionType transitionType;
        try {
            transitionType = TransitionType.valueOf(transitionStr.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid transition type selected: " + transitionStr);
            return;
        }
        transitionManager.doTransition(prevImage, nextImage, imageLabel, transitionType);
    }
    
    public static void main(String args[]) {
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
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SlideshowCreator().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify                     
    private javax.swing.JMenuItem DarkMode;
    private javax.swing.JMenuItem LightMode;
    private javax.swing.JMenu ThemesButton;
    private javax.swing.JPanel TimelinePanel;
    private javax.swing.JMenuItem addAudioFileMenuItem;
    private javax.swing.JMenu audioMenu;
    private javax.swing.JMenuItem createNewSlideMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JPanel fileExplorerHolder;
    private javax.swing.JPanel imageContainer;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JLabel intervalText;
    private javax.swing.JTextField intervalTextField;
    private javax.swing.JMenu jMenu;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel largeFileViewHolder;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JComboBox<String> modeComboBox;
    private javax.swing.JLabel modeSelectionLabel;
    private javax.swing.JPanel musicHolder;
    private javax.swing.JMenuItem openPreviousSlideMenuItem;
    private javax.swing.JMenuItem playAudioMenuItem;
    private javax.swing.JComboBox<String> playbackModeBox;
    private javax.swing.JLabel playbackModeLabel;
    private javax.swing.JButton presenterButton;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JLabel secondsText;
    private javax.swing.JPanel settings;
    private javax.swing.JPanel spacerPanel;
    private javax.swing.JComboBox<String> transitionBox;
    private javax.swing.JComboBox<String> transitionComboBox;
    private javax.swing.JLabel transitionLabel;
    private javax.swing.JButton transitionTest;
    private javax.swing.JPanel transitionsHolder;
    // End of variables declaration                   
}
