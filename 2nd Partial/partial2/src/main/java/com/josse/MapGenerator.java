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
 * Generates a 1080x1920 map image showing the route between two GPS points.
 * Attempts to download a real OpenStreetMap tile; falls back to a synthetic
 * painted map if the tile server is unreachable or returns an error.
 * Uses Nominatim (OpenStreetMap) for free reverse geocoding.
 */
public class MapGenerator {

    // Nominatim requires a descriptive User-Agent; anonymous requests may be blocked.
    private static final String NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s";
    private final HttpClient httpClient;

    public MapGenerator() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Returns a human-readable location name for the given coordinates.
     * Nominatim's "display_name" field contains the full address string.
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
     * Downloads a Mapnik tile centered between the two points and draws start/end pins.
     * Zoom is derived from the geographic span so both pins are visible in the frame.
     * Falls back to generateFallbackMap() if the tile server fails.
     */
    public Path generateMap(double lat1, double lon1, double lat2, double lon2) {
        int W = 1080, H = 1920;

        try {
            double centerLat = (lat1 + lat2) / 2;
            double centerLon = (lon1 + lon2) / 2;

            double latDiff = Math.abs(lat1 - lat2);
            double lonDiff = Math.abs(lon1 - lon2);
            double maxDiff = Math.max(latDiff, lonDiff);

            // Lower zoom = wider view; thresholds chosen so both pins fit comfortably.
            int zoom = maxDiff > 10 ? 4 :
                       maxDiff > 5  ? 5 :
                       maxDiff > 2  ? 6 :
                       maxDiff > 1  ? 7 :
                       maxDiff > 0.5 ? 8 : 9;

            System.out.println("Descargando mapa: centro=" + centerLat + "," + centerLon +
                             " zoom=" + zoom);

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

            BufferedImage mapImage = ImageIO.read(
                new java.io.ByteArrayInputStream(response.body()));

            if (mapImage == null) {
                System.err.println("Imagen del mapa es null, usando fallback");
                return generateFallbackMap(lat1, lon1, lat2, lon2);
            }

            Graphics2D g = mapImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);

            int x1 = lonToPixel(lon1, centerLon, zoom, W);
            int y1 = latToPixel(lat1, centerLat, zoom, H);
            int x2 = lonToPixel(lon2, centerLon, zoom, W);
            int y2 = latToPixel(lat2, centerLat, zoom, H);

            // Dashed orange line to visually connect start and end pins.
            g.setColor(new Color(255, 100, 0));
            g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND, 0, new float[]{15, 10}, 0));
            g.drawLine(x1, y1, x2, y2);

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
     * Synthetic map drawn entirely with Java2D — used when the OSM tile server
     * is unavailable. Paints an ocean gradient with a rough land polygon around
     * the route to give geographic context without network access.
     */
    private Path generateFallbackMap(double lat1, double lon1, double lat2, double lon2) {
        System.out.println("Usando mapa de respaldo simplificado");

        int W = 1080, H = 1920;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint ocean = new GradientPaint(
            0, 0, new Color(170, 211, 223),
            0, H, new Color(120, 180, 200)
        );
        g.setPaint(ocean);
        g.fillRect(0, 0, W, H);

        g.setColor(new Color(209, 219, 189));
        double margin = 8.0;
        double minLat = Math.min(lat1, lat2) - margin;
        double maxLat = Math.max(lat1, lat2) + margin;
        double minLon = Math.min(lon1, lon2) - margin;
        double maxLon = Math.max(lon1, lon2) + margin;

        int x1 = lonToX(lon1, minLon, maxLon, W);
        int y1 = latToY(lat1, minLat, maxLat, H);
        int x2 = lonToX(lon2, minLon, maxLon, W);
        int y2 = latToY(lat2, minLat, maxLat, H);

        // Rough hexagonal land mass surrounding the route.
        int[] xPoly = {
            x1 - 200, x1 + 200, x2 + 200, x2 - 200,
            (x1 + x2)/2 - 150, (x1 + x2)/2 + 150
        };
        int[] yPoly = {
            y1 - 300, y1 - 300, y2 + 300, y2 + 300,
            (y1 + y2)/2 - 200, (y1 + y2)/2 + 200
        };
        g.fillPolygon(xPoly, yPoly, 6);

        g.setColor(new Color(255, 100, 0));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 0, new float[]{15, 10}, 0));
        g.drawLine(x1, y1, x2, y2);

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

    /**
     * Draws the inspirational phrase over a semi-transparent box at the bottom of the map.
     * Writes to a new file (map_phrase.png) so the original map tile is preserved.
     */
    public Path addPhraseToMap(String phrase, Path mapPath) {
        try {
            BufferedImage img = ImageIO.read(mapPath.toFile());
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = img.getWidth();
            int H = img.getHeight();

            // Dark translucent box keeps the phrase readable over any map color.
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(40, H - 320, W - 80, 260, 20, 20);

            g.setColor(new Color(255, 215, 0, 100));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(40, H - 320, W - 80, 260, 20, 20);

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

    // --- Coordinate-to-pixel conversions (OSM tile map) ---

    /**
     * Maps a longitude to a horizontal pixel offset relative to the tile center.
     * Linear approximation — accurate enough at the zoom levels used (4–9).
     */
    private int lonToPixel(double lon, double centerLon, int zoom, int width) {
        double scale = Math.pow(2, zoom);
        double lonDiff = lon - centerLon;
        return (int) (width / 2 + (lonDiff * scale * width / 360.0));
    }

    /**
     * Maps a latitude to a vertical pixel offset relative to the tile center.
     * Y is inverted because screen coordinates grow downward while latitude grows upward.
     */
    private int latToPixel(double lat, double centerLat, int zoom, int height) {
        double scale = Math.pow(2, zoom);
        double latDiff = centerLat - lat; // inverted: positive diff → lower on screen
        return (int) (height / 2 + (latDiff * scale * height / 180.0));
    }

    // --- Coordinate-to-pixel conversions (fallback map, simple linear projection) ---

    private int lonToX(double lon, double minLon, double maxLon, int W) {
        return (int) ((lon - minLon) / (maxLon - minLon) * W);
    }

    /** Y is inverted: (1 - normalized) flips the axis so north is at the top. */
    private int latToY(double lat, double minLat, double maxLat, int H) {
        return (int) ((1 - (lat - minLat) / (maxLat - minLat)) * H);
    }

    // --- Drawing helpers ---

    /** Draws a colored circle pin with a drop shadow and a centered text label. */
    private void drawPin(Graphics2D g, int x, int y, Color color, String label) {
        int r = 20;

        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(x - r + 2, y + r - 5, r * 2, 10);   // drop shadow

        g.setColor(Color.WHITE);
        g.fillOval(x - r - 3, y - r - 3, (r + 3) * 2, (r + 3) * 2);  // white border

        g.setColor(color);
        g.fillOval(x - r, y - r, r * 2, r * 2);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x - fm.stringWidth(label) / 2, y + 6);
    }

    /** Word-wraps text within maxWidth pixels, advancing y by lineHeight per line. */
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