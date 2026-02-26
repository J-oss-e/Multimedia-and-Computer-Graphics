import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageOperations {
    public static void invertColors(BufferedImage image) {
            double width = image.getWidth();
            double height = image.getHeight();

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Color C =  new Color(image.getRGB(x, y));
                    Color invertedC = new Color(
                            255 - C.getRed(), 255 - C.getGreen(), 255 - C.getBlue()
                    );
                    image.setRGB(x, y, invertedC.getRGB());
                }
            }
    }

    public void rotateImage(BufferedImage editable, int x, int y, int width, int height, int angle) {

        BufferedImage image = editable;

        if (x < 0 || y < 0 ||
                x + width > image.getWidth() ||
                y + height > image.getHeight()) {

            throw new IllegalArgumentException("Rotation area out of bounds");
        }

        if (angle != 90 && angle != 180 && angle != 270) {
            throw new IllegalArgumentException("Angle must be 90, 180 or 270");
        }

        // 1️⃣ Extraer región
        BufferedImage region = new BufferedImage(width, height, image.getType());

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                region.setRGB(i, j, image.getRGB(x + i, y + j));
            }
        }

        // 2️⃣ Crear imagen rotada
        BufferedImage rotated;

        if (angle == 90 || angle == 270) {
            rotated = new BufferedImage(height, width, image.getType());
        } else {
            rotated = new BufferedImage(width, height, image.getType());
        }

        // 3️⃣ Aplicar transformación
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                int pixel = region.getRGB(i, j);

                switch (angle) {

                    case 90:
                        rotated.setRGB(height - 1 - j, i, pixel);
                        break;

                    case 180:
                        rotated.setRGB(width - 1 - i, height - 1 - j, pixel);
                        break;

                    case 270:
                        rotated.setRGB(j, width - 1 - i, pixel);
                        break;
                }
            }
        }

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                image.setRGB(x + i, y + j, Color.white.getRGB());
            }
        }

        for (int j = 0; j < rotated.getHeight(); j++) {
            for (int i = 0; i < rotated.getWidth(); i++) {

                if (x + i < image.getWidth() && y + j < image.getHeight()) {
                    image.setRGB(x + i, y + j, rotated.getRGB(i, j));
                }
            }
        }
    }

    public static BufferedImage cropImage(BufferedImage image, int x, int y, int width, int height) {

        BufferedImage cropped = new BufferedImage(width, height, image.getType());

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int originalPixel = image.getRGB(x + i, y + j);
                cropped.setRGB(i, j, originalPixel);
            }
        }

        return cropped;
    }

}
