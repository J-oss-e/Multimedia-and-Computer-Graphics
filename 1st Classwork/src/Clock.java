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

        for (int i = 0; i < 12; i++) {
            double angulo = i * (Math.PI * 2 / 12);


            int puntoX = (int) (centerX + (radius - 10) * Math.cos(angulo));
            int puntoY = (int) (centerY + (radius - 10) * Math.sin(angulo));

            for(int x = -1; x <= 1; x++){
                for(int y = -1; y <= 1; y++){
                    image.setRGB(puntoX + x, puntoY + y, Color.white.getRGB());
                }
            }
        }

        double anguloHora = 3.6;
        int largoHora = 50;

        for (int r = 0; r < largoHora; r++) {
            int x = (int) (centerX + r * Math.cos(anguloHora));
            int y = (int) (centerY + r * Math.sin(anguloHora));
            image.setRGB(x, y, Color.white.getRGB());
        }

        double anguloMinuto = 5.2;
        int largoMinuto = 80;

        for (int r = 0; r < largoMinuto; r++) {
            int x = (int) (centerX + r * Math.cos(anguloMinuto));
            int y = (int) (centerY + r * Math.sin(anguloMinuto));
            image.setRGB(x, y, Color.white.getRGB());
        }

        File outputfile = new File("clock.jpg");
        try {
            ImageIO.write(image, "jpg", outputfile);
        }catch(IOException e){
            throw new RuntimeException(e);
        }

    }
}
