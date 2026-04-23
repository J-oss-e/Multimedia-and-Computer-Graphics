package com.josse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Wrapper sobre ffmpeg/ffprobe. Ejecuta comandos de procesamiento de media.
 * Todos los métodos retornan boolean para indicar éxito/fallo.
 */
public class FFmpegProcessor {

    private String ffmpegPath;
    private static final int TARGET_W = (int) ScaleCalculator.getWidth();   // 1080
    private static final int TARGET_H = (int) ScaleCalculator.getHeight();  // 1920

    public FFmpegProcessor() { this.ffmpegPath = "ffmpeg"; }

    public FFmpegProcessor(String ffmpegPath) { this.ffmpegPath = ffmpegPath; }

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
            process.waitFor();
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
     * Usa scale+pad: escala al máximo posible y rellena con negro.
     * Guarda el resultado en /tmp/ y asigna scaledPath al media.
     */
    public boolean scaleMedia(VisualMedia media) {
        String inputPath  = media.getPath().toString();
        String outputPath = System.getProperty("java.io.tmpdir") +
                            "/scaled_" + media.getPath().getFileName();

        // Filtro: escala manteniendo ratio, luego hace pad hasta 1080x1920
        String filter = String.format(
            "scale=%d:%d:force_original_aspect_ratio=decrease," +
            "pad=%d:%d:(ow-iw)/2:(oh-ih)/2:black",
            TARGET_W, TARGET_H, TARGET_W, TARGET_H
        );

        String[] cmd;
        if (media.getType() == dataType.PHOTO) {
            // Para fotos: crear un video de 3 segundos
            cmd = new String[]{
                ffmpegPath, "-y", "-loop", "1", "-i", inputPath,
                "-vf", filter,
                "-t", "3", "-r", "30",
                "-c:v", "libx264", "-pix_fmt", "yuv420p",
                outputPath.replace(".", "_scaled.")
            };
            outputPath = outputPath.replace(".", "_scaled.") ;
        } else {
            cmd = new String[]{
                ffmpegPath, "-y", "-i", inputPath,
                "-vf", filter,
                "-c:v", "libx264", "-c:a", "aac",
                outputPath
            };
        }

        String result = commandExecution(cmd);
        boolean success = result != null && !result.contains("Error");
        if (success) {
            media.setScaledPath(Paths.get(outputPath));
        }
        return success;
    }

    /**
     * Normaliza el audio de un archivo según estándares de YouTube.
     * Target: -15 LUFS, true peak -1.5 dBTP, LRA 7 LU.
     */
    public boolean normalizeAudio(Path audio, Path output) {
        // Paso 1: análisis
        String[] analyzeCmd = {
            ffmpegPath, "-i", audio.toString(),
            "-af", "loudnorm=I=-15:TP=-1.5:LRA=7:print_format=json",
            "-f", "null", "-"
        };
        String analysis = commandExecution(analyzeCmd);
        if (analysis == null) return false;

        // Paso 2: normalización con los valores medidos (two-pass)
        // Para simplicidad, usamos loudnorm en un solo paso
        String[] normalizeCmd = {
            ffmpegPath, "-y", "-i", audio.toString(),
            "-af", "loudnorm=I=-15:TP=-1.5:LRA=7",
            output.toString()
        };
        String result = commandExecution(normalizeCmd);
        return result != null && !result.contains("Error");
    }

    /**
     * Ensambla todos los medios escalados en un video final usando
     * el protocolo de concatenación de ffmpeg.
     */
    public boolean assembleVideo(Path finalPath, List<VisualMedia> allMedia) {
        // Crear archivo de lista para ffmpeg concat
        StringBuilder concatList = new StringBuilder();
        for (VisualMedia m : allMedia) {
            Path p = (m.getScaledPath() != null) ? m.getScaledPath() : m.getPath();
            concatList.append("file '").append(p.toAbsolutePath()).append("'\n");
        }

        // Escribir el concat file en tmp
        Path concatFile = Paths.get(System.getProperty("java.io.tmpdir"), "concat_list.txt");
        try {
            java.nio.file.Files.writeString(concatFile, concatList.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        String[] cmd = {
            ffmpegPath, "-y",
            "-f", "concat", "-safe", "0",
            "-i", concatFile.toString(),
            "-c:v", "libx264", "-c:a", "aac",
            "-movflags", "+faststart",
            finalPath.toString()
        };

        String result = commandExecution(cmd);
        return result != null && !result.contains("Error");
    }
}