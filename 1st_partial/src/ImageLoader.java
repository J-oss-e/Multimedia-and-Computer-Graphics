import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class ImageLoader {

    public BufferedImage loadImage(String path) throws IOException {
        return ImageIO.read(new File(path));
    }

    public BufferedImage createCopy(BufferedImage original) {
        BufferedImage copy = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                original.getType()
        );
        /*
        Graphics g = copy.getGraphics(); // It's used to copy an image with a brush; it's a function from the awt library.
        g.drawImage(original, 0, 0, null);
        g.dispose();
        */

       // A substitute for graphic is a loop that goes pixel by pixel instead of using the awt function::
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                copy.setRGB(x, y, original.getRGB(x, y));
            }
        }

        return copy;
    }

    public void saveImage(BufferedImage image, String path) throws IOException {
        ImageIO.write(image, "png", new File(path));
    }
}
