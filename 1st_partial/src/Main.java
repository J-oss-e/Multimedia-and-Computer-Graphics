import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        int x = 200, y = 200, height = 200, width = 200, angle = 90;

        ImageLoader loader = new ImageLoader();

        BufferedImage original = loader.loadImage("C:\\Users\\Angel\\Documents\\Up ISGC\\4to Semestre\\Multimedia-and-Computer-Graphics\\1st_partial\\src\\img.png");
        BufferedImage copy = loader.createCopy(original);

        EditableImage editable = new EditableImage(original, copy);

        ImageOperations operations = new ImageOperations();
        operations.invertColors(editable.getEditedImage());
        operations.rotateImage(editable.getEditedImage(), x, y, width, height, angle);
        BufferedImage cropped = operations.cropImage(editable.getEditedImage(), x, y, width, height);
        editable.setEditedImage(cropped);

        loader.saveImage(editable.getEditedImage(), "test.jpg");

    }
}
