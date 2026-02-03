import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.*;
import java.awt.image.BufferedImage;

public class Clock {
    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);

        int centerX = image.getWidth()/2, centerY = image.getHeight()/2;
        int radius = 100;

        for(int x = 0; x < image.getWidth(); x++){
            for(int y = 0; y < image.getHeight(); y++){

            }
        }

        for(int x = 0; x < image.getWidth(); x ++) {
            for(int y = 0; y < image.getHeight(); y++) {

                double distanciaX = x - centerX;
                double distanciaY = y - centerY;
                double distanciaTotal = sqrt((distanciaY*distanciaY) + (distanciaX*distanciaX));

                if (distanciaTotal > radius - 1 && distanciaTotal < radius + 1) {
                    image.setRGB(x, y, Color.white.getRGB());
                }
            }
        }

        File outputfile = new File("clock.jpg");
        try {
            ImageIO.write(image, "jpg", outputfile);
        }catch(IOException e){
            throw new RuntimeException(e);
        }

    }
}
