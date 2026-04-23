package com.josse;

import java.nio.file.Path;
import java.nio.file.Paths;

public class main {
    public static void main(String[] args) {
        
        String apiKey = "TU_API_KEY_AQUI";
        String ffmpegPath = "ffmpeg";
        
        AppController controller = new AppController(apiKey, ffmpegPath);
        
        Path photoPath = Paths.get("C:\\Users\\Angel\\Downloads\\12382975864_09e6e069e7_o.jpg\\");
        controller.addMedia(photoPath);

        VisualMedia media = controller.getAllMedia().get(0);
        System.out.println("Width: " + media.getWidth());
        System.out.println("Height: " + media.getHeight());
        System.out.println("Date: " + media.getDate());
        System.out.println("Latitude: " + media.getLatitude());
        System.out.println("Longitude: " + media.getLongitude());
    }
}