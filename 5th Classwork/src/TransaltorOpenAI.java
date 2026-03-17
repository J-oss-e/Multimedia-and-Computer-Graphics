import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TransaltorOpenAI {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the path to the .txt file to translate: ");
        String inputPath = scanner.nextLine().trim();

        File inputFile = new File(inputPath);
        if (!inputFile.exists() || !inputFile.isFile()) {
            System.err.println("Error");
            System.exit(1);
        }
        if (!inputPath.toLowerCase().endsWith(".txt")) {
            System.err.println("Error");
            System.exit(1);
        }

        String fileContent = Files.readString(inputFile.toPath());
        if (fileContent.isBlank()) {
            System.err.println("Error");
            System.exit(1);
        }

        System.out.print("Enter the target language (e.g. Spanish, French, Japanese): ");
        String targetLanguage = scanner.nextLine().trim();
        if (targetLanguage.isBlank()) {
            System.err.println("Error");
            System.exit(1);
        }

        String openAIToken = System.getenv("OpenAIToken");
        System.out.println("\nTranslating to " + targetLanguage + "... please wait.");

        String escapedContent = escapeJson(fileContent);
        String jsonPayload = "{"
                + "\"model\": \"gpt-4o-mini\","
                + "\"messages\": ["
                + "  {\"role\": \"system\", \"content\": \"You are a professional translator. "
                + "Translate the user's text to " + escapeJson(targetLanguage) + ". "
                + "Return ONLY the translated text, no explanations or extra commentary.\"},"
                + "  {\"role\": \"user\", \"content\": \"" + escapedContent + "\"}"
                + "]"
                + "}";

        File tempPayload = File.createTempFile("openai_payload_", ".json");
        tempPayload.deleteOnExit();
        Files.writeString(tempPayload.toPath(), jsonPayload);

        Process process = getProcess(openAIToken, tempPayload);

        String apiResponse = new String(process.getInputStream().readAllBytes());

        String curlErrors = new String(process.getErrorStream().readAllBytes());

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.err.println("Error: curl exited with code " + exitCode);
            if (!curlErrors.isBlank()) {
                System.err.println("curl stderr: " + curlErrors);
            }
            System.exit(1);
        }

        String translatedText = extractTranslatedText(apiResponse);

        if (translatedText == null) {
            System.err.println("Error: Could not parse translation from API response.");
            System.err.println("Raw response: " + apiResponse);
            System.exit(1);
        }

        String baseName = inputPath.substring(0, inputPath.lastIndexOf('.'));
        String outputPath = baseName + "_" + targetLanguage.replaceAll("\\s+", "_") + ".txt";

        Files.writeString(Path.of(outputPath), translatedText);

        System.out.println("Translation complete!");
        System.out.println("Output file: " + outputPath);
    }

    private static Process getProcess(String openAIToken, File tempPayload) throws IOException {
        List<String> command = new ArrayList<>(Arrays.asList(
                "curl", "--silent", "--show-error",
                "-X", "POST",
                "https://api.openai.com/v1/chat/completions",
                "-H", "Content-Type: application/json",
                "-H", "Authorization: Bearer " + openAIToken,
                "-d", "@" + tempPayload.getAbsolutePath()
        ));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        return pb.start();
    }

    private static String extractTranslatedText(String json) {
        String marker = "\"content\":";
        int idx = json.indexOf(marker);
        if (idx == -1) return null;

        int start = json.indexOf('"', idx + marker.length());
        if (start == -1) return null;

        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"'  -> sb.append('"');
                    case '\\'  -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> sb.append(next);
                }
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}