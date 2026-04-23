package com.josse;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Abstract base for any visual media item (photo or video).
 * Subclasses must implement extractMetadata to populate fields from
 * their respective metadata sources (EXIF vs ffprobe).
 */
public abstract class VisualMedia {

    protected String name;
    protected Path path;
    // Separate from path so the original file is never overwritten during scaling.
    protected Path scaledPath;
    protected dataType type;
    // 0.0 means "no GPS data" — matches the default and the null-island check in AppController.
    protected double latitude;
    protected double longitude;
    protected LocalDateTime date;
    protected int width;
    protected int height;

    public VisualMedia() {
        this.name = "";
        this.path = null;
        this.scaledPath = null;
        this.type = null;
        this.latitude = 0;
        this.longitude = 0;
        this.date = null;
        this.width = 0;
        this.height = 0;
    }

    public VisualMedia(String name, Path path, dataType type,
                       double latitude, double longitude,
                       LocalDateTime date, int width, int height) {
        this.name = name;
        this.path = path;
        this.scaledPath = null;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.width = width;
        this.height = height;
    }

    /**
     * Populates name, dimensions, date and GPS from the file's metadata.
     * FFmpegProcessor is passed because Video needs ffprobe; Photo ignores it
     * and uses metadata-extractor directly instead.
     */
    public abstract void extractMetadata(FFmpegProcessor ffmpeg);

    // Getters
    public String getName()          { return name; }
    public Path getPath()            { return path; }
    public Path getScaledPath()      { return scaledPath; }
    public dataType getType()        { return type; }
    public double getLatitude()      { return latitude; }
    public double getLongitude()     { return longitude; }
    public LocalDateTime getDate()   { return date; }
    public int getWidth()            { return width; }
    public int getHeight()           { return height; }

    public void setScaledPath(Path scaledPath) {
        this.scaledPath = scaledPath;
    }
}