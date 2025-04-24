/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.team5.senior_project;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * @author Conner Williams
 */
public class AudioTimelinePanel extends javax.swing.JPanel {
    private List<File> audioFiles;
    private int totalSlideshowDuration; // in seconds
    private boolean autoMode;
    private List<SegmentBounds> segmentBoundsList = new ArrayList<>();
    private JPopupMenu popupMenu;
    private int clickedSegmentIndex = -1;
    private List<Integer> slideDurationsWithTransitions;
    private int pixelsPerSecond;


    public AudioTimelinePanel(List<File> audioFiles, List<Integer> slideDurationsWithTransitions, boolean autoMode, int pixelsPerSecond) {
        this.audioFiles = audioFiles;
        this.slideDurationsWithTransitions = slideDurationsWithTransitions;
        this.autoMode = autoMode;
        this.pixelsPerSecond = pixelsPerSecond;
        this.totalSlideshowDuration = slideDurationsWithTransitions.stream().mapToInt(Integer::intValue).sum();
        setPreferredSize(new Dimension(totalSlideshowDuration * pixelsPerSecond, 50)); // Force height
        setBackground(Color.LIGHT_GRAY); // Debugging: Make it visible
        
        // Setup context menu
        popupMenu = new JPopupMenu();
        JMenuItem removeItem = new JMenuItem("Remove Audio");
        popupMenu.add(removeItem);
        
        removeItem.addActionListener(e -> {
           if (clickedSegmentIndex >= 0 && clickedSegmentIndex < audioFiles.size()) {
               audioFiles.remove(clickedSegmentIndex);
               repaint();
               revalidate();
           } 
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
            
            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int xClick = e.getX();
                    for (int i = 0; i < segmentBoundsList.size(); i++) {
                        SegmentBounds bounds = segmentBoundsList.get(i);
                        if (xClick >= bounds.startX && xClick <= bounds.endX) {
                            clickedSegmentIndex = i;
                            popupMenu.show(AudioTimelinePanel.this, e.getX(), e.getY());
                            break;
                        }
                    }
                }
            }
        });
    }
        

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        segmentBoundsList.clear();

        if (audioFiles == null || audioFiles.isEmpty() || slideDurationsWithTransitions == null) return;

        int panelHeight = getHeight();

        // Draw slide segments (as gray blocks)
        int x = 0;
        for (int i = 0; i < slideDurationsWithTransitions.size(); i++) {
            int slideDuration = slideDurationsWithTransitions.get(i);
            int width = slideDuration * pixelsPerSecond;

            g.setColor(i % 2 == 0 ? Color.GRAY : Color.LIGHT_GRAY); // alternating colors
            g.fillRect(x, 0, width, panelHeight);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, 0, width, panelHeight);

            x += width;
        }

        // Draw audio segments on top
        int audioStartSec = 0;
        for (int i = 0; i < audioFiles.size(); i++) {
            File audioFile = audioFiles.get(i);
            int duration = getAudioDuration(audioFile);
            if (duration <= 0) continue;

            int startX = audioStartSec * pixelsPerSecond;
            int width = duration * pixelsPerSecond;

            g.setColor(Color.BLUE);
            g.fillRect(startX, 10, width, panelHeight - 20);
            g.setColor(Color.BLACK);
            g.drawRect(startX, 10, width, panelHeight - 20);

            // Filename label
            String filename = audioFile.getName();
            FontMetrics metrics = g.getFontMetrics();
            String displayName = filename;
            if (metrics.stringWidth(displayName) > width - 4) {
                while (displayName.length() > 3 && metrics.stringWidth(displayName + "...") > width - 4) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName += "...";
            }

            int textX = startX + Math.max((width - metrics.stringWidth(displayName)) / 2, 2);
            int textY = 10 + ((panelHeight - 20 + metrics.getHeight()) / 2) - 4;
            g.setColor(Color.WHITE);
            g.drawString(displayName, textX, textY);

            segmentBoundsList.add(new SegmentBounds(startX, startX + width));
            audioStartSec += duration;
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
    
    private static class SegmentBounds {
        int startX;
        int endX;
        
        SegmentBounds(int startX, int endX) {
            this.startX = startX;
            this.endX = endX;
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
