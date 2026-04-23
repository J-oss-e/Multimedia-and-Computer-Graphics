package com.josse;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Photo extends VisualMedia {

    public Photo() {
        super();
    }

    public Photo(String name, Path path, dataType type, double latitude, double longitude, LocalDateTime date, int width, int height) {
        super(name, path, type, latitude, longitude, date, width, height);
    }

    @Override
    public void extractMetadata(FFmpegProcessor ffmpeg) {
        String jsonString = ffmpeg.extractMediaMetadata(this.path);
        System.out.println(jsonString);
        
        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray streams = json.getAsJsonArray("streams");
        JsonObject firstStream = streams.get(0).getAsJsonObject();

        this.width = firstStream.get("width").getAsInt();
        this.height = firstStream.get("height").getAsInt();

        // Verificar si la imagen tiene metadata GPS/fecha
        if (!firstStream.has("tags")) {
            System.out.println("La imagen no tiene metadata GPS/fecha: " + this.path);
            return;
        }

        JsonObject tags = firstStream.getAsJsonObject("tags");
        String creationTime = tags.get("creation_time").getAsString();
        this.date = LocalDateTime.parse(creationTime, 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));

        String location = tags.get("location").getAsString();
        this.latitude = Double.parseDouble(location.substring(0, 8));
        this.longitude = Double.parseDouble(location.substring(8));
    }
}
