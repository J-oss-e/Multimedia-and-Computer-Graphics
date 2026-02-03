import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.*;
import java.awt.image.BufferedImage;

public class Landscape {
    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);

        int sunX = image.getWidth() / 4;
        int sunY = image.getHeight() / 4;
        double radiusSun = 40;

        double waveFrequency = 0.07;
        int waveAmplitude = 20;
        int floorLevel = (int) (image.getHeight() * 0.75);


        for (int x = 0; x < image.getWidth(); x++) {
            double waveHeight = floorLevel + waveAmplitude * Math.sin(x * waveFrequency);

            for (int y = 0; y < image.getHeight(); y++) {

                double distX = x - sunX;
                double distY = y - sunY;
                double distSun = sqrt((distX * distX) + (distY * distY));

                if (y > waveHeight) {
                    image.setRGB(x, y, Color.GREEN.getRGB());
                } else if (distSun < radiusSun) {
                    image.setRGB(x, y, Color.YELLOW.getRGB());
                } else {
                    image.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
        }

        int numRays = 8;

        for (int i = 0; i < numRays; i++) {
            double angle = i * (Math.PI * 2 / numRays);

            boolean diagonal = (i % 2 != 0);
            double rays = radiusSun/2;
            double DiagonalRays = rays*1.5;

            double rayLength = diagonal ? rays : DiagonalRays;

            for (double r = radiusSun; r < radiusSun + rayLength; r++) {
                int px = (int) (sunX + r * Math.cos(angle));
                int py = (int) (sunY + r * Math.sin(angle));

                if (px >= 0 && px < image.getWidth() && py >= 0 && py < image.getHeight()) {
                    image.setRGB(px, py, Color.black.getRGB());
                }
            }
        }

        File outputfile = new File("landscape.jpg");
        try {
            ImageIO.write(image, "jpg", outputfile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
