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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileView;
import net.coobird.thumbnailator.Thumbnails;
import java.awt.image.BufferedImage;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

/**
 *
 * @author Team 5
 */
public class SlideshowCreator extends javax.swing.JFrame {
    
    private List<File> imageFiles = new ArrayList<>(); // image list
    private List<File> audioFiles = new ArrayList<>(); // audio list
    private int index = 0; // image list index (now defunct and can likely be removed)
    private int audioIndex = 0; // tracks current audio track
    private Preferences prefs = Preferences.userNodeForPackage(SlideshowCreator.class);
    private List<Slide> slides = new ArrayList<>();
    private SlideShowFileManager slideShowFileManager = new SlideShowFileManager();
    private String audioPath = null;
    private boolean loop = true;
    private File currentSlideshowFile = null;
    private TimelinePanel timelinePanelObject; // renamed timeline panel reference
    private String currentSlideshowName = null; // Class-level variable

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
        
        fileExplorerPanel.getFileTree().addTreeSelectionListener(e -> {
            File selectedDir = fileExplorerPanel.getSelectedDirectory();
            if (selectedDir != null) {
                largeFileViewPanel.updateFolder(selectedDir);
            }
        });
        
        // Initialize the timeline panel
        timelinePanelObject = new TimelinePanel(); // Initialize timeline panel
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
                File selectedFile = timelinePanelObject.getImageList().getSelectedValue();
                if (selectedFile != null) {
                    updateImage(selectedFile);
                }
            }
        });
    }
    
    // Want to move into own file later
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
    
    private void saveSlideshowSettings(File file) {
        String filePath = file.getAbsolutePath();
        String slideshowName = file.getName().replaceFirst("[.][^.]+$", ""); // Extract name without extension
        List<Slide> slides = getSlides();
        boolean loop = isLoop();

        SlideshowSettingsSaver.saveSettingsToJson(filePath, slideshowName, slides, audioFiles, loop);
    }
       
    private void loadSlideshowSettings(File file) {
        try {
            currentSlideshowName = file.getParentFile().getName();
            File slideshowDir = file.getParentFile();

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
            JSONArray slides = json.getJSONArray("slides"); // Corrected line: Use "slides"
            if (json.has("audio")) {
                JSONArray audioArray = json.getJSONArray("audio");
                for (int i = 0; i < audioArray.length(); i++) {
                    audioFiles.add(new File(audioArray.getString(i)));
                }
            }

            for (int i = 0; i < slides.length(); i++) {
                JSONObject slideObject = slides.getJSONObject(i);
                String imagePath = slideObject.getString("image"); // Corrected line: Use "image"
                File imageFile = new File(imagePath);
                imageFiles.add(imageFile);
            }
            updateImageFiles(imageFiles);
            
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading slideshow settings.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Invalid JSON format in slideshow settings.", "Error", JOptionPane.ERROR_MESSAGE);
            System.out.println("Error parsing JSON: " + e.getMessage());
        }
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
        List<File> imageFiles = timelinePanelObject.getImages(); // Get the ordered files from the timeline panel.
        for (File imageFile: imageFiles){
            slides.add(new Slide(imageFile.getAbsolutePath(), 5,"fade", 0)); // Example slide.
        }
        return slides;
    }
    
    private String getAudioPath() {
        // Implement this method to get the audio path
        return null; // Example
    }
    
    private boolean isLoop() {
        // Implement this method to get the loop setting
        return true; // Example
    }
    
    private void updateTimeline() {
        // Update your timeline component with the slides in the 'slides' list.
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Slide slide : slides) {
            listModel.addElement(slide.getImagePath());
        }
        // Assuming your timeline is a JList named 'timelineList':
        // timelineList.setModel(listModel);
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
        imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
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
        musicHolder = new javax.swing.JPanel();
        imageContainer = new javax.swing.JPanel();
        presenterButton = new javax.swing.JButton();
        imageLabel = new javax.swing.JLabel();
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

        jSplitPane1.setDividerLocation(500);

        javax.swing.GroupLayout fileExplorerHolderLayout = new javax.swing.GroupLayout(fileExplorerHolder);
        fileExplorerHolder.setLayout(fileExplorerHolderLayout);
        fileExplorerHolderLayout.setHorizontalGroup(
            fileExplorerHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        fileExplorerHolderLayout.setVerticalGroup(
            fileExplorerHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 493, Short.MAX_VALUE)
        );

        jSplitPane2.setLeftComponent(fileExplorerHolder);

        javax.swing.GroupLayout largeFileViewHolderLayout = new javax.swing.GroupLayout(largeFileViewHolder);
        largeFileViewHolder.setLayout(largeFileViewHolderLayout);
        largeFileViewHolderLayout.setHorizontalGroup(
            largeFileViewHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 495, Short.MAX_VALUE)
        );
        largeFileViewHolderLayout.setVerticalGroup(
            largeFileViewHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 493, Short.MAX_VALUE)
        );

        jSplitPane2.setRightComponent(largeFileViewHolder);

        jTabbedPane1.addTab("Files", jSplitPane2);

        javax.swing.GroupLayout transitionsHolderLayout = new javax.swing.GroupLayout(transitionsHolder);
        transitionsHolder.setLayout(transitionsHolderLayout);
        transitionsHolderLayout.setHorizontalGroup(
            transitionsHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 500, Short.MAX_VALUE)
        );
        transitionsHolderLayout.setVerticalGroup(
            transitionsHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 493, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Transitions", transitionsHolder);

        javax.swing.GroupLayout musicHolderLayout = new javax.swing.GroupLayout(musicHolder);
        musicHolder.setLayout(musicHolderLayout);
        musicHolderLayout.setHorizontalGroup(
            musicHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 500, Short.MAX_VALUE)
        );
        musicHolderLayout.setVerticalGroup(
            musicHolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 493, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Music", musicHolder);

        jSplitPane1.setLeftComponent(jTabbedPane1);
        jTabbedPane1.getAccessibleContext().setAccessibleName("Files");

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
            .addGroup(imageContainerLayout.createSequentialGroup()
                .addGap(112, 112, 112)
                .addGroup(imageContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, imageContainerLayout.createSequentialGroup()
                        .addGap(0, 293, Short.MAX_VALUE)
                        .addComponent(presenterButton)))
                .addContainerGap())
        );
        imageContainerLayout.setVerticalGroup(
            imageContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, imageContainerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(presenterButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 487, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(imageContainer);

        javax.swing.GroupLayout TimelinePanelLayout = new javax.swing.GroupLayout(TimelinePanel);
        TimelinePanel.setLayout(TimelinePanelLayout);
        TimelinePanelLayout.setHorizontalGroup(
            TimelinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        TimelinePanelLayout.setVerticalGroup(
            TimelinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 134, Short.MAX_VALUE)
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
                    .addComponent(TimelinePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 528, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(TimelinePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(35, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Launches presenter application
    private void presenterButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                
        if (imageFiles != null && !imageFiles.isEmpty()) {
            File[] imageArray = imageFiles.toArray(new File[0]);
            new SlideshowPresenter(imageArray, 3000, true, false).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "No images to present.");
        }
    }                                               

   
    // Sets UI design to FlatLightLaf (light mode version of Flat Laf)
    private void LightModeActionPerformed(java.awt.event.ActionEvent evt) {                                          
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                SwingUtilities.updateComponentTreeUI(this);
                prefs.put("theme", "light"); // Save preference
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(SlideshowCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }                                         

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
    
    // Allows user to save currently created slideshow
    private void openPreviousSlideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openPreviousSlideMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Files", "json");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            currentSlideshowFile = file;
            loadSlideshowSettings(file);
        }
    }//GEN-LAST:event_openPreviousSlideMenuItemActionPerformed
        
    
    // Overwrites the currently working file as long as it exists in the folder already, allowing easy updates
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        if (currentSlideshowName == null) {
            JOptionPane.showMessageDialog(this, "Please add images to create a slideshow first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File slideshowDir = SlideShowFileManager.getSlideshowDirectory(currentSlideshowName); // Get the main slideshow directory
        File file = new File(slideshowDir, currentSlideshowName + ".json"); // Save the JSON file in the main directory
        saveSlideshowSettings(file);
    }//GEN-LAST:event_saveMenuItemActionPerformed
    

    // Allows user to save currently created slideshow
    private void createNewSlideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createNewSlideMenuItemActionPerformed
        JFileChooser fileChooser = createFileChooser(JFileChooser.FILES_ONLY, true);
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            processSelectedFiles(selectedFiles);
        } else {
            System.out.println("No image selected.");
        }
    }//GEN-LAST:event_createNewSlideMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.exit(0); // Terminate the application
    }//GEN-LAST:event_exitMenuItemActionPerformed
    
    private void addAudioFileMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAudioFileMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("WAV Audio Files", "wav"));

        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            for (File file : fileChooser.getSelectedFiles()) {
                addAudioFile(file);
            }
        }
    }//GEN-LAST:event_addAudioFileMenuItemActionPerformed

    private void playAudioMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playAudioMenuItemActionPerformed
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
    }//GEN-LAST:event_playAudioMenuItemActionPerformed

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
    
    private JFileChooser createFileChooser(int selectionMode, boolean multiSelection) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(selectionMode);
        fileChooser.setMultiSelectionEnabled(multiSelection);
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Image files (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(imageFilter);
        fileChooser.setFileView(createFileView());
        return fileChooser;
    }
    
    private FileView createFileView() {
        return new FileView() {
            @Override
            public Icon getIcon(File f) {
                if (f.isFile() && isImageFile(f)) {
                    return getThumbnailIcon(f);
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
                    return;
                } else {
                    // User chose yes, clear existing images
                    clearExistingImages();
                }
            }
        }
                
        List<File> newImages = new ArrayList<>();
        File targetFolder = SlideShowFileManager.getImagesFolder(currentSlideshowName);

        for (File selectedFile : selectedFiles) {
            File targetFile = new File(targetFolder, selectedFile.getName());
            targetFile = avoidDuplicateFileNames(targetFile, selectedFile, targetFolder);
            copyImageFile(selectedFile, newImages, targetFile);
        }
        updateImageFiles(newImages);
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
    
    private void copyImageFile(File file, List<File> imageList) {
        String slideshowName = JOptionPane.showInputDialog(this, "Enter Slideshow Name:", "New Slideshow", JOptionPane.PLAIN_MESSAGE);
        if (slideshowName == null || slideshowName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Slideshow name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File targetFolder = SlideShowFileManager.getImagesFolder(slideshowName);
        File targetFile = new File(targetFolder, file.getName());

        copyImageFile(file, imageList, targetFile);
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
    private javax.swing.JMenuItem addAudioFileMenuItem;
    private javax.swing.JMenu audioMenu;
    private javax.swing.JMenuItem createNewSlideMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JPanel fileExplorerHolder;
    private javax.swing.JPanel imageContainer;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JMenu jMenu;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel largeFileViewHolder;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JPanel musicHolder;
    private javax.swing.JMenuItem openPreviousSlideMenuItem;
    private javax.swing.JButton presenterButton;
    private javax.swing.JMenuItem playAudioMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JPanel transitionsHolder;
    // End of variables declaration//GEN-END:variables
}
