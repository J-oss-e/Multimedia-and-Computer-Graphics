package com.josse;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

/**
 * Genera mapas con los puntos GPS del primer y último elemento.
 * Usa Nominatim (OpenStreetMap) para reverse geocoding.
 * Dibuja el mapa con Graphics2D sobre una imagen base.
 */
public class MapGenerator {

    private static final String NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s";
    private final HttpClient httpClient;

    public MapGenerator() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Obtiene el nombre legible de una ubicación por coordenadas.
     * Usa la API de Nominatim (OpenStreetMap, gratuita).
     */
    public String getLocationName(double lat, double lon) {
        try {
            String url = String.format(NOMINATIM_URL, lat, lon);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "VideoCreatorApp/1.0")
                .GET().build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Parsear el campo "display_name" del JSON
            String body = response.body();
            int idx = body.indexOf("\"display_name\"");
            if (idx != -1) {
                int start = body.indexOf("\"", idx + 15) + 1;
                int end   = body.indexOf("\"", start);
                return body.substring(start, end);
            }
        } catch (Exception e) {
            System.err.println("Error en geocoding: " + e.getMessage());
        }
        return "Ubicación desconocida";
    }

    /**
     * Genera un mapa 1080x1920 con dos pines (inicio y fin).
     * Proyección equirectangular simple sobre el área delimitada por las coords.
     */
    public Path generateMap(double lat1, double lon1, double lat2, double lon2) {
        int W = 1080, H = 1920;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        // Fondo tipo mapa (azul oceano + verde tierra — simplificado)
        g.setColor(new Color(170, 211, 223)); // agua
        g.fillRect(0, 0, W, H);

        // Márgenes para las coordenadas
        double margin = 5.0;
        double minLat = Math.min(lat1, lat2) - margin;
        double maxLat = Math.max(lat1, lat2) + margin;
        double minLon = Math.min(lon1, lon2) - margin;
        double maxLon = Math.max(lon1, lon2) + margin;

        // Función de proyección a píxeles
        int x1 = lonToX(lon1, minLon, maxLon, W);
        int y1 = latToY(lat1, minLat, maxLat, H);
        int x2 = lonToX(lon2, minLon, maxLon, W);
        int y2 = latToY(lat2, minLat, maxLat, H);

        // Línea entre los dos puntos
        g.setColor(new Color(255, 100, 0));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{10, 10}, 0));
        g.drawLine(x1, y1, x2, y2);

        // Pin de inicio (verde)
        drawPin(g, x1, y1, new Color(0, 180, 0), "Inicio");

        // Pin de fin (rojo)
        drawPin(g, x2, y2, new Color(220, 0, 0), "Fin");

        g.dispose();

        Path output = Paths.get(System.getProperty("java.io.tmpdir"), "map.png");
        try {
            ImageIO.write(img, "PNG", output.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return output;
    }

    /** Superpone una frase inspiracional sobre el mapa. */
    public Path addPhraseToMap(String phrase, Path mapPath) {
        try {
            BufferedImage img = ImageIO.read(mapPath.toFile());
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = img.getWidth();
            int H = img.getHeight();

            // Caja semitransparente en la parte inferior
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(40, H - 320, W - 80, 260, 20, 20);

            // Texto de la frase
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.ITALIC, 42));
            drawWrappedText(g, phrase, 60, H - 290, W - 120, 52);

            g.dispose();

            Path output = Paths.get(System.getProperty("java.io.tmpdir"), "map_phrase.png");
            ImageIO.write(img, "PNG", output.toFile());
            return output;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- Helpers privados ---

    private int lonToX(double lon, double minLon, double maxLon, int W) {
        return (int) ((lon - minLon) / (maxLon - minLon) * W);
    }

    private int latToY(double lat, double minLat, double maxLat, int H) {
        // Invertido: lat mayor = arriba
        return (int) ((1 - (lat - minLat) / (maxLat - minLat)) * H);
    }

    private void drawPin(Graphics2D g, int x, int y, Color color, String label) {
        int r = 18;
        g.setColor(Color.WHITE);
        g.fillOval(x - r - 2, y - r - 2, (r + 2) * 2, (r + 2) * 2);
        g.setColor(color);
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x - fm.stringWidth(label) / 2, y + 5);
        // Sombra del pin
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(x - r, y + r, r * 2, 8);
    }

    private void drawWrappedText(Graphics2D g, String text,
                                  int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (fm.stringWidth(line + word) > maxWidth) {
                g.drawString(line.toString().trim(), x, y);
                y += lineHeight;
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (!line.isEmpty()) g.drawString(line.toString().trim(), x, y);
    }
}