package com.josse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Controlador principal. Coordina todos los componentes del sistema.
 * Punto de entrada para la lógica de negocio.
 */
public class AppController {

    private final APIClient       apiClient;
    private final FFmpegProcessor ffmpegProcessor;
    private final MapGenerator    mapGenerator;
    private final ScaleCalculator scaleCalculator;
    private VideoAssembler        videoAssembler;
    private final List<VisualMedia> allMedia;
    private Path essenceImage;
    private Path map;
    private Path audioNarration;
    private Path phraseFile; // separado de map para evitar colisión de rutas

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
     * Agrega un archivo de media al proyecto.
     * Detecta el tipo por extensión y extrae su metadata.
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
     * Genera el contenido AI: imagen de esencia, audio TTS y frase inspiracional.
     * Requiere que ya se hayan agregado medios con GPS.
     */
    public void generateAIContent() {
        if (allMedia.isEmpty()) {
            System.err.println("No hay media cargada.");
            return;
        }

        // Construir descripción SOLO de medios con GPS válido
        StringBuilder desc      = new StringBuilder();
        StringBuilder audioDesc = new StringBuilder();
        
        for (VisualMedia m : allMedia) {
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

        // --- Imagen de esencia ---
        System.out.println("Generando imagen de esencia con DALL-E...");
        Path essenceImg = apiClient.generateEssenceImage(desc.toString());
        if (essenceImg != null && essenceImg.toFile().exists()) {
            this.essenceImage = essenceImg;
            System.out.println("   Imagen de esencia: " + essenceImg);
        } else {
            System.err.println("   Error generando imagen de esencia");
        }

        // --- Audio TTS ---
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

        // --- Frase inspiracional (se guarda en phraseFile, NO en map) ---
        System.out.println("Generando frase inspiracional...");
        String phrase = apiClient.generatePhrase(desc.toString());
        System.out.println("   Frase: " + phrase);

        this.phraseFile = Paths.get(System.getProperty("java.io.tmpdir"), "phrase.txt");
        try {
            java.nio.file.Files.writeString(this.phraseFile, phrase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Genera el mapa con los pines del primer y último elemento (por fecha)
     * y superpone la frase inspiracional generada por AI.
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

        // Mapa base con pines
        Path mapBase = mapGenerator.generateMap(
            first.getLatitude(), first.getLongitude(),
            last.getLatitude(),  last.getLongitude()
        );

        if (mapBase == null || !mapBase.toFile().exists()) {
            System.err.println("Error: no se pudo generar el mapa base.");
            return;
        }

        // Leer frase desde phraseFile (independiente de this.map)
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

        // Superponer frase al mapa -> this.map
        this.map = mapGenerator.addPhraseToMap(phrase, mapBase);
        if (this.map != null) {
            System.out.println("Mapa generado en: " + this.map);
        } else {
            System.err.println("Error superponiendo frase al mapa.");
        }
    }

    /**
     * Ejecuta el flujo completo y retorna la ruta del video final.
     */
    public Path createVideo(Path outputPath) {
        this.videoAssembler = new VideoAssembler(
            allMedia, map, essenceImage, audioNarration, ffmpegProcessor);
        return videoAssembler.generateFinalVideo(outputPath);
    }

    public List<VisualMedia> getAllMedia() { return allMedia; }
}