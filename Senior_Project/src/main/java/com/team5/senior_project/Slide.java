/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team5.senior_project;
import java.io.File;

/**
 * Represents a single slide in a slideshow.
 */
public class Slide {
    private File imageFile;
    private TransitionType transition;
    private int transitionDuration; // Duration of the transition in milliseconds
    private String imagePath; // Path to the image file for this slide
    

    //Constructor to create a new Slide object.
    public Slide(String imagePath, File imageFile, TransitionType transition) {
        this.imagePath = imagePath; // Initialize the image path      
        this.imageFile = imageFile;
        this.transition = transition;
        transitionDuration = 2000; // Default duration
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public File getImageFile() {
        return imageFile;
    }
    
    public TransitionType getTransition() {
        return transition;
    }
    
    public void setTransition(TransitionType transition) {
        this.transition = transition;
    }

    public int getTransitionDuration() {
        return transitionDuration;
    }

    public void setTransitionDuration(int transitionDuration) {
        this.transitionDuration = transitionDuration;
    }
    
    @Override
    public String toString() {
        return imageFile.getName();
    }
}