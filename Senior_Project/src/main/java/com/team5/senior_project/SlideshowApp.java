/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.team5.senior_project;

/**
 *
 * @author Team 5
 * 
 * If there are errors that say "No main class found", right click the project, 
 * click on properties, click on Run, and select the main class there.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SlideshowApp {

    public static void main(String[] args) {
        // Launch the application using the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            SlideshowApp window = new SlideshowApp();
            window.createAndShowGUI();
        });
    }

    // Method to create and show the main GUI
    private void createAndShowGUI() {
        JFrame frame = createFrame("Slideshow Application"); // Create the main window
        JPanel panel = createMainPanel(); // Create a panel to hold components

        // Add components to the panel
        panel.add(createButton("Test", e -> showMessage(frame, "Test Worked")));

        // Add the panel to the frame
        frame.add(panel);

        // Finalize and display the frame
        frame.setVisible(true);
    }

    // Method to create the main JFrame (window)
    private JFrame createFrame(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());
        return frame;
    }

    // Method to create the main JPanel (container for components)
    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        return panel;
    }

    // Method to create a JButton with an ActionListener
    private JButton createButton(String text, ActionListener actionListener) {
        JButton button = new JButton(text);
        button.addActionListener(actionListener);
        return button;
    }

    // Method to show a message dialog
    private void showMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message);
    }
}



