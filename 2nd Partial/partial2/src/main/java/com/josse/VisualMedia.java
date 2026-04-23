package com.josse;

import java.nio.file.Path;
import java.time.LocalDateTime;

public abstract class VisualMedia {

    protected String name;
    protected Path path;
    protected dataType type;
    protected double latitude;
    protected double longitude;
    protected LocalDateTime date;
    protected int width;
    protected int height;

    public VisualMedia() {
        this.name = "";
        this.path = null;
        this.type = null;
        this.latitude = 0;
        this.longitude = 0;
        this.date = null;
        this.width = 0;
        this.height = 0;
        
    }

    public VisualMedia(String name, Path path, dataType type, double latitude, double longitude, LocalDateTime date, int width, int height) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.width = width;
        this.height = height;

    }

    public abstract void extractMetadata(FFmpegProcessor ffmpeg);

    public String getName() {
        return name;
    }
    public Path getPath() {
        return path;
    }
    public dataType getType() {
        return type;
    }
    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public LocalDateTime getDate() {
        return date;
    }
    public int getWidth(){
        return width;
    }
    public int getHeight(){
        return height;
    }
}
