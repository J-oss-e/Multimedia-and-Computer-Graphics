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

    public void addMedia(Path filePath){
        //TODO
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
}
