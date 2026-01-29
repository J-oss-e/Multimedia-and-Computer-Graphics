import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;

public class Clock {
    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);




        File outputfile = new File("clock.jpg");
        try {
            ImageIO.write(image, "jpg", outputfile);
        }catch(IOException e){
            throw new RuntimeException(e);
        }

    }
}
