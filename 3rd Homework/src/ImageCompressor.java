import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageCompressor {

    public void compress(String inputPath, String outputPath) {
        try {
            BufferedImage originalImage = ImageIO.read(new File(inputPath));

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            BufferedImage compressedImage =
                    new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            int compressionFactor = 16;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {

                    Color color = new Color(originalImage.getRGB(x, y));

                    int red = (color.getRed() / compressionFactor) * compressionFactor;
                    int green = (color.getGreen() / compressionFactor) * compressionFactor;
                    int blue = (color.getBlue() / compressionFactor) * compressionFactor;

                    Color newColor = new Color(red, green, blue);
                    compressedImage.setRGB(x, y, newColor.getRGB());
                }
            }

            ImageIO.write(compressedImage, "png", new File(outputPath));

        } catch (IOException e) {
            System.out.println("Error");
        }
    }
}
