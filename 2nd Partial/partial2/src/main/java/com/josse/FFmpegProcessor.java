package com.josse;

import java.nio.file.Path;
import java.util.List;


public class FFmpegProcessor {

    private String ffmpegPath;

    public FFmpegProcessor(){
        this.ffmpegPath = "";
    }

    public FFmpegProcessor(String ffmpegPath){
        this.ffmpegPath = ffmpegPath;
    }

    private boolean commandExecution(String[] instruction){
        boolean flag = true;
        //TODO
        return flag;
    }

    public boolean scaleMedia(VisualMedia media, double[] scales){
        boolean flag = true;
        //TODO
        return flag;
    }

    public boolean normalizeAudio(Path Audio){
        boolean flag = true;
        //TODO
        return flag;
    }

    public boolean assembleVideo(Path finalPath, List<VisualMedia> allMedia){
        boolean flag = true;
        //TODO
        return flag;
    }

}