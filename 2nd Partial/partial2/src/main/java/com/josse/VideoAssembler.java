package com.josse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VideoAssembler {
    
    private List<VisualMedia> allMedia;
    private Path map;
    private Path essenceImage;

    public VideoAssembler(List<VisualMedia> allMedia, Path map, Path essenceImage){
        this.allMedia = allMedia;
        this.map = map;
        this.essenceImage = essenceImage;
    }

    public List<VisualMedia> orderMedia(List<VisualMedia> toOrderMedia){
        //TODO
        List<VisualMedia> finalMedia = new ArrayList<>();
        return finalMedia;
    }
    
    public Path generateFinalVideo(){
        //TODO
        Path finalVideo = null;
        return finalVideo;
    }
}
