package com.team5.senior_project;

public class SlideshowSettings {
    public int duration; // in milliseconds
    public boolean loop;
    public boolean autoMode;

    public SlideshowSettings(int duration, boolean loop, boolean autoMode) {
        this.duration = duration;
        this.loop = loop;
        this.autoMode = autoMode;
    }
}
