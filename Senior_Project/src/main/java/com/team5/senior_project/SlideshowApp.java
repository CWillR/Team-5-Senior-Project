/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.team5.senior_project;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.UIManager;

/**
 *
 * @author Team 5
 * 
 * If there are errors that say "No main class found", right click the project, 
 * click on properties, click on Run, and select the main class there.
 */

public class SlideshowApp {

    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new SlideshowCreator().setVisible(true);
        });
    }
}



