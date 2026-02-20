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

}
