package com.josse;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Clase abstracta que representa cualquier medio visual (foto o video).
 * Define la estructura común y obliga a implementar extractMetadata.
 */
public abstract class VisualMedia {

    protected String name;
    protected Path path;
    protected Path scaledPath; // ruta del archivo escalado
    protected dataType type;
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

    /** Extrae metadata del archivo (dimensiones, fecha, GPS). */
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