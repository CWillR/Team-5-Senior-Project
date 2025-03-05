/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team5.senior_project;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SlideshowSettingsSaver {

    public static void saveSettingsToJson(String filePath, String slideshowName, List<Slide> slides, String audioPath, boolean loop) {
        JSONObject slideshowJson = new JSONObject();
        slideshowJson.put("name", slideshowName);
        slideshowJson.put("loop", loop);

        if (audioPath != null && !audioPath.isEmpty()) {
            slideshowJson.put("audio", audioPath);
        }

        JSONArray slidesArray = new JSONArray();
        for (Slide slide : slides) {
            JSONObject slideJson = new JSONObject();
            slideJson.put("image", slide.getImagePath());
            slideJson.put("duration", slide.getDuration());
            slideJson.put("transition", slide.getTransition());
            slideJson.put("interval", slide.getInterval());
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
