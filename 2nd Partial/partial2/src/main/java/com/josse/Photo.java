package com.josse;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Photo extends VisualMedia {

    public Photo() {
        super();
    }

    public Photo(String name, Path path, dataType type, double latitude, double longitude, LocalDateTime date, int width, int height) {
        super(name, path, type, latitude, longitude, date, width, height);
    }

    @Override
    public void extractMetadata() {
        //To do: Implement metadata extraction for photos using fmmpeg
    }
}
