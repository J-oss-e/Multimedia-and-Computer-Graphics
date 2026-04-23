package com.josse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ordena los medios por fecha y coordina el ensamblado del video final.
 * El orden del video es: essenceImage → media ordenada → mapa con frase.
 * Añade audio TTS normalizado según estándares de YouTube.
 */
public class VideoAssembler {

    private List<VisualMedia> allMedia;
    private Path map;
    private Path essenceImage;
    private Path audioNarration;
    private final FFmpegProcessor ffmpeg;

    public VideoAssembler(List<VisualMedia> allMedia, Path map,
                          Path essenceImage, Path audioNarration,
                          FFmpegProcessor ffmpeg) {
        this.allMedia       = new ArrayList<>(allMedia);
        this.map            = map;
        this.essenceImage   = essenceImage;
        this.audioNarration = audioNarration;
        this.ffmpeg         = ffmpeg;
    }

    /**
     * Ordena los medios de más antiguo a más reciente.
     * Elementos sin fecha van al final.
     */
    public List<VisualMedia> orderMedia(List<VisualMedia> toOrder) {
        List<VisualMedia> ordered = new ArrayList<>(toOrder);
        ordered.sort(Comparator.comparing(
            VisualMedia::getDate,
            Comparator.nullsLast(Comparator.naturalOrder())
        ));
        return ordered;
    }

    /**
     * Genera el video final: escala medios, los ordena por fecha,
     * añade intro (essenceImage) y outro (mapa), ensambla video,
     * normaliza audio y combina todo.
     */
    public Path generateFinalVideo(Path outputPath) {
        // 1. Calcular duración necesaria del video basada en el audio
        double audioDuration = 0;
        if (audioNarration != null && audioNarration.toFile().exists()) {
            audioDuration = getAudioDuration(audioNarration);
            System.out.println("📏 Duración del audio: " + audioDuration + " segundos");
        }

        // 2. Ordenar medios por fecha
        List<VisualMedia> ordered = orderMedia(allMedia);

        // 3. CONVERTIR VIDEOS A FOTOS (primer frame)
        System.out.println("\n  Convirtiendo videos a frames...");
        List<VisualMedia> photosOnly = new ArrayList<>();
        for (VisualMedia m : ordered) {
            if (m.getType() == dataType.VIDEO) {
                Path frame = ffmpeg.extractFirstFrame(m.getPath());
                if (frame != null) {
                    Photo framePhoto = new Photo();
                    framePhoto.path = frame;
                    framePhoto.type = dataType.PHOTO;
                    framePhoto.name = m.getName() + "_frame";
                    framePhoto.date = m.getDate();
                    framePhoto.latitude = m.getLatitude();
                    framePhoto.longitude = m.getLongitude();
                    photosOnly.add(framePhoto);
                } else {
                    System.err.println("⚠️  No se pudo extraer frame de: " + m.getName());
                }
            } else {
                photosOnly.add(m); // Ya es foto, agregar directo
            }
        }

        // 4. Construir lista completa: [essenceImage, ...photosOnly, map]
        List<VisualMedia> finalList = new ArrayList<>();

        // Agregar imagen de esencia
        if (essenceImage != null && essenceImage.toFile().exists()) {
            System.out.println("✅ Agregando imagen de esencia al inicio");
            Photo intro = new Photo();
            intro.path = essenceImage;
            intro.type = dataType.PHOTO;
            finalList.add(intro);
        } else {
            System.err.println("⚠️  Imagen de esencia no disponible");
        }

        finalList.addAll(photosOnly);

        // Agregar mapa con frase
        if (map != null && map.toFile().exists()) {
            System.out.println("✅ Agregando mapa con frase al final");
            Photo outro = new Photo();
            outro.path = map;
            outro.type = dataType.PHOTO;
            finalList.add(outro);
        } else {
            System.err.println("⚠️  Mapa no disponible");
        }

        // 5. Calcular duración por foto (ahora TODO es foto)
        int numPhotos = finalList.size();
        
        double photoDuration = 3.0; // Default
        if (audioDuration > 0 && numPhotos > 0) {
            photoDuration = audioDuration / numPhotos;
            photoDuration = Math.max(2.0, Math.min(8.0, photoDuration));
            System.out.println("📸 Duración calculada por foto: " + photoDuration + " segundos");
        }

        // 6. Escalar TODAS las fotos con la duración correcta
        System.out.println("\n📐 Escalando todas las fotos...");
        for (VisualMedia m : finalList) {
            boolean ok = ffmpeg.scaleMedia(m, photoDuration);
            if (!ok) System.err.println("Error escalando: " + m.getName());
        }

        // 7. Ensamblar video SIN audio
        Path videoNoAudio = Paths.get(
            System.getProperty("java.io.tmpdir"), "video_no_audio.mp4");
        
        System.out.println("\n🎬 Ensamblando video (sin audio)...");
        boolean ok = ffmpeg.assembleVideoSimple(videoNoAudio, finalList);
        
        if (!ok) {
            System.err.println("❌ Error ensamblando video");
            return null;
        }

        // 8. Si no hay audio, retornar el video sin audio
        if (audioNarration == null || !audioNarration.toFile().exists()) {
            System.err.println("⚠️  Audio de narración no disponible");
            System.out.println("   Retornando video sin audio...");
            try {
                java.nio.file.Files.copy(videoNoAudio, outputPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return outputPath;
            } catch (Exception e) {
                e.printStackTrace();
                return videoNoAudio;
            }
        }

        // 9. Normalizar audio
        Path audioNormalized = Paths.get(
            System.getProperty("java.io.tmpdir"), "audio_normalized.mp3");
        
        System.out.println("\n🔊 Normalizando audio según estándares YouTube...");
        System.out.println("   Target: -15 LUFS, -1.5 dBTP, 7 LU LRA");
        boolean audioOk = ffmpeg.normalizeAudio(audioNarration, audioNormalized);
        
        if (!audioOk) {
            System.err.println("⚠️  Error normalizando audio, usando original");
            audioNormalized = audioNarration;
        }

        // 10. Combinar video + audio normalizado
        System.out.println("\n🎵 Combinando video con audio...");
        String[] combineCmd = {
            "ffmpeg", "-y",
            "-i", videoNoAudio.toString(),
            "-i", audioNormalized.toString(),
            "-c:v", "copy",
            "-c:a", "aac",
            "-b:a", "192k",
            "-shortest",
            outputPath.toString()
        };

        try {
            System.out.println("   Ejecutando: " + String.join(" ", combineCmd));
            ProcessBuilder pb = new ProcessBuilder(combineCmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("error") || line.contains("Error")) {
                    System.err.println("   " + line);
                }
            }
            
            int exitCode = p.waitFor();
            
            if (exitCode == 0 && outputPath.toFile().exists()) {
                System.out.println("   ✅ Video final con audio creado exitosamente");
                return outputPath;
            } else {
                System.err.println("   ❌ Error combinando video y audio (exit code: " + exitCode + ")");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene la duración en segundos de un archivo de audio usando ffprobe.
     */
    private double getAudioDuration(Path audioPath) {
        try {
            String[] cmd = {
                "ffprobe", "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                audioPath.toString()
            };
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            p.waitFor();
            
            String json = output.toString();
            int idx = json.indexOf("\"duration\"");
            if (idx != -1) {
                int start = json.indexOf("\"", idx + 11) + 1;
                int end = json.indexOf("\"", start);
                return Double.parseDouble(json.substring(start, end));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}