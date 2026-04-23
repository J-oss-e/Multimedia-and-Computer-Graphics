package com.josse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AppController {
    private APIClient apiClient;
    private FFmpegProcessor fFmpegProcessor;
    private MapGenerator mapGenerator;
    private ScaleCalculator scaleCalculator; 
    private VideoAssembler videoAssembler;
    private List<VisualMedia> allMedia;
    private Path essenceImage;
    private Path map;

    public AppController(String apiKey, String ffmpegPath){
        this.apiClient = new APIClient(apiKey);
        this.fFmpegProcessor = new FFmpegProcessor(ffmpegPath);
        this.mapGenerator = new MapGenerator();
        this.scaleCalculator = new ScaleCalculator();
        this.allMedia = new ArrayList<>();
        this.videoAssembler = null; 
        this.essenceImage = null;
        this.map = null;
    }

    public void addMedia(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        VisualMedia media;

        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "webp":
                media = new Photo();
                media.path = filePath;
                media.type = dataType.PHOTO;
                break;
            case "mp4":
            case "mov":
            case "avi":
                media = new Video();
                media.path = filePath;
                media.type = dataType.VIDEO;
                break;
            default:
                System.out.println("Formato no soportado: " + extension);
                return;
        }

        media.extractMetadata(fFmpegProcessor);
        allMedia.add(media);
    }

    public void processMedia(){
        //TODO
    }

    public void generateAIContent(){
        //TODO
    }

    public void generateMap(){
        //TODO
    }

    public Path createVideo(){
        //TODO
        Path pathFinal = null;
        return pathFinal;
    }

    public List<VisualMedia> getAllMedia() {
        return allMedia;
    }
}
