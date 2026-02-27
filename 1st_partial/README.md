# Image Editor

This is a simple Java-based image editor designed as a student project. The application allows users to perform basic image manipulations such as inversion, rotation, and cropping. All operations work by scanning the image pixel by pixel, creating a modified copy while preserving the original for easy undo or further edits.

## 📁 Project Structure

- **Main.java** – Entry point of the application; handles user interaction and launches the editor.
- **EditableImage.java** – Keeps track of the original and edited images, providing a simple interface for accessing and updating both.
- **ImageLoader.java** – Handles file input/output operations, including loading and saving images.
- **ImageOperations.java** – Contains the image processing methods available to the user (rotate, invert, cut).

All classes are written with object‑oriented principles in mind to facilitate future extensions and improvements.

## 🛠️ Features

The editor currently supports the following operations:

1. **Rotate** – Rotate a rectangular region by 90, 180 or 270 degrees. The user supplies the coordinates of the top‑left corner plus width and height of the region.
2. **Invert** – Invert the colors of the entire image (RGB values are inverted).
3. **Cut (Crop)** – Extract a rectangular area and discard the rest of the image.
4. **Save** – Export the edited image in JPG format.

Each feature iterates over pixels rather than relying on high‑level APIs, offering a clear view of the underlying algorithms.

## 🔧 Dependencies

- Java Development Kit (JDK) 8 or higher.
- Uses the standard `javax.imageio` package for image I/O.

No external libraries are required.

## 🖥️ System Requirements

- **Operating System**: Cross-platform (Windows, macOS, Linux) as long as Java is installed.
- **Memory**: Depends on image size; larger images require more heap space.
- **Disk Space**: Minimal, only for storing source files and image assets.

## 🚀 Usage

### For Users
1. Compile the project using `javac` or open it in a Java IDE (Eclipse, IntelliJ IDEA, NetBeans, etc.).
2. Run the `Main` class and follow the console prompts to load an image from your filesystem.
3. Select from the menu of operations (invert, rotate, cut) and supply the required parameters when prompted.
4. Preview is simulated by inspecting the edited copy held in memory; when satisfied, choose the save option to write the result to disk as a JPG file.

> The application currently operates via a simple command‑line interface; no graphical frontend is provided.

### For Programmers / Contributors
- The source code is intentionally kept minimal and procedural within its classes to make studying the pixel‑manipulation logic easy.
- You can extend `ImageOperations` with additional static methods or refactor existing ones to instance methods if you prefer an object‑oriented design.
- `EditableImage` acts as a data holder; it can be replaced or augmented with an interface to support undo/redo or multiple layers.
- The `Main` class contains the command‑line menu; modifying it is a straightforward way to add new options or integrate a GUI library (e.g. Swing or JavaFX).
- Feel free to restructure packages (`tools`, `model`, etc.) for better separation of concerns.

## 💡 Improvements & Future Work

- Refactor using stronger OOP principles (interfaces, inheritance) to better separate concerns.
- Add a graphical user interface (GUI) for a more intuitive experience.
- Support additional file formats and more advanced operations (filters, scaling).
- Implement undo/redo functionality.

---

Feel free to explore the source and extend the editor as a learning exercise or starting point for a more complete application.