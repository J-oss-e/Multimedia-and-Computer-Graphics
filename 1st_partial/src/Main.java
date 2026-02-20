import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        ImageLoader loader = new ImageLoader();

        BufferedImage original = loader.loadImage("C:\\Users\\Angel\\Documents\\Up ISGC\\4to Semestre\\Multimedia-and-Computer-Graphics\\1st_partial\\src\\img.png");
        BufferedImage copy = loader.createCopy(original);

        EditableImage editable = new EditableImage(original, copy);

        ImageOperations operations = new ImageOperations();
        operations.invertColors(editable.getEditedImage());

        loader.saveImage(editable.getEditedImage(), "test.jpg");

    }
}
