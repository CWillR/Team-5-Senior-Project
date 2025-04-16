/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team5.senior_project;

import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class SlideshowSettingsSaver {
    
    /**
     * Saves the slideshow settings including slide images, audio files, and per-slide transitions.
     *
     * @param filePath      The path of the file to save the settings.
     * @param slideshowName The name of the slideshow.
     * @param slides        List of slides.
     * @param audioFiles    List of audio files.
     * @param loop          Whether the slideshow should loop.
     * @param mode          The playback mode.
     * @param interval      The slide interval (if applicable).
     * @param transitions   List of transition names (one per slide) as Strings.
     */
    public static void saveSettingsToJson(String filePath, String slideshowName, List<Slide> slides, List<File> audioFiles, 
                                          boolean loop, String mode, int interval, List<String> transitions) {
        JSONObject slideshowJson = new JSONObject();
        slideshowJson.put("name", slideshowName);
        slideshowJson.put("loop", loop);
        slideshowJson.put("mode", mode); // Save the mode selection
        slideshowJson.put("interval", interval);

        // If there are audio files, add them as an array.
        if (audioFiles != null && !audioFiles.isEmpty()) {
            JSONArray audioArray = new JSONArray();
            for (File audioFile : audioFiles) {
                audioArray.put(audioFile.getAbsolutePath());
            }
            slideshowJson.put("audio", audioArray);
        }

        // Build the slides array.
        JSONArray slidesArray = new JSONArray();
        for (int i = 0; i < slides.size(); i++) {
            JSONObject slideJson = new JSONObject();
            slideJson.put("image", slides.get(i).getImagePath());
            // Attach the transition for this slide if available.
            if (transitions != null && transitions.size() > i) {
                slideJson.put("transition", transitions.get(i));
            } else {
                slideJson.put("transition", "No Transition");
            }
            // Save the transition duration (in milliseconds)
            slideJson.put("transitionDuration", slides.get(i).getTransitionDuration());
            slidesArray.put(slideJson);
        }
        slideshowJson.put("slides", slidesArray);

        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(slideshowJson.toString(4)); // 4 for nice indentation
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
