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
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Team 5
 */
public class SlideshowPresenter extends javax.swing.JFrame {

    private List<Slide> slides = new ArrayList<>();
    private int currentSlideIndex = 0;
    
    /**
     * Creates new form SlideshowPresenter
     */
    public SlideshowPresenter() {
        initComponents();
        imageLabel.setPreferredSize(new Dimension(600, 400)); // Adjust dimensions as needed
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
            // Print slideshow-level settings for testing
            System.out.println("Slideshow Settings:");
            System.out.println("  Name: " + slideshowJson.getString("name"));
            System.out.println("  Loop: " + slideshowJson.getBoolean("loop"));
            if (slideshowJson.has("audio")) {
                System.out.println("  Audio: " + slideshowJson.getString("audio"));
            } else {
                System.out.println("  Audio: null");
            }

            JSONArray slidesArray = slideshowJson.getJSONArray("slides");
            for (int i = 0; i < slidesArray.length(); i++) {
                JSONObject slideJson = slidesArray.getJSONObject(i);
                String imagePath = slideJson.getString("image");
                int duration = slideJson.getInt("duration");
                String transition = slideJson.getString("transition");
                int interval = slideJson.optInt("interval", 0);

                // Print slide-level settings for testing
                System.out.println("\n  Slide " + (i + 1) + ":");
                System.out.println("    Image: " + imagePath);
                System.out.println("    Duration: " + duration);
                System.out.println("    Transition: " + transition);
                System.out.println("    Interval: " + interval);

                slides.add(new Slide(imagePath, duration, transition, interval));
            }
            updateSlide();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading slideshow: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        firstSlideButton = new javax.swing.JButton();
        previousSlideButton = new javax.swing.JButton();
        nextSlideButton = new javax.swing.JButton();
        lastSlideButton = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openSlideMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Slideshow Presenter");

        firstSlideButton.setText("First");
        firstSlideButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firstSlideButtonActionPerformed(evt);
            }
        });

        previousSlideButton.setText("Previous");
        previousSlideButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousSlideButtonActionPerformed(evt);
            }
        });

        nextSlideButton.setText("Next");
        nextSlideButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextSlideButtonActionPerformed(evt);
            }
        });

        lastSlideButton.setText("Last");
        lastSlideButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lastSlideButtonActionPerformed(evt);
            }
        });

        fileMenu.setText("File");

        openSlideMenuItem.setText("Open Slide");
        openSlideMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openSlideMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openSlideMenuItem);

        jMenuBar1.add(fileMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(143, Short.MAX_VALUE)
                .addComponent(firstSlideButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                .addComponent(previousSlideButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                .addComponent(nextSlideButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                .addComponent(lastSlideButton)
                .addContainerGap(143, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(35, Short.MAX_VALUE)
                .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(firstSlideButton)
                    .addComponent(nextSlideButton)
                    .addComponent(previousSlideButton)
                    .addComponent(lastSlideButton))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Opens FileChooser for user to select a saved slideshow to load
    private void openSlideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openSlideMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser(SlideShowFileManager.getSavedSlidesFolder());
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            loadSlideshow(file);
        }
    }//GEN-LAST:event_openSlideMenuItemActionPerformed

    // Goes to first slide
    private void firstSlideButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firstSlideButtonActionPerformed
        if (!slides.isEmpty()) {
            currentSlideIndex = 0;
            updateSlide();
        }
    }//GEN-LAST:event_firstSlideButtonActionPerformed

    // Goes to the previous slide
    private void previousSlideButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousSlideButtonActionPerformed
        if (!slides.isEmpty() && currentSlideIndex > 0) {
            currentSlideIndex--;
            updateSlide();
        }
    }//GEN-LAST:event_previousSlideButtonActionPerformed

    // Goes to the next slide
    private void nextSlideButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextSlideButtonActionPerformed
        if (!slides.isEmpty() && currentSlideIndex < slides.size() - 1) {
            currentSlideIndex++;
            updateSlide();
        }
    }//GEN-LAST:event_nextSlideButtonActionPerformed

    // Goes to last slide
    private void lastSlideButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lastSlideButtonActionPerformed
        if (!slides.isEmpty()) {
            currentSlideIndex = slides.size() - 1;
            updateSlide();
        }
    }//GEN-LAST:event_lastSlideButtonActionPerformed

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
    private javax.swing.JMenu fileMenu;
    private javax.swing.JButton firstSlideButton;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JButton lastSlideButton;
    private javax.swing.JButton nextSlideButton;
    private javax.swing.JMenuItem openSlideMenuItem;
    private javax.swing.JButton previousSlideButton;
    // End of variables declaration//GEN-END:variables
}
