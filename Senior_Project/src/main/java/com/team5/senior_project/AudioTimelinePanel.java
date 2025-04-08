/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.team5.senior_project;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author Conner Williams
 */
public class AudioTimelinePanel extends javax.swing.JPanel {
    private List<File> audioFiles;
    private int totalSlideshowDuration; // in seconds
    private boolean autoMode;


    public AudioTimelinePanel(List<File> audioFiles, int totalSlideshowDuration, boolean autoMode) {
        this.audioFiles = audioFiles;
        this.totalSlideshowDuration = totalSlideshowDuration;
        this.autoMode = autoMode;
        setPreferredSize(new Dimension(800, 50)); // Force height
        setBackground(Color.LIGHT_GRAY); // Debugging: Make it visible
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        System.out.println("Painting AudioTimelinePanel...");

        if (audioFiles == null || audioFiles.isEmpty()) {
            System.out.println("No audio files to display.");
            return;
        }

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int x = 0;

        // Debugging total duration
        System.out.println("Total Slideshow Duration: " + totalSlideshowDuration);

        for (int i = 0; i < audioFiles.size(); i++) {
            File audioFile = audioFiles.get(i);
            int audioDuration = getAudioDuration(audioFile);
            if (audioDuration <= 0) continue;

            System.out.println("Audio File: " + audioFile.getName() + " | Duration: " + audioDuration + " sec");

            // Ensure proportional width based on total duration
            int width;

            if (autoMode) {
                width = (int) ((audioDuration / (double) totalSlideshowDuration) * panelWidth);
                if (width < 5 && audioDuration > 0) {
                    // Adjust very small durations to be visible
                    width = Math.max(5, (int) (panelWidth * 0.02)); // At least 2% of the total width
                }
            } else {
                width = panelWidth / audioFiles.size();
            }
            
            // Draw audio segment
            g.setColor(Color.BLUE);
            g.fillRect(x, 10, width, panelHeight - 20);
            g.setColor(Color.BLACK);
            g.drawRect(x, 10, width, panelHeight - 20);
            
            // Draw filename inside the segment
            String filename = audioFile.getName();
            FontMetrics metrics = g.getFontMetrics();
            int stringWidth = metrics.stringWidth(filename);
            int stringHeight = metrics.getHeight();
            int textX = x + Math.max((width - stringWidth) / 2, 2); // Ensure padding
            int textY = 10 + ((panelHeight - 20 + stringHeight) / 2) - 4;
            
            // Clip text if too long
            if (stringWidth > width - 4) {
                while (filename.length() > 3 && metrics.stringWidth(filename + "...") > width - 4) {
                    filename = filename.substring(0, filename.length() - 1);
                }
                filename += "...";
            }
            
            g.setColor(Color.WHITE);
            g.drawString(filename, textX, textY);

            // Draw a white separator if not the last segment
            if (i < audioFiles.size() - 1) {
                g.setColor(Color.WHITE);
                g.fillRect(x + width - 1, 10, 2, panelHeight - 20);
            }

            x += width;
        }
    }

    private int getAudioDuration(File audioFile) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioStream.getFormat();
            long frames = audioStream.getFrameLength();
            double duration = (frames + 0.0) / format.getFrameRate();
            return (int) duration;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
