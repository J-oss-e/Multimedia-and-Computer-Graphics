package com.josse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class main {
    public static void main(String[] args) {
        
        Scanner scanner = new Scanner(System.in);
        
        // ============================================
        // CONFIGURACIÓN INICIAL
        // ============================================
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   VIDEO CREATOR - Multimedia Project  ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        System.out.print("Ingresa tu API Key de OpenAI: ");
        String apiKey = scanner.nextLine().trim();
        
        String ffmpegPath = "ffmpeg"; // O la ruta completa si no está en PATH
        
        AppController controller = new AppController(apiKey, ffmpegPath);
        
        // ============================================
        // AGREGAR ARCHIVOS MULTIMEDIA
        // ============================================
        System.out.println("\n=== AGREGAR ARCHIVOS MULTIMEDIA ===");
        System.out.println("Formatos soportados: jpg, jpeg, png, webp, mp4, mov, avi");
        System.out.println("(Escribe 'fin' para terminar de agregar archivos)\n");
        
        int archivosCargados = 0;
        while (true) {
            System.out.print("Ruta del archivo #" + (archivosCargados + 1) + ": ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("fin")) {
                break;
            }
            
            if (input.isEmpty()) {
                System.out.println("⚠️  Ruta vacía, intenta de nuevo.\n");
                continue;
            }
            
            try {
                Path filePath = Paths.get(input);
                if (!filePath.toFile().exists()) {
                    System.out.println("❌ Archivo no encontrado: " + input + "\n");
                    continue;
                }
                
                controller.addMedia(filePath);
                archivosCargados++;
                System.out.println("✅ Archivo agregado correctamente\n");
                
            } catch (Exception e) {
                System.out.println("❌ Error al agregar archivo: " + e.getMessage() + "\n");
            }
        }
        
        if (archivosCargados == 0) {
            System.out.println("\n❌ No se agregaron archivos. Saliendo...");
            scanner.close();
            return;
        }
        
        // ============================================
        // MOSTRAR METADATA EXTRAÍDA
        // ============================================
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║         METADATA EXTRAÍDA             ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        int index = 1;
        for (VisualMedia m : controller.getAllMedia()) {
            System.out.println("📄 Archivo #" + index + ": " + m.getName());
            System.out.println("   Dimensiones: " + m.getWidth() + "x" + m.getHeight());
            System.out.println("   Fecha:       " + (m.getDate() != null ? m.getDate() : "Sin fecha"));
            System.out.println("   GPS:         " + m.getLatitude() + ", " + m.getLongitude());
            System.out.println();
            index++;
        }
        
        // Validar que haya suficientes archivos con GPS
        long archivosConGPS = controller.getAllMedia().stream()
            .filter(m -> m.getLatitude() != 0.0 && m.getLongitude() != 0.0)
            .count();
        
        if (archivosConGPS < 2) {
            System.out.println("⚠️  ADVERTENCIA: Se necesitan al menos 2 archivos con GPS válido");
            System.out.println("    para generar el mapa. Archivos con GPS: " + archivosConGPS);
            System.out.print("\n¿Deseas continuar de todas formas? (s/n): ");
            String respuesta = scanner.nextLine().trim();
            if (!respuesta.equalsIgnoreCase("s")) {
                System.out.println("Proceso cancelado.");
                scanner.close();
                return;
            }
        }
        
        // ============================================
        // GENERAR CONTENIDO CON IA
        // ============================================
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║      GENERANDO CONTENIDO CON IA       ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        System.out.println("🤖 Generando imagen de esencia y frase inspiracional...");
        controller.generateAIContent();
        
        // ============================================
        // GENERAR MAPA
        // ============================================
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║          GENERANDO MAPA               ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        System.out.println("🗺️  Creando mapa con ubicaciones...");
        controller.generateMap();
        
        // ============================================
        // CREAR VIDEO FINAL
        // ============================================
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║        CREANDO VIDEO FINAL            ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        System.out.print("Nombre del video de salida (sin extensión): ");
        String nombreVideo = scanner.nextLine().trim();
        if (nombreVideo.isEmpty()) {
            nombreVideo = "video_final";
        }
        
        Path outputPath = Paths.get(nombreVideo + ".mp4");
        
        System.out.println("\n🎬 Procesando video...");
        System.out.println("   (Esto puede tardar varios minutos)\n");
        
        Path videoCreado = controller.createVideo(outputPath);
        
        // ============================================
        // RESULTADO FINAL
        // ============================================
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║            RESULTADO FINAL            ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        if (videoCreado != null) {
            System.out.println("✅ ¡Video creado exitosamente!");
            System.out.println("📁 Ubicación: " + videoCreado.toAbsolutePath());
            System.out.println("\n🎉 Proceso completado con éxito.");
        } else {
            System.out.println("❌ Error al crear el video.");
            System.out.println("   Revisa los errores mostrados arriba.");
        }
        
        scanner.close();
    }
}