package com.josse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Facade that coordinates all pipeline components in order:
 * addMedia → generateAIContent → generateMap → createVideo.
 * Each step is independent so the caller (main) can inspect intermediate
 * results (e.g., print metadata) between stages.
 */
public class AppController {

    private final APIClient         apiClient;
    private final FFmpegProcessor   ffmpegProcessor;
    private final MapGenerator      mapGenerator;
    private final ScaleCalculator   scaleCalculator;
    private VideoAssembler          videoAssembler;
    private final List<VisualMedia> allMedia;
    private Path essenceImage;
    private Path map;
    private Path audioNarration;
    // phraseFile is kept separate from map so that generateAIContent() can write
    // the phrase before generateMap() reads it, without the two paths colliding.
    private Path phraseFile;

    public AppController(String apiKey, String ffmpegPath) {
        this.apiClient       = new APIClient(apiKey);
        this.ffmpegProcessor = new FFmpegProcessor(ffmpegPath);
        this.mapGenerator    = new MapGenerator();
        this.scaleCalculator = new ScaleCalculator();
        this.allMedia        = new ArrayList<>();
        this.videoAssembler  = null;
        this.essenceImage    = null;
        this.map             = null;
        this.audioNarration  = null;
        this.phraseFile      = null;
    }

    /**
     * Detects media type by file extension, extracts metadata, and adds it to the list.
     * Metadata extraction happens here so the list is always fully populated
     * before any AI or map generation step begins.
     */
    public void addMedia(Path filePath) {
        String fileName  = filePath.getFileName().toString();
        String extension = fileName.substring(
            fileName.lastIndexOf(".") + 1).toLowerCase();

        VisualMedia media;

        switch (extension) {
            case "jpg": case "jpeg": case "png": case "webp":
                media = new Photo();
                break;
            case "mp4": case "mov": case "avi":
                media = new Video();
                break;
            default:
                System.out.println("Formato no soportado: " + extension);
                return;
        }

        media.path = filePath;
        media.extractMetadata(ffmpegProcessor);
        allMedia.add(media);
        System.out.println("Media agregada: " + fileName +
            " | " + media.getWidth() + "x" + media.getHeight() +
            " | " + media.getDate());
    }

    /**
     * Calls OpenAI to generate the DALL-E essence image, TTS narration audio,
     * and inspirational phrase. Skips any media item whose GPS is exactly 0,0
     * because that coordinate ("null island") indicates missing GPS data, not
     * a real location in the Gulf of Guinea.
     */
    public void generateAIContent() {
        if (allMedia.isEmpty()) {
            System.err.println("No hay media cargada.");
            return;
        }

        StringBuilder desc      = new StringBuilder();
        StringBuilder audioDesc = new StringBuilder();

        for (VisualMedia m : allMedia) {
            // 0.0 / 0.0 is the default when no GPS was found — skip to avoid bad AI prompts.
            if (m.getLatitude() == 0.0 && m.getLongitude() == 0.0) {
                System.out.println("Saltando " + m.getName() + " (sin GPS)");
                continue;
            }
            String loc = mapGenerator.getLocationName(m.getLatitude(), m.getLongitude());
            desc.append(loc).append(", ");
            audioDesc.append("At ").append(loc)
                     .append(" on ").append(m.getDate() != null ? m.getDate().toLocalDate() : "an unknown date")
                     .append(". ");
        }

        if (desc.length() == 0) {
            System.err.println("No hay medios con GPS válido para generar contenido AI");
            return;
        }

        System.out.println("Descripcion de lugares: " + desc.toString());

        System.out.println("Generando imagen de esencia con DALL-E...");
        Path essenceImg = apiClient.generateEssenceImage(desc.toString());
        if (essenceImg != null && essenceImg.toFile().exists()) {
            this.essenceImage = essenceImg;
            System.out.println("   Imagen de esencia: " + essenceImg);
        } else {
            System.err.println("   Error generando imagen de esencia");
        }

        System.out.println("Generando guion de audio...");
        String audioScript = apiClient.generateAudioDescription(audioDesc.toString());
        System.out.println("   Guion: " + audioScript.substring(0, Math.min(100, audioScript.length())) + "...");

        System.out.println("Generando audio con TTS...");
        Path audioPath      = Paths.get(System.getProperty("java.io.tmpdir"), "narration.mp3");
        Path generatedAudio = apiClient.generateAudio(audioScript, audioPath);
        if (generatedAudio != null && generatedAudio.toFile().exists()) {
            this.audioNarration = generatedAudio;
            System.out.println("   Audio generado: " + generatedAudio);
        } else {
            System.err.println("   Error generando audio");
        }

        System.out.println("Generando frase inspiracional...");
        String phrase = apiClient.generatePhrase(desc.toString());
        System.out.println("   Frase: " + phrase);

        // Persist phrase to a temp file so generateMap() can read it independently.
        this.phraseFile = Paths.get(System.getProperty("java.io.tmpdir"), "phrase.txt");
        try {
            java.nio.file.Files.writeString(this.phraseFile, phrase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates the map image using only the chronologically first and last media items.
     * The phrase written by generateAIContent() is overlaid on the map after download.
     */
    public void generateMap() {
        if (allMedia.size() < 2) {
            System.err.println("Se necesitan al menos 2 medios con GPS.");
            return;
        }

        List<VisualMedia> sorted = new ArrayList<>(allMedia);
        sorted.sort(Comparator.comparing(VisualMedia::getDate,
            Comparator.nullsLast(Comparator.naturalOrder())));

        VisualMedia first = sorted.get(0);
        VisualMedia last  = sorted.get(sorted.size() - 1);

        Path mapBase = mapGenerator.generateMap(
            first.getLatitude(), first.getLongitude(),
            last.getLatitude(),  last.getLongitude()
        );

        if (mapBase == null || !mapBase.toFile().exists()) {
            System.err.println("Error: no se pudo generar el mapa base.");
            return;
        }

        // Fall back to a generic phrase if AI generation was skipped or failed.
        String phrase = "A journey worth remembering.";
        if (this.phraseFile != null && this.phraseFile.toFile().exists()) {
            try {
                phrase = java.nio.file.Files.readString(this.phraseFile).trim();
                System.out.println("   Frase leída: " + phrase);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("   phraseFile no encontrado, usando frase por defecto.");
        }

        this.map = mapGenerator.addPhraseToMap(phrase, mapBase);
        if (this.map != null) {
            System.out.println("Mapa generado en: " + this.map);
        } else {
            System.err.println("Error superponiendo frase al mapa.");
        }
    }

    /** Assembles the final video; must be called after generateAIContent and generateMap. */
    public Path createVideo(Path outputPath) {
        this.videoAssembler = new VideoAssembler(
            allMedia, map, essenceImage, audioNarration, ffmpegProcessor);
        return videoAssembler.generateFinalVideo(outputPath);
    }

    public List<VisualMedia> getAllMedia() { return allMedia; }
}