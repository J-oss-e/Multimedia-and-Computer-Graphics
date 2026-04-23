package com.josse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Representa un video. Usa ffprobe para extraer metadata.
 * Extiende VisualMedia, por lo que hereda atributos como name, path, type, GPS y fecha.
 */
public class Video extends VisualMedia {

    private double duration;

    // Patrón para parsear coordenadas GPS del formato ffprobe
    private static final Pattern GPS_PATTERN =
        Pattern.compile("([+-]\\d+\\.\\d+)([+-]\\d+\\.\\d+)");

    public Video() {
        super();
        this.type = dataType.VIDEO;
        this.duration = 0;
    }

    @Override
    public void extractMetadata(FFmpegProcessor ffmpeg) {
        String jsonString = ffmpeg.extractMediaMetadata(this.path);
        if (jsonString == null) {
            System.err.println("ffprobe no retornó datos para: " + this.path);
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

            // --- Dimensiones y duración desde streams ---
            JsonArray streams = json.getAsJsonArray("streams");
            for (int i = 0; i < streams.size(); i++) {
                JsonObject stream = streams.get(i).getAsJsonObject();
                String codecType = stream.get("codec_type").getAsString();
                if ("video".equals(codecType)) {
                    this.width    = stream.get("width").getAsInt();
                    this.height   = stream.get("height").getAsInt();
                    if (stream.has("duration")) {
                        this.duration = Double.parseDouble(
                            stream.get("duration").getAsString());
                    }
                    break;
                }
            }

            // --- Fecha y GPS desde format.tags ---
            JsonObject format = json.getAsJsonObject("format");
            if (format != null && format.has("tags")) {
                JsonObject tags = format.getAsJsonObject("tags");

                if (tags.has("creation_time")) {
                    String creationTime = tags.get("creation_time").getAsString();
                    // Formato: 2024-01-15T12:30:00.000000Z
                    DateTimeFormatter fmt = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    this.date = LocalDateTime.parse(
                        creationTime.substring(0, 20).replace(".000000", ""), fmt);
                }

                if (tags.has("location")) {
                    String location = tags.get("location").getAsString();
                    Matcher m = GPS_PATTERN.matcher(location);
                    if (m.find()) {
                        this.latitude  = Double.parseDouble(m.group(1));
                        this.longitude = Double.parseDouble(m.group(2));
                    }
                }
            }

            this.name = this.path.getFileName().toString();

        } catch (Exception e) {
            System.err.println("Error parseando metadata de video: " + this.path);
            e.printStackTrace();
        }
    }

    public double getDuration() { return duration; }
}