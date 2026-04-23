# Documentación del Paquete com.josse

## Descripción General
Este proyecto es una aplicación **VIDEO CREATOR** que genera videos profesionales a partir de multimedia (fotos y videos) combinados con contenido generado por IA (OpenAI). El sistema extrae metadatos, crea mapas de ubicaciones, genera narración y ensambla un video final en formato portrait (1080x1920).

---

## 📋 Tabla de Clases

1. [main](#main---punto-de-entrada)
2. [AppController](#appcontroller---controlador-principal)
3. [VisualMedia](#visualmedia---clase-abstracta-base)
4. [Photo](#photo---representa-fotos)
5. [Video](#video---representa-videos)
6. [dataType](#datatype---enumeración-de-tipos)
7. [APIClient](#apiclient---cliente-de-openai)
8. [FFmpegProcessor](#ffmpegprocessor---procesador-multimedia)
9. [MapGenerator](#mapgenerator---generador-de-mapas)
10. [VideoAssembler](#videoassembler---ensamblador-de-video)
11. [ScaleCalculator](#scalecalculator---constantes-de-resolución)

---

## 🔴 main - Punto de Entrada

**Ubicación:** `com.josse.main`

**Propósito:** Clase principal que orquesta toda la interacción con el usuario mediante una interfaz de línea de comandos (CLI).

### Funcionalidad:
- Solicita la API Key de OpenAI al usuario
- Gestiona la adición de archivos multimedia (fotos y videos)
- Valida que existan archivos con GPS válido
- Controla el flujo de:
  1. Agregación de archivos
  2. Generación de contenido IA
  3. Generación de mapas
  4. Creación del video final

### Método Principal:
```java
public static void main(String[] args)
```

### Características:
- ✅ Interfaz amigable con emojis y decoraciones ASCII
- ✅ Validación de archivos existentes
- ✅ Advertencia si hay menos de 2 archivos con GPS
- ✅ Entrada flexible del nombre del video
- ✅ Cierre seguro de Scanner

### Formatos Soportados:
- **Fotos:** jpg, jpeg, png, webp
- **Videos:** mp4, mov, avi

---

## 🎮 AppController - Controlador Principal

**Ubicación:** `com.josse.AppController`

**Propósito:** Orquesta todos los componentes del sistema: API, procesamiento, mapas y ensamblado de video.

### Componentes Inyectados:
```java
private final APIClient apiClient;           // Cliente OpenAI
private final FFmpegProcessor ffmpegProcessor;// Procesador multimedia
private final MapGenerator mapGenerator;      // Generador de mapas
private final ScaleCalculator scaleCalculator;// Calculador de escala
private VideoAssembler videoAssembler;        // Ensamblador final
private final List<VisualMedia> allMedia;     // Colección de medios
private Path essenceImage;                    // Imagen generada por IA
private Path map;                             // Mapa con coordenadas
private Path audioNarration;                  // Audio narración
```

### Métodos Principales:

#### `public void addMedia(Path filePath)`
Agrega un archivo multimedia al proyecto. Detecta el tipo por extensión.

**Proceso:**
1. Identifica extensión del archivo
2. Crea instancia `Photo` o `Video` según corresponda
3. Extrae metadatos usando FFmpegProcessor
4. Almacena en la colección

**Ejemplo:**
```java
controller.addMedia(Paths.get("foto.jpg"));
```

#### `public void generateAIContent()`
Genera contenido IA basado en ubicaciones GPS de los archivos.

**Proceso:**
1. Filtra medios con GPS válido
2. Obtiene nombres de ubicaciones con Nominatim
3. Genera imagen de esencia con DALL-E 3
4. Genera descripción narrativa
5. Convierte a audio MP3 con TTS
6. Genera frase inspiracional

**Validaciones:**
- ⚠️ Requiere al menos 1 medio con GPS válido
- ⚠️ Almacena frase en archivo temporal

#### `public void generateMap()`
Crea mapa visual mostrando primer y último punto GPS (por fecha).

**Proceso:**
1. Ordena medios por fecha
2. Obtiene coordenadas del primero y último
3. Genera mapa base con OpenStreetMap
4. Añade pines de inicio/fin
5. Superpone frase inspiracional

#### `public Path createVideo(Path outputPath)`
Ejecuta el flujo completo de creación de video.

**Retorna:** Ruta del video final MP4

#### `public List<VisualMedia> getAllMedia()`
Retorna colección de todos los medios agregados.

---

## 📦 VisualMedia - Clase Abstracta Base

**Ubicación:** `com.josse.VisualMedia`

**Propósito:** Define la estructura común para fotos y videos.

### Atributos Protegidos:
```java
protected String name;              // Nombre del archivo
protected Path path;                // Ruta original
protected Path scaledPath;          // Ruta escalada (procesada)
protected dataType type;            // PHOTO o VIDEO
protected double latitude;          // Coordenada GPS
protected double longitude;         // Coordenada GPS
protected LocalDateTime date;       // Fecha de creación
protected int width;                // Ancho en píxeles
protected int height;               // Alto en píxeles
```

### Métodos Abstractos:
```java
public abstract void extractMetadata(FFmpegProcessor ffmpeg);
```

Cada subclase implementa su propia estrategia de extracción de metadatos.

### Getters Disponibles:
- `getName()`, `getPath()`, `getScaledPath()`
- `getType()`, `getLatitude()`, `getLongitude()`
- `getDate()`, `getWidth()`, `getHeight()`

### Setters:
- `setScaledPath(Path)` - Establece ruta del archivo escalado

### Constructores:
```java
public VisualMedia()
public VisualMedia(String name, Path path, dataType type,
                   double latitude, double longitude,
                   LocalDateTime date, int width, int height)
```

---

## 📸 Photo - Representa Fotos

**Ubicación:** `com.josse.Photo`

**Hereda de:** `VisualMedia`

**Propósito:** Especialización para archivos fotográficos (JPG, PNG, WEBP).

### Extracción de Metadatos:
Usa la librería **metadata-extractor** (no FFprobe, que no lee GPS de imágenes estáticas).

**Proceso:**
1. Lee datos JPEG/PNG con ImageMetadataReader
2. Extrae dimensiones del directorio JPEG
3. Extrae fecha del directorio EXIF-SubIFD
4. Extrae coordenadas GPS del directorio GPS

**Formato de Coordenadas GPS:** `WGS84` (latitud, longitud)

**Ejemplo de Uso:**
```java
Photo photo = new Photo();
photo.path = Paths.get("viaje.jpg");
photo.extractMetadata(ffmpegProcessor);
// Resultado: photo.latitude, photo.longitude, photo.date establecidas
```

---

## 🎬 Video - Representa Videos

**Ubicación:** `com.josse.Video`

**Hereda de:** `VisualMedia`

**Propósito:** Especialización para archivos de video (MP4, MOV, AVI).

### Atributos Adicionales:
```java
private double duration;  // Duración en segundos
```

### Extracción de Metadatos:
Usa **FFprobe** en formato JSON para extraer información.

**Proceso:**
1. Ejecuta `ffprobe -print_format json` en el video
2. Parsea JSON para obtener:
   - Dimensiones (width, height) del stream de video
   - Duración del stream
   - Fecha de creación de los metadatos (tags)
   - Ubicación GPS (tag "location")

**Formato GPS en FFprobe:** `±lat±lon` (ej: "+40.7128-74.0060")

**Getter Adicional:**
```java
public double getDuration()
```

**Ejemplo de Uso:**
```java
Video video = new Video();
video.path = Paths.get("video.mp4");
video.extractMetadata(ffmpegProcessor);
// Resultado: video.width, video.height, video.duration establecidas
```

---

## 🏷️ dataType - Enumeración de Tipos

**Ubicación:** `com.josse.dataType`

**Propósito:** Define los tipos de media soportados.

### Valores:
```java
public enum dataType {
    VIDEO,  // Archivo de video
    PHOTO   // Archivo de foto/imagen
}
```

**Uso:**
```java
if (media.getType() == dataType.PHOTO) {
    // Procesar como foto
}
```

---

## 🔌 APIClient - Cliente de OpenAI

**Ubicación:** `com.josse.APIClient`

**Propósito:** Interfaz para consumir servicios de OpenAI.

### Servicios Utilizados:
| Servicio | Modelo | Uso |
|----------|--------|-----|
| Chat Completion | gpt-4o-mini | Frases inspiracionales (económico) |
| Image Generation | DALL-E 3 | Imagen de esencia del viaje |
| Text-to-Speech | tts-1 | Audio narración con voz "nova" |

### Métodos Principales:

#### `public String generatePhrase(String description)`
Genera una frase inspiracional corta (máx 20 palabras).

**Parámetros:**
- `description`: Descripción de lugares visitados

**Retorna:** Frase inspiracional

**Ejemplo:**
```java
String phrase = apiClient.generatePhrase("Paris, Rome, Barcelona");
// "A journey through history's greatest masterpieces awaits."
```

#### `public String generateAudioDescription(String description)`
Genera descripción narrativa para el audio del video.

**Parámetros:**
- `description`: Descripción de lugares y fechas visitadas

**Retorna:** Descripción narrativa (máx 120 palabras)

#### `public Path generateEssenceImage(String description)`
Genera imagen visual de la esencia del viaje usando DALL-E 3.

**Características:**
- Resolución: 1024x1792 (portrait)
- Formato: PNG (decodificado de Base64)
- Estilo: Cinematic, vibrant

**Parámetros:**
- `description`: Descripción de lugares

**Retorna:** Ruta del PNG generado en carpeta temporal

**Manejo de Errores:** Retorna `null` si hay error

#### `public Path generateAudio(String text, Path outputPath)`
Convierte texto a audio MP3 usando TTS.

**Características:**
- Modelo: tts-1
- Voz: nova (narrativa, clara)
- Formato: MP3

**Parámetros:**
- `text`: Texto a convertir
- `outputPath`: Ruta del archivo MP3 a generar

**Retorna:** Ruta del MP3 generado

### Métodos Privados:

#### `private String callChatCompletion(String userMessage)`
Helper para llamadas al endpoint de Chat Completion.

**Configuración:**
- Modelo: gpt-4o-mini
- Rol: user
- Max tokens: 200

---

## ⚙️ FFmpegProcessor - Procesador Multimedia

**Ubicación:** `com.josse.FFmpegProcessor`

**Propósito:** Wrapper sobre FFmpeg/FFprobe para procesamiento multimedia.

### Constantes:
```java
private static final int TARGET_W = 1080;   // Ancho portrait
private static final int TARGET_H = 1920;   // Alto portrait
```

### Métodos Principales:

#### `public String extractMediaMetadata(Path media)`
Extrae metadatos de un archivo multimedia en formato JSON.

**Comandos Ejecutados:**
```bash
ffprobe -v quiet -print_format json -show_streams -show_format <archivo>
```

**Retorna:** JSON string con metadata

**Ejemplo de Salida JSON:**
```json
{
  "streams": [
    {
      "codec_type": "video",
      "width": 1920,
      "height": 1080,
      "duration": "10.5"
    }
  ],
  "format": {
    "tags": {
      "creation_time": "2024-01-15T12:30:00.000000Z",
      "location": "+40.7128-74.0060"
    }
  }
}
```

#### `public boolean scaleMedia(VisualMedia media)`
Escala un medio a resolución portrait (1080x1920).

**Parámetro por defecto:** 3 segundos de duración para fotos

**Para Fotos:**
1. Convierte a video de N segundos
2. Aplica filtro de escala con padding negro
3. Genera MP4 con codec H.264

**Para Videos:**
1. Escala preservando aspect ratio
2. Añade padding negro si es necesario
3. Regenera con codec H.264

#### `public boolean scaleMedia(VisualMedia media, double photoDuration)`
Escala un medio con duración personalizada (para fotos).

**Parámetros:**
- `media`: Medio a procesar
- `photoDuration`: Duración en segundos (solo aplica a fotos)

#### `public boolean normalizeAudio(Path audio, Path output)`
Normaliza audio según estándares de YouTube.

**Estándares YouTube:**
- Loudness: -15 LUFS (Integrated)
- True Peak: -1.5 dBTP
- Loudness Range (LRA): 7 LU

**Comando FFmpeg:**
```bash
ffmpeg -i <audio> -af "loudnorm=I=-15:TP=-1.5:LRA=7" <output>
```

#### `public boolean assembleVideoSimple(Path finalPath, List<VisualMedia> allMedia)`
Ensambla el video final a partir de clips escalados.

**Proceso:**
1. Crea archivo de concatenación (concat demuxer)
2. Usa COPY codec (sin recodificación)
3. Preserva codecs originales

**Archivo Concatenación:**
```
file '/ruta/video1.mp4'
file '/ruta/video2.mp4'
file '/ruta/video3.mp4'
```

### Métodos Privados:

#### `private String commandExecution(String[] instruction)`
Ejecuta comando shell y retorna salida como String.

**Manejo de Errores:** Registra código de salida si ≠ 0

---

## 🗺️ MapGenerator - Generador de Mapas

**Ubicación:** `com.josse.MapGenerator`

**Propósito:** Genera mapas visuales con coordenadas GPS.

### Servicios Externos:

#### Nominatim (OpenStreetMap Reverse Geocoding)
- **Endpoint:** `https://nominatim.openstreetmap.org/reverse`
- **Uso:** Obtener nombre legible de una ubicación
- **Parámetros:** `lat`, `lon`
- **Retorna:** JSON con `display_name`

#### OpenStreetMap Static Map API
- **Endpoint:** `https://staticmap.openstreetmap.de/staticmap.php`
- **Uso:** Descargar tiles de mapa
- **Parámetros:** center, zoom, size, maptype

### Métodos Principales:

#### `public String getLocationName(double lat, double lon)`
Obtiene el nombre legible de una ubicación.

**Proceso:**
1. Realiza petición HTTP a Nominatim
2. Parsea JSON para extraer `display_name`
3. Maneja errores de red

**Retorna:** Nombre de ubicación (ej: "Paris, Île-de-France, France")

**Fallback:** "Ubicación desconocida" si hay error

**Ejemplo:**
```java
String location = mapGenerator.getLocationName(48.8566, 2.3522);
// "Paris, 75001, Île-de-France, France"
```

#### `public Path generateMap(double lat1, double lon1, double lat2, double lon2)`
Genera mapa visual con dos pines (inicio y fin).

**Resolución:** 1080x1920 (portrait)

**Proceso:**
1. Calcula centro entre dos puntos
2. Calcula zoom dinámico basado en distancia
3. Descarga tiles de mapa
4. Dibuja línea punteada entre puntos
5. Añade dos pines: verde (Inicio), rojo (Fin)

**Lógica de Zoom:**
```
maxDiff > 10  → zoom = 4  (mundo)
maxDiff > 5   → zoom = 5  (continente)
maxDiff > 2   → zoom = 6  (país)
maxDiff > 1   → zoom = 7  (región)
maxDiff > 0.5 → zoom = 8  (ciudad)
otros         → zoom = 9  (barrio)
```

**Características Visuales:**
- Línea: Naranja punteada (15px-10px)
- Pin Inicio: Verde, etiqueta "Inicio"
- Pin Fin: Rojo, etiqueta "Fin"

**Retorna:** Ruta del PNG generado

**Fallback:** `generateFallbackMap()` si hay error

#### `public Path addPhraseToMap(String phrase, Path mapBase)`
Superpone una frase inspiracional en el mapa.

**Características:**
- Posición: Centro inferior
- Fuente: Sans Serif, 48pt
- Color: Blanco con sombra negra
- Fondо: Semi-transparente

**Parámetros:**
- `phrase`: Texto a superponer
- `mapBase`: Ruta del mapa base PNG

**Retorna:** Ruta del PNG con frase

### Método Privado:

#### `private int latToPixel(...)`
Convierte coordenada de latitud a píxel en imagen del tile.

#### `private int lonToPixel(...)`
Convierte coordenada de longitud a píxel en imagen del tile.

#### `private void drawPin(...)`
Dibuja un pin circular con etiqueta en el mapa.

#### `private Path generateFallbackMap(...)`
Genera mapa alternativo si falla descarga de tiles.

---

## 📏 ScaleCalculator - Constantes de Resolución

**Ubicación:** `com.josse.ScaleCalculator`

**Propósito:** Define constantes de resolución para video portrait.

### Constantes:
```java
private static final double width  = 1080;  // Ancho en píxeles
private static final double height = 1920;  // Alto en píxeles
```

### Métodos:
```java
public static double getWidth()   // Retorna 1080
public static double getHeight()  // Retorna 1920
```

**Justificación:** Formato portrait estándar para:
- TikTok
- Instagram Reels
- YouTube Shorts
- Dispositivos móviles

---

## 🎞️ VideoAssembler - Ensamblador de Video

**Ubicación:** `com.josse.VideoAssembler`

**Propósito:** Orquesta el ensamblado final del video.

### Componentes Inyectados:
```java
private List<VisualMedia> allMedia;        // Todos los medios
private Path map;                          // Mapa con frase
private Path essenceImage;                 // Imagen de esencia
private Path audioNarration;               // Audio narración
private final FFmpegProcessor ffmpeg;      // Procesador
```

### Métodos Principales:

#### `public List<VisualMedia> orderMedia(List<VisualMedia> toOrder)`
Ordena medios cronológicamente.

**Proceso:**
1. Ordena por fecha de menor a mayor
2. Coloca elementos sin fecha al final

**Retorna:** List ordenada

#### `public Path generateFinalVideo(Path outputPath)`
Ejecuta flujo completo de creación del video.

**Flujo Completo:**

1. **Obtener Duración de Audio**
   - Extrae duración del MP3 de narración
   - Usa ffprobe en formato JSON

2. **Ordenar Medios por Fecha**
   - Aplica `orderMedia()`

3. **Construir Lista Final**
   - [essenceImage, ...media_ordenado, map]
   - Validación: elementos deben existir

4. **Calcular Duración por Foto**
   - Si hay audio: `photoDuration = audioDuration / numPhotos`
   - Rango: 2.0 a 8.0 segundos
   - Default: 3.0 segundos

5. **Escalar Todos los Medios**
   - Fotos: Escala con duración calculada
   - Videos: Escala preservando duración original
   - Resolución target: 1080x1920 (portrait)

6. **Ensamblar Video Sin Audio**
   - Concatena todos los clips escalados
   - Genera `video_no_audio.mp4`

7. **Normalizar Audio**
   - Aplica estándares YouTube
   - Target: -15 LUFS, -1.5 dBTP, 7 LU LRA
   - Fallback: Usa original si hay error

8. **Combinar Video + Audio Normalizado**
   - Usa codec copy para video (sin recodificación)
   - Codifica audio a AAC 192kbps
   - Usa -shortest para alinear duraciones

9. **Retornar Video Final**
   - Ruta del MP4 con audio sincronizado
   - `null` si hay error

### Métodos Privados:

#### `private double getAudioDuration(Path audioPath)`
Extrae duración de archivo MP3 usando ffprobe.

**Retorna:** Duración en segundos (0 si hay error)

---

## 🔄 Flujo Completo de Ejecución

```
main.java (entrada usuario)
    ↓
AppController.addMedia() ← agregar fotos/videos
    ↓
[Photo/Video].extractMetadata() ← extraer GPS, fecha, dimensiones
    ↓
AppController.generateAIContent()
    ├─ APIClient.generateEssenceImage() → PNG
    ├─ APIClient.generateAudioDescription() → texto
    ├─ APIClient.generateAudio() → MP3
    └─ APIClient.generatePhrase() → frase
    ↓
AppController.generateMap()
    ├─ MapGenerator.generateMap() → PNG base
    └─ MapGenerator.addPhraseToMap() → PNG final
    ↓
AppController.createVideo()
    ↓
VideoAssembler.generateFinalVideo()
    ├─ FFmpegProcessor.scaleMedia() ← todas las fotos/videos
    ├─ FFmpegProcessor.assembleVideoSimple() → video sin audio
    ├─ FFmpegProcessor.normalizeAudio() → audio normalizado
    └─ Combina video + audio → video_final.mp4
    ↓
Retorna ruta del video final al usuario
```

---

## 📊 Dependencias Externas

### Librerías Maven:
- **Gson:** Parsing de JSON (OpenAI API, FFprobe)
- **metadata-extractor:** Extracción EXIF de imágenes
- **Java HTTP Client:** Requests a OpenAI, Nominatim, OpenStreetMap

### Herramientas del Sistema:
- **FFmpeg:** Procesamiento de video/audio
- **FFprobe:** Extracción de metadatos multimedia

---

## ⚠️ Consideraciones de Seguridad

1. **API Key OpenAI:** Se solicita en tiempo de ejecución (no hardcodeado)
2. **Archivos Temporales:** Se generan en `/tmp` del sistema
3. **Validación de Entrada:** Se valida existencia de archivos
4. **Manejo de Errores:** Try-catch en operaciones de I/O y network

---

## 🎯 Casos de Uso Principales

### Caso 1: Crear Video de Viaje Automático
```
1. Cargar fotos/videos de viaje
2. Extraer metadatos (GPS, fecha, dimensiones)
3. Generar imagen de esencia y descripción con IA
4. Crear mapa interactivo con ruta
5. Generar narración de audio y frase inspiracional
6. Ensamblar video final en resolución portrait
7. Normalizar audio para plataformas sociales
```

### Caso 2: Validaciones Críticas
- ✅ Mínimo 2 archivos con GPS para generar mapa
- ✅ Formato de archivo soportado
- ✅ Archivo debe existir en ruta especificada
- ✅ API Key válida para acceder a OpenAI

---

## 📝 Notas de Implementación

### Escalado de Medios (1080x1920 Portrait)
- Mantiene aspect ratio original
- Rellena espacios con píxeles negros
- Fotos se convierten a video MP4 de duración variable
- Videos se recodifican a H.264

### Sincronización Audio-Video
- Si audio > video: se corta audio (-shortest)
- Si video > audio: se completa con silencio
- Audio normalizado a -15 LUFS (YouTube standard)

### Limitaciones Conocidas
- GPS en videos depende de metadatos de cámara
- Descarga de tiles de mapa requiere conexión
- OpenAI requiere API key válida y saldo disponible
- FFmpeg debe estar en PATH o ruta completa

