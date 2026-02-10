import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageDecompressor {

    public void decompress(String inputPath, String outputPath) {
        try {
            BufferedImage compressedImage = ImageIO.read(new File(inputPath));

            int width = compressedImage.getWidth();
            int height = compressedImage.getHeight();

            BufferedImage decompressedImage =
                    new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    decompressedImage.setRGB(x, y, compressedImage.getRGB(x, y));
                }
            }

            ImageIO.write(decompressedImage, "png", new File(outputPath));

        } catch (IOException e) {
            System.out.println("Error decompress");
        }
    }
}
