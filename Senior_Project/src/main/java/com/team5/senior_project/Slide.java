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
    private String transition; // Transition effect to use when moving to the next slide
    

    //Constructor to create a new Slide object.
    public Slide(String imagePath, String transition) {
        this.imagePath = imagePath; // Initialize the image path
        this.transition = transition; // Initialize the transition effect
        
    }
    
    public String getImagePath() {
        return imagePath;
    }

    public String getTransition() {
        return transition;
    }

  
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
 
    public void setTransition(String transition) {
        this.transition = transition;
    }
}