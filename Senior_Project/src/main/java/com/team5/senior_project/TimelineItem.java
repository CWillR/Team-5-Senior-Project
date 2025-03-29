package com.team5.senior_project;

import java.io.File;

public class TimelineItem {
    private File imageFile;
    private TransitionType transition;
    
    public TimelineItem(File imageFile, TransitionType transition) {
        this.imageFile = imageFile;
        this.transition = transition;
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
    
    @Override
    public String toString() {
        return imageFile.getName();
    }
}
