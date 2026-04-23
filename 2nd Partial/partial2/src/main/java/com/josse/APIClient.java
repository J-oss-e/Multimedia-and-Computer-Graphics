package com.josse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

/**
 * Cliente para la API de Google Gemini (AI Studio).
 * Maneja generación de imágenes, audio y frases inspiracionales.
 * API Key: [TU_KEY_AQUI] — reemplazar antes de entregar.
 */
public class APIClient {

    private final String apiKey;
    private static final String GEMINI_TEXT_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/" +
        "gemini-1.5-flash:generateContent?key=%s";
    private static final String GEMINI_TTS_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/" +
        "gemini-1.5-flash:generateContent?key=%s"; // Gemini Flash para texto→audio

    private final HttpClient httpClient;

    public APIClient(String apiKey) {
        this.apiKey    = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Genera una frase inspiracional basada en los lugares visitados.
     * @param description descripción de los lugares
     * @return frase generada por Gemini
     */
    public String generatePhrase(String description) {
        String prompt = String.format(
            "Generate a short, poetic, inspirational phrase (max 20 words) " +
            "inspired by these places: %s. " +
            "Return ONLY the phrase, no quotes or extra text.", description);

        String body = String.format("""
            {"contents":[{"parts":[{"text":"%s"}]}]}
            """, prompt.replace("\"", "\\\""));

        return callGeminiText(body);
    }

    /**
     * Genera una descripción de audio para el video completo.
     * @param description descripción de todos los medios con fechas/lugares
     * @return texto de la descripción generada
     */
    public String generateAudioDescription(String description) {
        String prompt = String.format(
            "You are a narrator for a travel video. " +
            "Write a warm, engaging audio description for these moments: %s. " +
            "Keep it under 150 words, conversational tone.", description);

        String body = String.format("""
            {"contents":[{"parts":[{"text":"%s"}]}]}
            """, prompt.replace("\"", "\\\""));

        return callGeminiText(body);
    }

    /**
     * Convierte texto a audio usando Text-to-Speech del sistema (fallback).
     * Gemini aún no tiene TTS directo en free tier; usamos ffmpeg síntesis.
     * @param text       texto a convertir
     * @param outputPath ruta donde guardar el .mp3
     * @return ruta del audio generado
     */
    public Path generateAudio(String text, Path outputPath) {
        // Usando espeak como TTS local (cross-platform)
        // En producción, aquí iría la llamada a Google TTS API
        try {
            String[] cmd = {
                "espeak", "-w", outputPath.toString(),
                "-s", "150", "-v", "en",
                text
            };
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.start().waitFor();
            return outputPath;
        } catch (Exception e) {
            System.err.println("Error generando audio: " + e.getMessage());
            return null;
        }
    }

    // --- Helper privado ---

    private String callGeminiText(String jsonBody) {
        try {
            String url = String.format(GEMINI_TEXT_URL, apiKey);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Extraer el texto de la respuesta JSON
            String body = response.body();
            int textIdx = body.indexOf("\"text\":");
            if (textIdx != -1) {
                int start = body.indexOf("\"", textIdx + 7) + 1;
                int end   = body.indexOf("\"", start);
                return body.substring(start, end);
            }
            System.err.println("Respuesta inesperada de Gemini: " + body);
            return "Error generando contenido";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error generando contenido";
        }
    }
}