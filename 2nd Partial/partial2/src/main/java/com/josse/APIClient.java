package com.josse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Cliente para la API de OpenAI.
 * - Texto/frases:    gpt-4o-mini  (barato, más que suficiente)
 * - Imagen esencia:  dall-e-3
 * - Audio TTS:       tts-1
 *
 * API Key: sk-... (reemplazar antes de entregar)
 */
public class APIClient {

    private final String apiKey;
    private final HttpClient httpClient;

    private static final String BASE = "https://api.openai.com/v1";

    public APIClient(String apiKey) {
        this.apiKey     = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Genera una frase inspiracional corta basada en los lugares visitados.
     * Usa gpt-4o-mini (rápido y económico).
     */
    public String generatePhrase(String description) {
        String prompt =
            "Generate a short poetic inspirational phrase, max 20 words, " +
            "inspired by these places: " + description +
            ". Return ONLY the phrase.";
        return callChatCompletion(prompt);
    }

    /**
     * Genera una descripción narrativa para usar como audio del video.
     */
    public String generateAudioDescription(String description) {
        String prompt =
            "You are a warm travel narrator. Write a short audio description " +
            "(under 120 words) for a travel video covering: " + description;
        return callChatCompletion(prompt);
    }

    /**
     * Genera la imagen de esencia con DALL-E 3.
     * Retorna la ruta del archivo PNG descargado en /tmp.
     */
    public Path generateEssenceImage(String description) {
        try {
            String prompt = "A single artistic image capturing the essence of " +
                            "a trip to: " + description +
                            ". Cinematic, vibrant, portrait orientation.";

            String body = """
                {
                  "model": "dall-e-3",
                  "prompt": "%s",
                  "n": 1,
                  "size": "1024x1792",
                  "response_format": "b64_json"
                }
                """.formatted(prompt.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/images/generations"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Extraer b64_json de la respuesta
            String resp = response.body();
            int idx = resp.indexOf("\"b64_json\"");
            if (idx == -1) {
                System.err.println("Error DALL-E: " + resp);
                return null;
            }
            int start = resp.indexOf("\"", idx + 10) + 1;
            int end   = resp.indexOf("\"", start);
            byte[] imageBytes = Base64.getDecoder().decode(resp.substring(start, end));

            Path outputPath = Paths.get(
                System.getProperty("java.io.tmpdir"), "essence_image.png");
            java.nio.file.Files.write(outputPath, imageBytes);
            return outputPath;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convierte texto a audio MP3 con OpenAI TTS (tts-1, voz "nova").
     * Retorna la ruta del archivo de audio generado.
     */
    public Path generateAudio(String text, Path outputPath) {
        try {
            String body = """
                {
                  "model": "tts-1",
                  "input": "%s",
                  "voice": "nova",
                  "response_format": "mp3"
                }
                """.formatted(text.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/audio/speech"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            // La respuesta es bytes directamente (MP3)
            HttpResponse<byte[]> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            java.nio.file.Files.write(outputPath, response.body());
            return outputPath;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- Helper privado ---

    private String callChatCompletion(String userMessage) {
        try {
            String body = """
                {
                  "model": "gpt-4o-mini",
                  "messages": [
                    {"role": "user", "content": "%s"}
                  ],
                  "max_tokens": 200
                }
                """.formatted(userMessage.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String resp = response.body();
            // Parsear "content" del primer choice
            int idx = resp.indexOf("\"content\":");
            if (idx == -1) {
                System.err.println("Error GPT: " + resp);
                return "Error generating content";
            }
            int start = resp.indexOf("\"", idx + 10) + 1;
            int end   = resp.indexOf("\"", start);
            return resp.substring(start, end);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating content";
        }
    }
}