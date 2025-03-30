package com.team5.senior_project;

public class SlideshowSettings {
    public int duration; // in milliseconds
    public boolean loop;
    public boolean autoMode;
    public boolean isNavigationButtonsVisible;

    public SlideshowSettings(int duration, boolean loop, boolean autoMode, boolean isNavigationButtonsVisible) {
        this.duration = duration;
        this.loop = loop;
        this.autoMode = autoMode;
        this.isNavigationButtonsVisible = isNavigationButtonsVisible;
    }
}
