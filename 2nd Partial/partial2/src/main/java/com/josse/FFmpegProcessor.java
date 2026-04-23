package com.josse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Thin wrapper around the ffmpeg/ffprobe CLI.
 * All media processing (scaling, assembly, audio normalization) goes through
 * this class so the rest of the app never constructs raw ffmpeg commands.
 */
public class FFmpegProcessor {

    private String ffmpegPath;
    private static final int TARGET_W = (int) ScaleCalculator.getWidth();   // 1080
    private static final int TARGET_H = (int) ScaleCalculator.getHeight();  // 1920

    public FFmpegProcessor() {
        this.ffmpegPath = "ffmpeg";
    }

    public FFmpegProcessor(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    /**
     * Runs a CLI command and returns its combined stdout+stderr as a String.
     * redirectErrorStream(true) is required because ffmpeg writes all progress
     * output to stderr; without merging, the stderr pipe fills up and blocks the process.
     */
    private String commandExecution(String[] instruction) {
        try {
            ProcessBuilder pb = new ProcessBuilder(instruction);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("⚠️  Proceso terminó con código: " + exitCode);
            }

            return output.toString();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Returns ffprobe's full JSON report for a media file (streams + format). */
    public String extractMediaMetadata(Path media) {
        String[] cmd = {
            "ffprobe", "-v", "quiet",
            "-print_format", "json",
            "-show_streams", "-show_format",
            media.toString()
        };
        return commandExecution(cmd);
    }

    /**
     * Scales a photo or video to 1080x1920, preserving aspect ratio with black padding.
     * Photos are converted to a fixed-duration video clip; videos are re-encoded in place.
     * Delegates to the two-arg overload with a default duration of 3 seconds.
     */
    public boolean scaleMedia(VisualMedia media) {
        return scaleMedia(media, 3.0);
    }

    /**
     * Scales a media file to TARGET_W x TARGET_H.
     * The vf filter chain: scale down to fit inside the target box (preserving AR),
     * then pad the remaining space with black bars — letterbox for landscape, pillarbox for portrait.
     * Photos use -loop 1 to create a synthetic video stream before -t truncates it.
     */
    public boolean scaleMedia(VisualMedia media, double photoDuration) {
        String inputPath = media.getPath().toString();

        String fileName = media.getPath().getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String outputPath;

        // Photos become .mp4 clips; videos keep their extension.
        if (media.getType() == dataType.PHOTO) {
            outputPath = System.getProperty("java.io.tmpdir") +
                        "/scaled_" + baseName + ".mp4";
        } else {
            outputPath = System.getProperty("java.io.tmpdir") +
                        "/scaled_" + fileName;
        }

        // force_original_aspect_ratio=decrease shrinks without cropping;
        // pad then centers the result inside the full target canvas.
        String filter = String.format(
            "scale=%d:%d:force_original_aspect_ratio=decrease," +
            "pad=%d:%d:(ow-iw)/2:(oh-ih)/2:black",
            TARGET_W, TARGET_H, TARGET_W, TARGET_H
        );

        String[] cmd;
        if (media.getType() == dataType.PHOTO) {
            cmd = new String[]{
                ffmpegPath, "-y",
                "-loop", "1",       // repeat the still image as a video source
                "-i", inputPath,
                "-vf", filter,
                "-t", String.valueOf(photoDuration),
                "-r", "30",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",  // required for broad player compatibility
                outputPath
            };
        } else {
            cmd = new String[]{
                ffmpegPath, "-y",
                "-i", inputPath,
                "-vf", filter,
                "-c:v", "libx264",
                "-c:a", "aac",
                outputPath
            };
        }

        System.out.println("\n🎬 Escalando: " + media.getName());
        if (media.getType() == dataType.PHOTO) {
            System.out.println("   Duración: " + photoDuration + " segundos");
        }
        System.out.println("   Comando: " + String.join(" ", cmd));

        String result = commandExecution(cmd);

        boolean fileExists = Paths.get(outputPath).toFile().exists();
        boolean success = result != null && fileExists;

        if (success) {
            media.setScaledPath(Paths.get(outputPath));
            System.out.println("   ✅ Escalado exitoso → " + outputPath);
        } else {
            System.err.println("   ❌ Error escalando " + media.getName());
            if (result != null) {
                System.err.println("   Salida ffmpeg:\n" + result);
            }
        }

        return success;
    }

    /**
     * Normalizes audio loudness to YouTube's recommended standard:
     * -15 LUFS integrated, -1.5 dBTP true peak, 7 LU loudness range.
     * Using values outside this range causes YouTube to re-normalize on upload,
     * which degrades quality.
     */
    public boolean normalizeAudio(Path audio, Path output) {
        String[] normalizeCmd = {
            ffmpegPath, "-y",
            "-i", audio.toString(),
            "-af", "loudnorm=I=-15:TP=-1.5:LRA=7",
            output.toString()
        };

        System.out.println("🔊 Normalizando audio: " + audio.getFileName());
        String result = commandExecution(normalizeCmd);
        boolean success = result != null && output.toFile().exists();

        if (success) {
            System.out.println("   ✅ Audio normalizado");
        } else {
            System.err.println("   ❌ Error normalizando audio");
        }

        return success;
    }

    /**
     * Concatenates pre-scaled media files using the concat demuxer.
     * -safe 0 allows absolute paths in the list file, which is needed because
     * the tmp directory path varies per OS and may contain spaces.
     * -c copy avoids re-encoding since all inputs already share the same codec/resolution.
     */
    public boolean assembleVideoSimple(Path finalPath, List<VisualMedia> allMedia) {
        System.out.println("\n📦 Ensamblando video final...");

        StringBuilder concatList = new StringBuilder();
        for (VisualMedia m : allMedia) {
            Path p = (m.getScaledPath() != null) ? m.getScaledPath() : m.getPath();
            // Forward slashes are required in ffmpeg concat list files even on Windows.
            String absPath = p.toAbsolutePath().toString().replace("\\", "/");
            concatList.append("file '").append(absPath).append("'\n");
        }

        Path concatFile = Paths.get(System.getProperty("java.io.tmpdir"), "concat_list.txt");
        try {
            java.nio.file.Files.writeString(concatFile, concatList.toString());
            System.out.println("   Lista de concatenación creada: " + concatFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        String[] cmd = {
            ffmpegPath, "-y",
            "-f", "concat",
            "-safe", "0",
            "-i", concatFile.toString(),
            "-c", "copy",
            finalPath.toString()
        };

        System.out.println("   Ejecutando ensamblado...");
        String result = commandExecution(cmd);
        boolean success = result != null && finalPath.toFile().exists();

        if (success) {
            System.out.println("   ✅ Video final creado: " + finalPath.toAbsolutePath());
        } else {
            System.err.println("   ❌ Error ensamblando video");
            if (result != null) {
                System.err.println("   Salida ffmpeg:\n" + result);
            }
        }

        return success;
    }

    /**
     * Extracts frame 0 from a video as a PNG.
     * Used by VideoAssembler to treat video clips as stills, so duration
     * math stays uniform across all media types (every item = one photo duration).
     */
    public Path extractFirstFrame(Path videoPath) {
        String fileName = videoPath.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        Path outputPath = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "frame_" + baseName + ".png"
        );

        String[] cmd = {
            ffmpegPath, "-y",
            "-i", videoPath.toString(),
            "-vf", "select=eq(n\\,0)",  // select only the first frame (n=0)
            "-vframes", "1",
            outputPath.toString()
        };

        System.out.println("🖼️  Extrayendo primer frame de: " + fileName);
        String result = commandExecution(cmd);

        if (result != null && outputPath.toFile().exists()) {
            System.out.println("   ✅ Frame extraído: " + outputPath);
            return outputPath;
        } else {
            System.err.println("   ❌ Error extrayendo frame");
            return null;
        }
    }
}