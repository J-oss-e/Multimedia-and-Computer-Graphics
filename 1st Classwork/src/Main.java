import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;

public class Main {
    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);


        for(int x = 0; x < image.getWidth(); x++) {
            int z = (x * image.getHeight()) / image.getWidth();
            for (int y = 0; y < z; y++) {
                image.setRGB(x, y, Color.red.getRGB());
            }
        }

        for(int x = 0; x < image.getWidth(); x++){
            int z = (x * image.getHeight()) / image.getWidth();
            for(int y = image.getHeight() -1; y > z; y--){
                image.setRGB(x, y, Color.blue.getRGB());
            }
        }



        File outputfile = new File("output.jpg");
        try {
            ImageIO.write(image, "jpg", outputfile);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}