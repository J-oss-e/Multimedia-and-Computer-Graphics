package com.josse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private String commandExecution(String[] instruction) {
        try {
            ProcessBuilder pb = new ProcessBuilder(instruction);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            process.waitFor();
            return output.toString();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String extractMediaMetadata(Path Media){
        String[] cmd = {
            "ffprobe",
            "-v", "quiet",
            "-print_format", "json",
            "-show_streams",
            "-show_format",
            Media.toString()
        }; 
        return commandExecution(cmd); 
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