import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;

public class Barycentric {
    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);

        double height = image.getHeight();
        double width = image.getWidth();
        double xC = width/2, yC = 0;
        double xA = 0, yA = height;
        double xB = width, yB = height;


        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double λ1 = ((yB - yC)*(x - xC) + (xC - xB)*(y - yC))/((yB - yC)*(xA - xC) + (xC - xB)*(yA - yC));
                double λ2 = ((yC - yA)*(x - xC) + (xA - xC)*(y - yC))/((yB - yC)*(xA - xC) + (xC - xB)*(yA - yC));
                double λ3 = 1-λ1-λ2;

                if (λ1 >= 0 && λ2 >= 0 && λ3 >= 0) {
                    int red = (int)((λ1*Color.red.getRed()) + (λ2*Color.green.getRed()) + (λ3*Color.blue.getRed()));
                    int green = (int)((λ1*Color.red.getGreen()) + (λ2*Color.green.getGreen()) + (λ3*Color.blue.getGreen()));
                    int blue = (int)((λ1*Color.red.getBlue()) + (λ2*Color.green.getBlue()) + (λ3*Color.blue.getBlue()));
                    Color blendedColor = new Color(red, green, blue);
                    image.setRGB(x, y, blendedColor.getRGB());
                }
            }
        }

        File outputfile = new File("Barycentric.jpg");
        try {
            ImageIO.write(image, "jpg", outputfile);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}