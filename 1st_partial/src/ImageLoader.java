import javax.imageio.ImageIO;
import java.awt.*;
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

        Graphics g = copy.getGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        return copy;
    }

    public void saveImage(BufferedImage image, String path) throws IOException {
        ImageIO.write(image, "png", new File(path));
    }
}
