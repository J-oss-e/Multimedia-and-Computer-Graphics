package com.josse;

//import java.nio.file.Path;
import java.nio.file.Paths;

public class main {
    public static void main(String[] args) {

        String apiKey     = "TU_API_KEY_AQUI";
        String ffmpegPath = "ffmpeg";

        AppController controller = new AppController(apiKey, ffmpegPath);

        // USA RUTAS REALES de fotos tuyas que tengas a la mano
        controller.addMedia(Paths.get("C:/Users/Angel/Downloads/12382975864_09e6e069e7_o.jpg"));

        for (VisualMedia m : controller.getAllMedia()) {
            System.out.println("--- " + m.getName());
            System.out.println("  Dimensiones: " + m.getWidth() + "x" + m.getHeight());
            System.out.println("  Fecha:       " + m.getDate());
            System.out.println("  GPS:         " + m.getLatitude() + ", " + m.getLongitude());
        }
    }
}