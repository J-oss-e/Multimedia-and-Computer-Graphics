package com.josse;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
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
 * Descarga tiles reales de OpenStreetMap para el mapa base.
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
     * Usa tiles reales de OpenStreetMap Static Map API.
     */
    public Path generateMap(double lat1, double lon1, double lat2, double lon2) {
        int W = 1080, H = 1920;
        
        try {
            // Calcular el centro del mapa
            double centerLat = (lat1 + lat2) / 2;
            double centerLon = (lon1 + lon2) / 2;
            
            // Calcular zoom basado en la distancia entre puntos
            double latDiff = Math.abs(lat1 - lat2);
            double lonDiff = Math.abs(lon1 - lon2);
            double maxDiff = Math.max(latDiff, lonDiff);
            
            // Zoom apropiado: más distancia = menos zoom
            int zoom = maxDiff > 10 ? 4 : 
                       maxDiff > 5  ? 5 : 
                       maxDiff > 2  ? 6 : 
                       maxDiff > 1  ? 7 : 
                       maxDiff > 0.5 ? 8 : 9;
            
            System.out.println("Descargando mapa: centro=" + centerLat + "," + centerLon + 
                             " zoom=" + zoom);
            
            // Descargar mapa base de OpenStreetMap Static Map
            String mapUrl = String.format(
                "https://staticmap.openstreetmap.de/staticmap.php?" +
                "center=%f,%f&zoom=%d&size=%dx%d&maptype=mapnik",
                centerLat, centerLon, zoom, W, H
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mapUrl))
                .header("User-Agent", "VideoCreatorApp/1.0")
                .GET().build();
            
            HttpResponse<byte[]> response = 
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() != 200) {
                System.err.println("Error descargando mapa: HTTP " + response.statusCode());
                return generateFallbackMap(lat1, lon1, lat2, lon2);
            }
            
            // Cargar la imagen del mapa
            BufferedImage mapImage = ImageIO.read(
                new java.io.ByteArrayInputStream(response.body()));
            
            if (mapImage == null) {
                System.err.println("Imagen del mapa es null, usando fallback");
                return generateFallbackMap(lat1, lon1, lat2, lon2);
            }
            
            Graphics2D g = mapImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Convertir coordenadas geográficas a píxeles en el tile
            int x1 = lonToPixel(lon1, centerLon, zoom, W);
            int y1 = latToPixel(lat1, centerLat, zoom, H);
            int x2 = lonToPixel(lon2, centerLon, zoom, W);
            int y2 = latToPixel(lat2, centerLat, zoom, H);
            
            // Línea entre puntos con patrón de guiones
            g.setColor(new Color(255, 100, 0));
            g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, 
                        BasicStroke.JOIN_ROUND, 0, new float[]{15, 10}, 0));
            g.drawLine(x1, y1, x2, y2);
            
            // Pines
            drawPin(g, x1, y1, new Color(0, 180, 0), "Inicio");
            drawPin(g, x2, y2, new Color(220, 0, 0), "Fin");
            
            g.dispose();
            
            Path output = Paths.get(System.getProperty("java.io.tmpdir"), "map.png");
            ImageIO.write(mapImage, "PNG", output.toFile());
            System.out.println("Mapa generado exitosamente");
            return output;
            
        } catch (Exception e) {
            System.err.println("Error generando mapa con tiles: " + e.getMessage());
            e.printStackTrace();
            return generateFallbackMap(lat1, lon1, lat2, lon2);
        }
    }

    /**
     * Mapa de respaldo simple si falla la descarga de tiles.
     */
    private Path generateFallbackMap(double lat1, double lon1, double lat2, double lon2) {
        System.out.println("Usando mapa de respaldo simplificado");
        
        int W = 1080, H = 1920;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        // Gradiente de fondo (oceano)
        GradientPaint ocean = new GradientPaint(
            0, 0, new Color(170, 211, 223),
            0, H, new Color(120, 180, 200)
        );
        g.setPaint(ocean);
        g.fillRect(0, 0, W, H);

        // Tierra simplificada
        g.setColor(new Color(209, 219, 189));
        double margin = 8.0;
        double minLat = Math.min(lat1, lat2) - margin;
        double maxLat = Math.max(lat1, lat2) + margin;
        double minLon = Math.min(lon1, lon2) - margin;
        double maxLon = Math.max(lon1, lon2) + margin;

        // Proyección de puntos
        int x1 = lonToX(lon1, minLon, maxLon, W);
        int y1 = latToY(lat1, minLat, maxLat, H);
        int x2 = lonToX(lon2, minLon, maxLon, W);
        int y2 = latToY(lat2, minLat, maxLat, H);

        // Área de tierra alrededor de la ruta
        int[] xPoly = {
            x1 - 200, x1 + 200, x2 + 200, x2 - 200,
            (x1 + x2)/2 - 150, (x1 + x2)/2 + 150
        };
        int[] yPoly = {
            y1 - 300, y1 - 300, y2 + 300, y2 + 300,
            (y1 + y2)/2 - 200, (y1 + y2)/2 + 200
        };
        g.fillPolygon(xPoly, yPoly, 6);

        // Línea de ruta
        g.setColor(new Color(255, 100, 0));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, 
                    BasicStroke.JOIN_ROUND, 0, new float[]{15, 10}, 0));
        g.drawLine(x1, y1, x2, y2);

        // Pines
        drawPin(g, x1, y1, new Color(0, 180, 0), "Inicio");
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
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(40, H - 320, W - 80, 260, 20, 20);

            // Borde dorado sutil
            g.setColor(new Color(255, 215, 0, 100));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(40, H - 320, W - 80, 260, 20, 20);

            // Texto de la frase
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.ITALIC | Font.BOLD, 42));
            drawWrappedText(g, phrase, 60, H - 270, W - 120, 52);

            g.dispose();

            Path output = Paths.get(System.getProperty("java.io.tmpdir"), "map_phrase.png");
            ImageIO.write(img, "PNG", output.toFile());
            return output;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- Conversión de coordenadas geográficas a píxeles ---

    private int lonToPixel(double lon, double centerLon, int zoom, int width) {
        double scale = Math.pow(2, zoom);
        double lonDiff = lon - centerLon;
        return (int) (width / 2 + (lonDiff * scale * width / 360.0));
    }

    private int latToPixel(double lat, double centerLat, int zoom, int height) {
        double scale = Math.pow(2, zoom);
        double latDiff = centerLat - lat; // Invertido: Y crece hacia abajo
        return (int) (height / 2 + (latDiff * scale * height / 180.0));
    }

    // Proyección simple (para mapa de respaldo)
    private int lonToX(double lon, double minLon, double maxLon, int W) {
        return (int) ((lon - minLon) / (maxLon - minLon) * W);
    }

    private int latToY(double lat, double minLat, double maxLat, int H) {
        return (int) ((1 - (lat - minLat) / (maxLat - minLat)) * H);
    }

    // --- Helpers de dibujo ---

    private void drawPin(Graphics2D g, int x, int y, Color color, String label) {
        int r = 20;
        
        // Sombra del pin
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(x - r + 2, y + r - 5, r * 2, 10);
        
        // Borde blanco del pin
        g.setColor(Color.WHITE);
        g.fillOval(x - r - 3, y - r - 3, (r + 3) * 2, (r + 3) * 2);
        
        // Pin de color
        g.setColor(color);
        g.fillOval(x - r, y - r, r * 2, r * 2);
        
        // Label dentro del pin
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x - fm.stringWidth(label) / 2, y + 6);
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
        if (!line.isEmpty()) {
            g.drawString(line.toString().trim(), x, y);
        }
    }
}