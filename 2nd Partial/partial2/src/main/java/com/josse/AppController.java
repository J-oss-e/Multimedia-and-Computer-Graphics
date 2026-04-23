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

    public AppController(String apiKey, String ffmpegPath) {
        this.apiClient       = new APIClient(apiKey);
        this.ffmpegProcessor = new FFmpegProcessor(ffmpegPath);
        this.mapGenerator    = new MapGenerator();
        this.scaleCalculator = new ScaleCalculator();
        this.allMedia        = new ArrayList<>();
        this.videoAssembler  = null;
        this.essenceImage    = null;
        this.map             = null;
        this.audioNarration = null;
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
     * Genera el contenido AI: imagen de esencia y frase inspiracional.
     * Requiere que ya se hayan agregado medios con GPS.
     */
    public void generateAIContent() {
        if (allMedia.isEmpty()) {
            System.err.println("No hay media cargada.");
            return;
        }

        // Construir descripción SOLO de medios con GPS válido
        StringBuilder desc = new StringBuilder();
        StringBuilder audioDesc = new StringBuilder();
        
        for (VisualMedia m : allMedia) {
            // ⚠️ FILTRAR medios sin GPS
            if (m.getLatitude() == 0.0 && m.getLongitude() == 0.0) {
                System.out.println("⚠️  Saltando " + m.getName() + " (sin GPS)");
                continue;
            }
            
            String loc = mapGenerator.getLocationName(m.getLatitude(), m.getLongitude());
            desc.append(loc).append(", ");
            audioDesc.append("At ").append(loc)
                    .append(" on ").append(m.getDate() != null ? m.getDate().toLocalDate() : "an unknown date")
                    .append(". ");
        }

        // Validar que haya al menos un lugar válido
        if (desc.length() == 0) {
            System.err.println("❌ No hay medios con GPS válido para generar contenido AI");
            return;
        }

        System.out.println("📝 Descripción de lugares: " + desc.toString());

        // Generar imagen de esencia
        System.out.println("🎨 Generando imagen de esencia con DALL-E...");
        Path essenceImg = apiClient.generateEssenceImage(desc.toString());
        
        if (essenceImg != null && essenceImg.toFile().exists()) {
            this.essenceImage = essenceImg;
            System.out.println("   ✅ Imagen de esencia: " + essenceImg);
        } else {
            System.err.println("   ❌ Error generando imagen de esencia");
        }

        // Generar descripción narrativa para el audio
        System.out.println("🎙️ Generando guion de audio...");
        String audioScript = apiClient.generateAudioDescription(audioDesc.toString());
        System.out.println("   Guion: " + audioScript.substring(0, Math.min(100, audioScript.length())) + "...");

        // Convertir a audio con TTS
        System.out.println("🔊 Generando audio con TTS...");
        Path audioPath = Paths.get(System.getProperty("java.io.tmpdir"), "narration.mp3");
        Path generatedAudio = apiClient.generateAudio(audioScript, audioPath);
        
        if (generatedAudio != null && generatedAudio.toFile().exists()) {
            this.audioNarration = generatedAudio;
            System.out.println("   ✅ Audio generado: " + generatedAudio);
        } else {
            System.err.println("   ❌ Error generando audio");
        }

        // Generar frase inspiracional
        System.out.println("💭 Generando frase inspiracional...");
        String phrase = apiClient.generatePhrase(desc.toString());
        System.out.println("   Frase: " + phrase);

        this.map = Paths.get(System.getProperty("java.io.tmpdir"), "phrase.txt");
        try {
            java.nio.file.Files.writeString(this.map, phrase);
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    /**
     * Genera el mapa con los pines del primer y último elemento (por fecha).
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

        // Generar mapa base
        Path mapBase = mapGenerator.generateMap(
            first.getLatitude(), first.getLongitude(),
            last.getLatitude(),  last.getLongitude()
        );

        // Leer frase guardada y superponerla
        String phrase = "A journey worth remembering.";
        if (this.map != null && this.map.toFile().exists()) {
            try { phrase = java.nio.file.Files.readString(this.map); }
            catch (Exception e) { e.printStackTrace(); }
        }

        this.map = mapGenerator.addPhraseToMap(phrase, mapBase);
        System.out.println("Mapa generado en: " + this.map);
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