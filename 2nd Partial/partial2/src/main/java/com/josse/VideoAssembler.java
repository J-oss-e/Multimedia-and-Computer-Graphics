package com.josse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the final video from all collected media, the AI essence image,
 * the map outro, and the TTS narration.
 * Output order: essenceImage → user media (chronological) → map with phrase.
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
        // Defensive copy so changes to the caller's list don't affect assembly.
        this.allMedia       = new ArrayList<>(allMedia);
        this.map            = map;
        this.essenceImage   = essenceImage;
        this.audioNarration = audioNarration;
        this.ffmpeg         = ffmpeg;
    }

    /**
     * Sorts media oldest-first. Items with no date go to the end so they
     * don't displace dated content from its correct chronological position.
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
     * Full assembly pipeline:
     * 1. Measure audio duration to calculate per-slide display time.
     * 2. Order media by date.
     * 3. Convert video clips to their first frame — this keeps all segments as
     *    equal-length stills so photo duration math stays uniform across the whole list.
     * 4. Prepend essence image, append map outro.
     * 5. Clamp per-photo duration to [2, 8] s — prevents illegibly fast slides
     *    or excessively slow pacing when photo count and audio length are mismatched.
     * 6. Scale every item to 1080x1920.
     * 7. Concatenate into a silent video.
     * 8. Normalize audio and mix in with -shortest so the output ends with whichever
     *    stream finishes first (avoids a silent tail if the slideshow outlasts the narration).
     */
    public Path generateFinalVideo(Path outputPath) {
        double audioDuration = 0;
        if (audioNarration != null && audioNarration.toFile().exists()) {
            audioDuration = getAudioDuration(audioNarration);
            System.out.println("📏 Duración del audio: " + audioDuration + " segundos");
        }

        List<VisualMedia> ordered = orderMedia(allMedia);

        // Videos are reduced to their first frame so every item in the final list
        // is a still image — this lets a single photoDuration value govern the whole video.
        System.out.println("\n  Convirtiendo videos a frames...");
        List<VisualMedia> photosOnly = new ArrayList<>();
        for (VisualMedia m : ordered) {
            if (m.getType() == dataType.VIDEO) {
                Path frame = ffmpeg.extractFirstFrame(m.getPath());
                if (frame != null) {
                    Photo framePhoto = new Photo();
                    framePhoto.path      = frame;
                    framePhoto.type      = dataType.PHOTO;
                    framePhoto.name      = m.getName() + "_frame";
                    framePhoto.date      = m.getDate();
                    framePhoto.latitude  = m.getLatitude();
                    framePhoto.longitude = m.getLongitude();
                    photosOnly.add(framePhoto);
                } else {
                    System.err.println("⚠️  No se pudo extraer frame de: " + m.getName());
                }
            } else {
                photosOnly.add(m);
            }
        }

        List<VisualMedia> finalList = new ArrayList<>();

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

        if (map != null && map.toFile().exists()) {
            System.out.println("✅ Agregando mapa con frase al final");
            Photo outro = new Photo();
            outro.path = map;
            outro.type = dataType.PHOTO;
            finalList.add(outro);
        } else {
            System.err.println("⚠️  Mapa no disponible");
        }

        int numPhotos = finalList.size();
        double photoDuration = 3.0; // fallback when no audio is available
        if (audioDuration > 0 && numPhotos > 0) {
            photoDuration = audioDuration / numPhotos;
            // Clamp: < 2 s is too fast to read; > 8 s makes the video feel stagnant.
            photoDuration = Math.max(2.0, Math.min(8.0, photoDuration));
            System.out.println("📸 Duración calculada por foto: " + photoDuration + " segundos");
        }

        System.out.println("\n📐 Escalando todas las fotos...");
        for (VisualMedia m : finalList) {
            boolean ok = ffmpeg.scaleMedia(m, photoDuration);
            if (!ok) System.err.println("Error escalando: " + m.getName());
        }

        // Build silent video first; audio is merged in a separate step so that
        // normalization can be applied without re-encoding the video stream.
        Path videoNoAudio = Paths.get(
            System.getProperty("java.io.tmpdir"), "video_no_audio.mp4");

        System.out.println("\n🎬 Ensamblando video (sin audio)...");
        boolean ok = ffmpeg.assembleVideoSimple(videoNoAudio, finalList);

        if (!ok) {
            System.err.println("❌ Error ensamblando video");
            return null;
        }

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

        Path audioNormalized = Paths.get(
            System.getProperty("java.io.tmpdir"), "audio_normalized.mp3");

        System.out.println("\n🔊 Normalizando audio según estándares YouTube...");
        System.out.println("   Target: -15 LUFS, -1.5 dBTP, 7 LU LRA");
        boolean audioOk = ffmpeg.normalizeAudio(audioNarration, audioNormalized);

        if (!audioOk) {
            // Normalization failed — use the raw TTS audio rather than aborting.
            System.err.println("⚠️  Error normalizando audio, usando original");
            audioNormalized = audioNarration;
        }

        System.out.println("\n🎵 Combinando video con audio...");
        // -shortest ends the output at whichever stream ends first, preventing
        // a silent black tail if the video slideshow is longer than the narration.
        String[] combineCmd = {
            "ffmpeg", "-y",
            "-i", videoNoAudio.toString(),
            "-i", audioNormalized.toString(),
            "-c:v", "copy",     // copy video stream — no re-encode needed
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

    /** Reads audio duration via ffprobe. Returns 0 if the file can't be probed. */
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
                int end   = json.indexOf("\"", start);
                return Double.parseDouble(json.substring(start, end));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}