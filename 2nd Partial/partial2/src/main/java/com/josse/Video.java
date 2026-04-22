package com.josse;
import java.nio.file.Path;
import java.time.LocalDateTime;

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
    public void extractMetadata() {
        //To do: Implement metadata extraction for videos using fmmpeg
    }
    
    public double getDuration() {
        return duration;
    }
    
}
