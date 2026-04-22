package com.josse;

import java.nio.file.Path;
//import java.util.Base64;

public class APIClient {
    
    private String apiKey;
    private static final String baseUrl = "baseURL";

    public APIClient(String apiKey){
        this.apiKey = apiKey;
    }


    public String generateEssenceImage(String description){
        //TODO
        String finalImage = "TODO";
        return finalImage;
    }

    public String generateAudio(Path semiFinalMedia){
        //TODO
        String finalAudio = "TODO";
        return finalAudio;
    }

    public String generatePhrase(String description){
        //TODO
        String finalPhrase = "TODO";
        return finalPhrase;
    }

}