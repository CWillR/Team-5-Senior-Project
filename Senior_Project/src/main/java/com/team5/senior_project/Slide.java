/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team5.senior_project;

/**
 * Represents a single slide in a slideshow.
 */
public class Slide {
    private String imagePath; // Path to the image file for this slide
    

    //Constructor to create a new Slide object.
    public Slide(String imagePath) {
        this.imagePath = imagePath; // Initialize the image path        
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}