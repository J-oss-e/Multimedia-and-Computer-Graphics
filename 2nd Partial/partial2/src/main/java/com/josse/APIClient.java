package com.josse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * HTTP client for the OpenAI API.
 * Model choices:
 *   - gpt-4o-mini  for text (phrase + audio script) — fast and cheap for short prompts
 *   - dall-e-3     for the essence image — only model that supports 1024x1792 portrait
 *   - tts-1        for narration audio — lower latency than tts-1-hd, sufficient for video
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
     * Returns a poetic phrase (max 20 words) inspired by the visited places.
     * The prompt instructs the model to return ONLY the phrase so no trimming is needed.
     */
    public String generatePhrase(String description) {
        String prompt =
            "Generate a short poetic inspirational phrase, max 20 words, " +
            "inspired by these places: " + description +
            ". Return ONLY the phrase.";
        return callChatCompletion(prompt);
    }

    /**
     * Returns a short narration script (under 120 words) to be fed into TTS.
     * Kept brief so the generated audio fits within the video's slideshow duration.
     */
    public String generateAudioDescription(String description) {
        String prompt =
            "You are a warm travel narrator. Write a short audio description " +
            "(under 120 words) for a travel video covering: " + description;
        return callChatCompletion(prompt);
    }

    /**
     * Generates a portrait DALL-E 3 image and saves it as a PNG in the system temp dir.
     * Uses response_format=b64_json to avoid a second HTTP round-trip for the image URL.
     * Size 1024x1792 is the closest DALL-E 3 option to the 1080x1920 target resolution.
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

            // Manual string scan avoids deserializing the full response object
            // just to extract one base64 field.
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
     * Converts text to an MP3 file using OpenAI TTS with the "nova" voice.
     * The audio/speech endpoint returns raw MP3 bytes (not JSON), so the response
     * is handled with BodyHandlers.ofByteArray() and written directly to disk.
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

            HttpResponse<byte[]> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            java.nio.file.Files.write(outputPath, response.body());
            return outputPath;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sends a single-turn chat request to gpt-4o-mini and returns the message content.
     * JSON is parsed manually (indexOf scan) rather than with Gson to avoid allocating
     * a full response object for a single string field.
     */
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