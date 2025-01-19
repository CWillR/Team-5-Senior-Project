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

public class SlideshowApp {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new SlideshowCreator().setVisible(true);
        });
    }
}



