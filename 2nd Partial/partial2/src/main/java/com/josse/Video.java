package com.josse;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Video extends VisualMedia {

    private double duration;

    public Video() {
        super();
        this.duration = 0;
    }

    public Video(String name, Path path, dataType type, double latitude, double longitude, LocalDateTime date, double duration, int width, int height) {
        super(name, path, type, latitude, longitude, date, width, height);
        this.duration = duration;
    }

    @Override
    public void extractMetadata(FFmpegProcessor ffmpeg) {
        String jsonString = ffmpeg.extractMediaMetadata(this.path);
        
        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray streams = json.getAsJsonArray("streams");
        JsonObject firstStream = streams.get(0).getAsJsonObject();

        // Dimensiones
        this.width = firstStream.get("width").getAsInt();
        this.height = firstStream.get("height").getAsInt();

        // Duración
        this.duration = Double.parseDouble(firstStream.get("duration").getAsString());

        // Fecha
        JsonObject tags = firstStream.getAsJsonObject("tags");
        String creationTime = tags.get("creation_time").getAsString();
        this.date = LocalDateTime.parse(creationTime,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));

        // GPS
        String location = tags.get("location").getAsString();
        this.latitude = Double.parseDouble(location.substring(0, 8));
        this.longitude = Double.parseDouble(location.substring(8));
    }
    
    public double getDuration() {
        return duration;
    }
    
}
