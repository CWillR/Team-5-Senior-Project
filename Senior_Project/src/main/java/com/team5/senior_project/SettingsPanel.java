/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.team5.senior_project;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

/**
 *
 * @author Team 5
 */
public class SettingsPanel extends javax.swing.JPanel {

    // Instance variable to track automatic mode
    private boolean autoMode = false;
    
    /**
     * Creates new form SettingsPanel
     */
    public SettingsPanel() {
        initComponents();
        // Set default to "Preset Duration"
        modeComboBox.setSelectedIndex(1); // index 1 corresponds to "Preset Duration"
        // Set default interval to 3 seconds
        intervalTextField.setText("3");
        // Since we are in Preset Duration mode, update the autoMode flag
        autoMode = true;
        // Optionally, force layout update if needed
        intervalText.getParent().revalidate();
        intervalText.getParent().repaint();
        
        // Add listener for mode changes
        modeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMode();
            }
        });
    }

    /**
     * Update the settings panel based on the selected mode.
     * When the mode is "Manual Duration", hide the interval input;
     * when "Preset Duration", show the interval input.
     */
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
            // Force a layout update
            intervalText.getParent().revalidate();
            intervalText.getParent().repaint();
            autoSlideChange();
        }
    }    
    
    private void autoSlideChange() {
        System.out.println("Auto slide change (Preset Duration) activated");
        // Implement auto mode functionality here
    }
    
    private void manualSlideChange() {
        System.out.println("Manual slide change activated");
        // Implement manual mode functionality here
    }
    
        // Public getters so the main frame can retrieve settings if needed

    public String getSelectedMode() {
        return (String) modeComboBox.getSelectedItem();
    }

    public String getIntervalText() {
        return intervalTextField.getText();
    }
    
    public String getPlaybackMode() {
        return (String) playbackModeBox.getSelectedItem();
    }
    
    public boolean isAutoMode() {
        return autoMode;
    }

    public void setPlaybackMode(String mode) {
        playbackModeBox.setSelectedItem(mode);
    }

    public void setIntervalText(String text) {
        intervalTextField.setText(text);
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
        modeComboBox.setSelectedItem(autoMode ? "Preset Duration" : "Manual Duration");
        updateMode(); // Update the UI based on the new mode
    }

    public void setSelectedMode(String mode) {
        modeComboBox.setSelectedItem(mode);
        updateMode(); // Update the UI based on the new mode
    }

    public SlideshowSettings getSlideshowSettings() {
        int duration = 3000; // default duration in milliseconds
        try {
            String intervalStr = getIntervalText().trim();
            double seconds = Double.parseDouble(intervalStr);
            // Enforce a minimum interval of 0.01 seconds.
            if (seconds < 0.01) {
                seconds = 0.01;
            }
            duration = (int) (seconds * 1000);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid interval value. Using default interval of 3 seconds.");
        }
        boolean loop = getPlaybackMode().equals("Loop Slideshow");
        boolean autoMode = isAutoMode();
        boolean isNavigationButtonsVisible = showNavButtonsToggle.isSelected();
        
        return new SlideshowSettings(duration, loop, autoMode, isNavigationButtonsVisible);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        playbackModeLabel = new javax.swing.JLabel();
        playbackModeBox = new javax.swing.JComboBox<>();
        secondsText = new javax.swing.JLabel();
        intervalTextField = new javax.swing.JTextField();
        intervalText = new javax.swing.JLabel();
        modeComboBox = new javax.swing.JComboBox<>();
        modeSelectionLabel = new javax.swing.JLabel();
        showNavButtonsToggle = new javax.swing.JRadioButton();

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

        modeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Manual Duration", "Preset Duration" }));

        modeSelectionLabel.setText("Select Automatic or Manual Slide Show");

        showNavButtonsToggle.setText("Show Navigation Buttons");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(modeSelectionLabel)
                    .addComponent(modeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(playbackModeLabel)
                    .addComponent(playbackModeBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(intervalText)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(intervalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(secondsText))
                    .addComponent(showNavButtonsToggle))
                .addContainerGap(286, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modeSelectionLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(modeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(intervalText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(intervalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(secondsText))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playbackModeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playbackModeBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showNavButtonsToggle)
                .addContainerGap(293, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void playbackModeBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playbackModeBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_playbackModeBoxActionPerformed

    private void intervalTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intervalTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_intervalTextFieldActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel intervalText;
    private javax.swing.JTextField intervalTextField;
    private javax.swing.JComboBox<String> modeComboBox;
    private javax.swing.JLabel modeSelectionLabel;
    private javax.swing.JComboBox<String> playbackModeBox;
    private javax.swing.JLabel playbackModeLabel;
    private javax.swing.JLabel secondsText;
    private javax.swing.JRadioButton showNavButtonsToggle;
    // End of variables declaration//GEN-END:variables
}
