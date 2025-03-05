/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team5.senior_project;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.UIManager;
/**
 *
 * @author 12564
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