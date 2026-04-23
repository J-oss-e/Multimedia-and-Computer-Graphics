package com.josse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ordena los medios por fecha y coordina el ensamblado del video final.
 * El orden del video es: essenceImage → media ordenada → mapa con frase.
 */
public class VideoAssembler {

    private List<VisualMedia> allMedia;
    private Path map;
    private Path essenceImage;
    private final FFmpegProcessor ffmpeg;

    public VideoAssembler(List<VisualMedia> allMedia, Path map,
                          Path essenceImage, FFmpegProcessor ffmpeg) {
        this.allMedia     = new ArrayList<>(allMedia);
        this.map          = map;
        this.essenceImage = essenceImage;
        this.ffmpeg       = ffmpeg;
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
     * añade intro (essenceImage) y outro (mapa), y ensambla todo.
     */
    public Path generateFinalVideo(Path outputPath) {
        // 1. Escalar todos los medios
        for (VisualMedia m : allMedia) {
            boolean ok = ffmpeg.scaleMedia(m);
            if (!ok) System.err.println("Error escalando: " + m.getName());
        }

        // 2. Ordenar por fecha
        List<VisualMedia> ordered = orderMedia(allMedia);

        // 3. Construir lista completa: [essenceImage, ...media, map]
        List<VisualMedia> finalList = new ArrayList<>();

        // Agregar imagen de esencia como Photo temporal
        if (essenceImage != null) {
            Photo intro = new Photo();
            intro.path = essenceImage;
            ffmpeg.scaleMedia(intro);
            finalList.add(intro);
        }

        finalList.addAll(ordered);

        // Agregar mapa como Photo temporal
        if (map != null) {
            Photo outro = new Photo();
            outro.path = map;
            ffmpeg.scaleMedia(outro);
            finalList.add(outro);
        }

        // 4. Ensamblar
        boolean ok = ffmpeg.assembleVideo(outputPath, finalList);
        return ok ? outputPath : null;
    }
}