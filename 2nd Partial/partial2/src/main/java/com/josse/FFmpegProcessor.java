package com.josse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Wrapper sobre ffmpeg/ffprobe. Ejecuta comandos de procesamiento de media.
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

    /** Ejecuta un comando y retorna su salida como String. */
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

    /** Extrae metadata del medio con ffprobe en formato JSON. */
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
     * Escala un medio a portrait (1080x1920) manteniendo aspect ratio.
     * Para fotos: crea un video de 3 segundos por defecto.
     * Para videos: escala el video completo.
     */
    public boolean scaleMedia(VisualMedia media) {
        return scaleMedia(media, 3.0); // Por defecto 3 segundos
    }

    /**
     * Escala un medio con duración personalizada (solo aplica a fotos).
     */
    public boolean scaleMedia(VisualMedia media, double photoDuration) {
        String inputPath  = media.getPath().toString();
        
        String fileName = media.getPath().getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String outputPath;
        
        if (media.getType() == dataType.PHOTO) {
            outputPath = System.getProperty("java.io.tmpdir") + 
                        "/scaled_" + baseName + ".mp4";
        } else {
            outputPath = System.getProperty("java.io.tmpdir") + 
                        "/scaled_" + fileName;
        }

        String filter = String.format(
            "scale=%d:%d:force_original_aspect_ratio=decrease," +
            "pad=%d:%d:(ow-iw)/2:(oh-ih)/2:black",
            TARGET_W, TARGET_H, TARGET_W, TARGET_H
        );

        String[] cmd;
        if (media.getType() == dataType.PHOTO) {
            cmd = new String[]{
                ffmpegPath, "-y", 
                "-loop", "1", 
                "-i", inputPath,
                "-vf", filter,
                "-t", String.valueOf(photoDuration), // ⚠️ Usar duración dinámica
                "-r", "30",
                "-c:v", "libx264", 
                "-pix_fmt", "yuv420p",
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
     * Normaliza el audio de un archivo según estándares de YouTube.
     * Target: -15 LUFS, true peak -1.5 dBTP, LRA 7 LU.
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
     * Ensambla video SIN duraciones custom (ya están embedidas en los archivos).
     */
    public boolean assembleVideoSimple(Path finalPath, List<VisualMedia> allMedia) {
        System.out.println("\n📦 Ensamblando video final...");
        
        // Crear archivo de lista SIMPLE (sin duraciones)
        StringBuilder concatList = new StringBuilder();
        for (VisualMedia m : allMedia) {
            Path p = (m.getScaledPath() != null) ? m.getScaledPath() : m.getPath();
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
     * Extrae el primer frame de un video como imagen PNG.
     * Retorna la ruta de la imagen extraída.
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
            "-vf", "select=eq(n\\,0)", // Primer frame
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