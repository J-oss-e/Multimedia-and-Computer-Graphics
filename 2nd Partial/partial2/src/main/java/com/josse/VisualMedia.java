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

    public VisualMedia() {
        this.name = "";
        this.path = null;
        this.type = null;
        this.latitude = 0;
        this.longitude = 0;
        this.date = null;
    }

    public VisualMedia(String name, Path path, dataType type, double latitude, double longitude, LocalDateTime date) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
    }

    public abstract void metadata();

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
}
