/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.team5.senior_project;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileView;
import java.util.prefs.Preferences;
import java.awt.BorderLayout;
import javax.swing.JOptionPane;

/**
 *
 * @author Team 5
 */
public class SlideshowCreator extends javax.swing.JFrame {
    
    // Global variables needed for keeping image index and storing the image list
    private java.io.File[] imageFiles; // image list
    private final int[] index = {0}; // image list index
    private static final Preferences prefs = Preferences.userNodeForPackage(SlideshowCreator.class);
    private TimelinePanel timelinePanel;

    private File workingFile = null; // stores location of most recently saved slideshow file (file currently being worked on)
    
    /**
     * Creates new form SlideshowCreator
     */
    public SlideshowCreator() {
        initComponents();
        applySavedTheme(); // Apply saved theme when starting

        
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

        
        if (this.TimelinePanel == null) {
            this.TimelinePanel = new javax.swing.JPanel();
        }
        this.TimelinePanel.removeAll();
        this.TimelinePanel.setLayout(new BorderLayout());
        timelinePanel = new TimelinePanel();
        this.TimelinePanel.add(timelinePanel, BorderLayout.CENTER);
        this.TimelinePanel.revalidate();
        this.TimelinePanel.repaint();

        // Set the timeline change listener so that any reordering refreshes the main image display.
        timelinePanel.setTimelineChangeListener(() -> {
            updateImage();
        });
        
        timelinePanel.getImageList().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                File selectedFile = timelinePanel.getImageList().getSelectedValue();
                if (selectedFile != null && imageFiles != null) {
                    // Update the index based on the selected file
                    for (int i = 0; i < imageFiles.length; i++) {
                        if (imageFiles[i].equals(selectedFile)) {
                            index[0] = i;
                            break;
                        }
                    }
                    updateImage();
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
    
    // Creates global SlideShowImages folder where the program stores user images
    public class SlideShowFileManager {
        private static final File imagesFolder = new File(System.getProperty("user.dir"), "SlideShowImages");
        private static final File savedSlidesFolder = new File(System.getProperty("user.dir"), "SavedSlideShows");

        static {
            if (!imagesFolder.exists()) {
                if (imagesFolder.mkdir()) {
                    System.out.println("SlideShowImages folder created at: " + imagesFolder.getAbsolutePath());
                } else {
                    System.err.println("Failed to create SlideShowImages folder.");
                }
            }
            if (!savedSlidesFolder.exists()) {
                if (savedSlidesFolder.mkdir()) {
                    System.out.println("SlideShowImages folder created at: " + savedSlidesFolder.getAbsolutePath());
                } else {
                    System.err.println("Failed to create SavedSlideShows folder.");
                }
            }
        }

        public static File getImagesFolder() {
            return imagesFolder;
        }
        
        public static File getSavedSlidesFolder() {
            return savedSlidesFolder;
        }
    }
       
    /* 
    Updates JLabel with the image matching the current index
    Likely called due to index value changing
    This is where any image modifications should likely go
    */
    private void updateImage() {
        // Rebuild the imageFiles array from the current timeline ordering.
        if (timelinePanel != null) {
            List<File> orderedImages = timelinePanel.getImages();
            if (!orderedImages.isEmpty()) {
                imageFiles = orderedImages.toArray(new File[0]);
            }
        }

        if (imageFiles != null && imageFiles.length > 0) {
            // Ensure the current index is valid.
            if (index[0] < 0 || index[0] >= imageFiles.length) {
                index[0] = 0;
            }

            // Get the dimensions of the image label.
            int labelWidth = imageLabel.getWidth();
            int labelHeight = imageLabel.getHeight();

            // Fallback to preferred size if necessary.
            if (labelWidth <= 0 || labelHeight <= 0) {
                labelWidth = imageLabel.getPreferredSize().width;
                labelHeight = imageLabel.getPreferredSize().height;
            }

            // Load the image.
            ImageIcon originalIcon = new ImageIcon(imageFiles[index[0]].getAbsolutePath());
            Image originalImage = originalIcon.getImage();

            // Calculate the scaling ratio to fit the image within the label while preserving aspect ratio.
            double widthRatio = (double) labelWidth / originalImage.getWidth(null);
            double heightRatio = (double) labelHeight / originalImage.getHeight(null);
            double scaleRatio = Math.min(widthRatio, heightRatio);

            // Calculate the new dimensions.
            int newWidth = (int) (originalImage.getWidth(null) * scaleRatio);
            int newHeight = (int) (originalImage.getHeight(null) * scaleRatio);

            // Scale the image.
            Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            ImageIcon resizedIcon = new ImageIcon(resizedImage);

            // Set the scaled image as the icon.
            imageLabel.setIcon(resizedIcon);

            // Center the image in the label.
            imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        }
    }

    // Writes the current information for the slides to the .ssx in saveMenuItemActionPerformed class
    private void saveSlideshow(File saveFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile))) {
            for (File imageFile : imageFiles) { // add all user modification settings here so that it's saved
                writer.write(imageFile.getAbsolutePath());
                writer.newLine();
            }
            System.out.println("Slideshow saved to: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving slideshow: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Loads built slideshow into the SlideShowCreator JLabel when a file is selected
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

                // Update the timeline panel with loaded images
                timelinePanel.setImages(loadedImages);
                timelinePanel.revalidate();
                timelinePanel.repaint();

                updateImage(); // Ensure first image is displayed
                System.out.println("Slideshow loaded successfully.");

                // Auto-select the first image in the timeline
                timelinePanel.getImageList().setSelectedIndex(0);
                timelinePanel.getImageList().ensureIndexIsVisible(0);
            } else {
                System.err.println("No valid images found in the slideshow file.");
            }

        } catch (IOException e) {
            System.err.println("Error loading slideshow: " + e.getMessage());
            e.printStackTrace();
        }
        workingFile = loadFile;
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
        jMenu3 = new javax.swing.JMenu();
        selectFolderMenuItem = new javax.swing.JMenuItem();
        addImageMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        openFileMenuItem = new javax.swing.JMenuItem();
        ThemesButton = new javax.swing.JMenu();
        LightMode = new javax.swing.JMenuItem();
        DarkMode = new javax.swing.JMenuItem();

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

        jMenu3.setText("File");

        selectFolderMenuItem.setText("Select Folder");
        selectFolderMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectFolderMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(selectFolderMenuItem);

        addImageMenuItem.setText("Add Image");
        addImageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addImageMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(addImageMenuItem);

        saveAsMenuItem.setText("Save As");
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(saveAsMenuItem);

        saveMenuItem.setText("Save");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(saveMenuItem);

        openFileMenuItem.setText("Open File");
        openFileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(openFileMenuItem);

        menuBar.add(jMenu3);

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
                    .addComponent(TimelinePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 528, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(TimelinePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    // Launches presenter application
    private void presenterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_presenterButtonActionPerformed
        new SlideshowPresenter().setVisible(true);
    }//GEN-LAST:event_presenterButtonActionPerformed

   
    // Selects folder of images to add to our image folder and sequentially to the image index for display
    private void selectFolderMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectFolderMenuItemActionPerformed
        // Create a JFileChooser instance
        JFileChooser folderChooser = new JFileChooser();

           // Set it to select files & directories
        folderChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        
        // Filter only image files
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
        "Image files (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif");
        folderChooser.setFileFilter(imageFilter);
        
        // Custom FileView for image previews to show thumbnails
        folderChooser.setFileView(new FileView() {
        @Override
        // Check if the file is an image and return a thumbnail icon
        public Icon getIcon(File f) {
            if (f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") ||
                               f.getName().toLowerCase().endsWith(".jpeg") ||
                               f.getName().toLowerCase().endsWith(".png") ||
                               f.getName().toLowerCase().endsWith(".gif"))) {
                return getThumbnailIcon(f);   // Get thumbnail for images
            }
            return super.getIcon(f);
        }
        
        private Icon getThumbnailIcon(File file) {
            try {
                // Generate thumbnail by resizing the image to 50x50 pixels
                ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                Image image = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH); // Resize the image
                return new ImageIcon(image);
            } catch (Exception e) {
                return null;  // Return null if there is an issue generating the thumbnail
            }
        }
    });

        // Open the dialog and get the result
        int returnValue = folderChooser.showOpenDialog(this);

        // Check if the user selected a folder
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = folderChooser.getSelectedFile();
            System.out.println("Selected Folder: " + selectedFolder.getAbsolutePath());
            
        // If a file is selected instead of a folder, get its parent directory
        if (selectedFolder.isFile()) {
            selectedFolder = selectedFolder.getParentFile();
        }
            System.out.println("Selected Folder: " + selectedFolder.getAbsolutePath());


            // Get the list of files in the selected directory
            File[] files = selectedFolder.listFiles();

            if (files != null) {
                System.out.println("Files detected in selected folder:");

                // Get the existing images if the array is already initialized
                List<File> imageList = (imageFiles != null) ? new ArrayList<>(Arrays.asList(imageFiles)) : new ArrayList<>();
                Set<String> existingImageNames = new HashSet<>();

                // Store existing image names to prevent duplicates
                for (File img : imageList) {
                    existingImageNames.add(img.getName().toLowerCase());
                }

                for (File file : files) {
                    System.out.println(file.getAbsolutePath());

                    // Check if the file is an image (case-insensitive extension check)
                    String fileName = file.getName().toLowerCase();
                    if (file.isFile() && (fileName.endsWith(".jpg") || 
                                          fileName.endsWith(".png") || 
                                          fileName.endsWith(".jpeg") || 
                                          fileName.endsWith(".gif"))) {
                        if (!existingImageNames.contains(file.getName().toLowerCase())) {
                            try {
                                // Copy the file to the SlideShowImages folder
                                Path source = file.toPath();
                                Path target = SlideShowFileManager.getImagesFolder().toPath().resolve(file.getName());
                                System.out.println("Copying file: " + source.toString() + " to " + target.toString());
                                Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                System.out.println("Copied: " + file.getName());

                                // Add new image to the list
                                imageList.add(target.toFile());
                            } catch (IOException ex) {
                                System.err.println("Error copying file: " + file.getAbsolutePath());
                            }
                        } else {
                            System.out.println("Skipped duplicate image: " + file.getName());
                        }
                    } else {
                        System.out.println("Skipped non-image file: " + file.getName());
                    }
                }

                // Convert list back to array
                imageFiles = imageList.toArray(new File[0]);

                // Display the first image if available
                if (imageFiles.length > 0) {
                    updateImage();
                    
                    timelinePanel.setImages(Arrays.asList(imageFiles));
                    timelinePanel.revalidate();
                    timelinePanel.repaint();

                }

                System.out.println("Image files copied successfully to: " + SlideShowFileManager.getImagesFolder().getAbsolutePath());
            } else {
                System.out.println("The selected folder is empty or an error occurred.");
            }
        } else {
            System.out.println("No folder selected.");
        }

        // Print working directory to confirm location
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
    }//GEN-LAST:event_selectFolderMenuItemActionPerformed

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

    // Selects image to add to our image folder and adds it sequentially to the image index for display
    private void addImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImageMenuItemActionPerformed
       JFileChooser fileChooser = new JFileChooser();
       fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
       fileChooser.setMultiSelectionEnabled(true);
       FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
               "Image files (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif");
       fileChooser.setFileFilter(imageFilter);

       // Custom FileView for image previews
       fileChooser.setFileView(new FileView() {
           @Override
           public Icon getIcon(File f) {
               if (f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") ||
                                    f.getName().toLowerCase().endsWith(".jpeg") ||
                                    f.getName().toLowerCase().endsWith(".png") ||
                                    f.getName().toLowerCase().endsWith(".gif"))) {
                   return getThumbnailIcon(f);
               }
               return super.getIcon(f);
           }

           private Icon getThumbnailIcon(File file) {
               try {
                   ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                   Image image = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                   return new ImageIcon(image);
               } catch (Exception e) {
                   return null;
               }
           }
       });

       int returnValue = fileChooser.showOpenDialog(this);
       if (returnValue == JFileChooser.APPROVE_OPTION) {
           File[] selectedFiles = fileChooser.getSelectedFiles();
           System.out.println("Selected Images:");

           List<File> newImages = new ArrayList<>();
           if (imageFiles != null) {
               newImages.addAll(Arrays.asList(imageFiles));
           }

           // Define the target folder
           File targetFolder = SlideShowFileManager.getImagesFolder();
           for (File selectedFile : selectedFiles) {
               System.out.println("Selected Image: " + selectedFile.getAbsolutePath());
               File targetFile = new File(targetFolder, selectedFile.getName());

               // Avoid duplicate filenames
               int counter = 1;
               while (targetFile.exists()) {
                   int dotIndex = selectedFile.getName().lastIndexOf('.');
                   String nameWithoutExt = (dotIndex > 0) ? selectedFile.getName().substring(0, dotIndex) : selectedFile.getName();
                   String ext = (dotIndex > 0) ? selectedFile.getName().substring(dotIndex) : "";
                   targetFile = new File(targetFolder, nameWithoutExt + " (" + counter + ")" + ext);
                   counter++;
               }

               try {
                   Files.copy(selectedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                   System.out.println("Copied image to: " + targetFile.getAbsolutePath());
                   newImages.add(targetFile);
               } catch (IOException ex) {
                   System.err.println("Error copying image: " + selectedFile.getAbsolutePath());
               }
           }

           // Update the global array and timeline panel once.
           imageFiles = newImages.toArray(new File[0]);
           updateImage();
           timelinePanel.setImages(newImages);
           // Auto-select and highlight the first image.
           if (newImages.size() > 0) {
               timelinePanel.getImageList().setSelectedIndex(0);
               timelinePanel.getImageList().ensureIndexIsVisible(0);
           }
           timelinePanel.revalidate();
           timelinePanel.repaint();
       } else {
           System.out.println("No image selected.");
       }
    }//GEN-LAST:event_addImageMenuItemActionPerformed

    // Allows user to save currently created slideshow
    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Slideshow");
        fileChooser.setCurrentDirectory(SlideShowFileManager.getSavedSlidesFolder()); // Sets directory to created folder for saved slideshows
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Slideshow Files (*.ssx)", "ssx"));
    
        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".ssx")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".ssx");
            }
            
            if (fileToSave.exists()) { // Confirmation message for if user wants to overwrite an existing file
                int overwriteChoice = JOptionPane.showConfirmDialog(
                        null,
                        "A file with this name already exists. Do you want to overwrite it?",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (overwriteChoice != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            workingFile = fileToSave;
            saveSlideshow(fileToSave);
        }
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    // Overwrites the currently working file as long as it exists in the folder already, allowing easy updates
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        if (workingFile != null) {
            saveSlideshow(workingFile);
        } else {
            JOptionPane.showMessageDialog(null, "No previously saved slideshow. Use 'Save As' first.");
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    // Allows user to continue editing a previously made file by loading it in
    private void openFileMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Slideshow");
        fileChooser.setCurrentDirectory(SlideShowFileManager.getSavedSlidesFolder());
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Slideshow Files (*.ssx)", "ssx"));

        int userSelection = fileChooser.showOpenDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();
            loadSlideshow(fileToLoad);
        }
    }//GEN-LAST:event_openFileMenuItemActionPerformed
 
    /**
     * @param args the command line arguments
     */    
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
    private javax.swing.JMenuItem addImageMenuItem;
    private javax.swing.JPanel fileExplorerHolder;
    private javax.swing.JPanel imageContainer;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel largeFileViewHolder;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JPanel musicHolder;
    private javax.swing.JMenuItem openFileMenuItem;
    private javax.swing.JButton presenterButton;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem selectFolderMenuItem;
    private javax.swing.JPanel transitionsHolder;
    // End of variables declaration//GEN-END:variables
}
