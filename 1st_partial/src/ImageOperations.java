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

    public static void rotateImage(BufferedImage image, double angle) {

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
