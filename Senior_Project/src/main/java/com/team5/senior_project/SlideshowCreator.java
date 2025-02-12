/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.team5.senior_project;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Image;
import java.io.BufferedWriter;
import java.io.File;
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

/**
 *
 * @author Team 5
 */
public class SlideshowCreator extends javax.swing.JFrame {
    
    // Global variables needed for keeping image index and storing the image list
    private java.io.File[] imageFiles; // image list
    private final int[] index = {0}; // image list index
    private static final Preferences prefs = Preferences.userNodeForPackage(SlideshowCreator.class);

    /**
     * Creates new form SlideshowCreator
     */
    public SlideshowCreator() {
        initComponents();
        applySavedTheme(); // Apply saved theme when starting
    }
    
    // Probably want to move the things into their own file later
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
    public class SlideShowManager {
        private static final File programFolder = new File(System.getProperty("user.dir"), "SlideShowImages");

        static {
            if (!programFolder.exists()) {
                if (programFolder.mkdir()) {
                    System.out.println("SlideShowImages folder created at: " + programFolder.getAbsolutePath());
                } else {
                    System.err.println("Failed to create SlideShowImages folder.");
                }
            }
        }

        public static File getProgramFolder() {
            return programFolder;
        }

        public static void main(String[] args) {
            System.out.println("Accessing SlideShowImages folder: " + SlideShowManager.getProgramFolder().getAbsolutePath());
        }
    }
    
    // Creates global SavedSlideShows folder where the program stores saved created slideshows
    public class SlideShowSaveFolder {
        private static final File saveFolder = new File(System.getProperty("user.dir"), "SavedSlideShows");

        static {
            if (!saveFolder.exists()) {
                if (saveFolder.mkdir()) {
                    System.out.println("SlideShowImages folder created at: " + saveFolder.getAbsolutePath());
                } else {
                    System.err.println("Failed to create SavedSlideShows folder.");
                }
            }
        }

        public static File getSaveFolder() {
            return saveFolder;
        }

        public static void main(String[] args) {
            System.out.println("Accessing SavedSlideShows folder: " + SlideShowManager.getProgramFolder().getAbsolutePath());
        }
    }
    
    /* 
    Updates JLabel with the image matching the current index
    Likely called due to index value changing
    This is where any image modifications should likely go
    */
    private void updateImage() {
        if (imageFiles != null && imageFiles.length > 0) {
            // Load the image from the file
            ImageIcon originalIcon = new ImageIcon(imageFiles[index[0]].getAbsolutePath());
            Image originalImage = originalIcon.getImage();

            // Get the width and height of the JLabel
            int labelWidth = imageLabel.getWidth();
            int labelHeight = imageLabel.getHeight();

            // Calculate the scaling ratio
            double widthRatio = (double) labelWidth / originalImage.getWidth(null);
            double heightRatio = (double) labelHeight / originalImage.getHeight(null);

            // Find the smaller ratio to preserve aspect ratio
            double scaleRatio = Math.min(widthRatio, heightRatio);

            // Calculate new dimensions while maintaining the aspect ratio
            int newWidth = (int) (originalImage.getWidth(null) * scaleRatio);
            int newHeight = (int) (originalImage.getHeight(null) * scaleRatio);

            // Scale the image to the new size
            Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

            // Set the resized image as the label icon
            imageLabel.setIcon(new ImageIcon(resizedImage));
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
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        presenterButton = new javax.swing.JButton();
        firstSlideButton = new javax.swing.JButton();
        imageLabel = new javax.swing.JLabel();
        nextSlideButton = new javax.swing.JButton();
        previousSlideButton = new javax.swing.JButton();
        lastSlideButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        selectFolderMenuItem = new javax.swing.JMenuItem();
        addImageMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        ThemesButton = new javax.swing.JMenu();
        LightMode = new javax.swing.JMenuItem();
        DarkMode = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Slideshow Creator");

        presenterButton.setText("Open Presenter");
        presenterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                presenterButtonActionPerformed(evt);
            }
        });

        firstSlideButton.setText("First");
        firstSlideButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firstSlideButtonActionPerformed(evt);
            }
        });

        nextSlideButton.setText("Next");
        nextSlideButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextSlideButtonActionPerformed(evt);
            }
        });

        previousSlideButton.setText("Previous");
        previousSlideButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousSlideButtonActionPerformed(evt);
            }
        });

        lastSlideButton.setText("Last");
        lastSlideButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lastSlideButtonActionPerformed(evt);
            }
        });

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

        saveMenuItem.setText("Save Slideshow");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(saveMenuItem);

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
                .addGap(143, 143, 143)
                .addComponent(firstSlideButton)
                .addGap(39, 39, 39)
                .addComponent(previousSlideButton)
                .addGap(44, 44, 44)
                .addComponent(nextSlideButton)
                .addGap(43, 43, 43)
                .addComponent(lastSlideButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 577, Short.MAX_VALUE)
                        .addComponent(presenterButton))
                    .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(presenterButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 353, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(firstSlideButton)
                    .addComponent(nextSlideButton)
                    .addComponent(previousSlideButton)
                    .addComponent(lastSlideButton))
                .addGap(16, 16, 16))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    // Launches presenter application
    private void presenterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_presenterButtonActionPerformed
        new SlideshowPresenter().setVisible(true);
    }//GEN-LAST:event_presenterButtonActionPerformed

    // Goes to the first image in the image list
    private void firstSlideButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firstSlideButtonActionPerformed
        index[0] = 0;
        updateImage();
    }//GEN-LAST:event_firstSlideButtonActionPerformed
    
    // Goes back one in the image list (back to previous image)
    private void previousSlideButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousSlideButtonActionPerformed
        index[0] = (index[0] - 1 + imageFiles.length) % imageFiles.length; // Cycle through images
        updateImage();
    }//GEN-LAST:event_previousSlideButtonActionPerformed

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
                                Path target = SlideShowManager.getProgramFolder().toPath().resolve(file.getName());
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
                }

                System.out.println("Image files copied successfully to: " + SlideShowManager.getProgramFolder().getAbsolutePath());
            } else {
                System.out.println("The selected folder is empty or an error occurred.");
            }
        } else {
            System.out.println("No folder selected.");
        }

        // Print working directory to confirm location
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
    }//GEN-LAST:event_selectFolderMenuItemActionPerformed

    // Goes to the next image in the list
    private void nextSlideButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextSlideButtonActionPerformed
        index[0] = (index[0] + 1) % imageFiles.length; // Cycle through images
        updateImage();
    }//GEN-LAST:event_nextSlideButtonActionPerformed

    // Goes to the last image in the list
    private void lastSlideButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lastSlideButtonActionPerformed
        index[0] = imageFiles.length - 1;
        updateImage();
    }//GEN-LAST:event_lastSlideButtonActionPerformed

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
        // Create a JFileChooser instance
        JFileChooser fileChooser = new JFileChooser();

        // Set file selection mode to only files
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
       
        // Enable multiple file selection
        fileChooser.setMultiSelectionEnabled(true);
        
        // Set a file filter to only allow image files
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
            "Image files (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(imageFilter);
        
        // Custom FileView for image previews
        fileChooser.setFileView(new FileView() 
        {
            @Override
            public Icon getIcon(File f) 
            {
                if (f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") ||
                               f.getName().toLowerCase().endsWith(".jpeg") ||
                               f.getName().toLowerCase().endsWith(".png") ||
                               f.getName().toLowerCase().endsWith(".gif"))) 
            {
                return getThumbnailIcon(f);
            }
            return super.getIcon(f);
            }
            
            private Icon getThumbnailIcon(File file) 
            {
                try 
                {
                    ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                    Image image = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH); // Resize the image
                    return new ImageIcon(image);
                } catch (Exception e) 
                {
                    return null;
                }
            }
        });

        // Open the dialog and get the result
        int returnValue = fileChooser.showOpenDialog(this);

        // Check if the user selected a file
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles(); // Get the selected files
            System.out.println("Selected Images:");

            // Define the target folder
            File targetFolder = SlideShowManager.getProgramFolder();
            for (File selectedFile : selectedFiles) {
                System.out.println("Selected Image: " + selectedFile.getAbsolutePath());            
                File targetFile = new File(targetFolder, selectedFile.getName());

            try {
                // Copy the file to the target folder, replacing if it already exists
                Files.copy(selectedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied image to: " + targetFile.getAbsolutePath());

                // Check if the imageFiles array exists, if not initialize it
                if (imageFiles == null) {
                    imageFiles = new File[] { targetFile };
                } else {
                    // Ensure the image is not already in the array
                    boolean exists = false;
                    for (File file : imageFiles) {
                        if (file.getName().equals(targetFile.getName())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        // Expand array to include the new image
                        imageFiles = Arrays.copyOf(imageFiles, imageFiles.length + 1);
                        imageFiles[imageFiles.length - 1] = targetFile;
                    }
                }

                // Display the newly added image
                updateImage();

                } catch (IOException ex) {
                    System.err.println("Error copying image: " + selectedFile.getAbsolutePath());
                }
            }
        } else {
            System.out.println("No image selected.");
        }
    }//GEN-LAST:event_addImageMenuItemActionPerformed

    // Allows user to save currently created slideshow
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Slideshow");
        fileChooser.setCurrentDirectory(SlideShowSaveFolder.getSaveFolder()); // Sets directory to created folder for saved slideshows
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Slideshow Files (*.ssx)", "ssx"));
    
        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".ssx")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".ssx");
            }
            saveSlideshow(fileToSave);
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed
 
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
    private javax.swing.JMenuItem addImageMenuItem;
    private javax.swing.JButton firstSlideButton;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JButton lastSlideButton;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton nextSlideButton;
    private javax.swing.JButton presenterButton;
    private javax.swing.JButton previousSlideButton;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem selectFolderMenuItem;
    // End of variables declaration//GEN-END:variables
}
