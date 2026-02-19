import java.awt.image.BufferedImage;

public class EditableImage {

    private final BufferedImage original;
    private BufferedImage edited;

    public EditableImage(BufferedImage original, BufferedImage edited) {
        this.original = original;
        this.edited = edited;
    }

    public BufferedImage getEditedImage() {
        return edited;
    }

    public void setEditedImage(BufferedImage edited) {
        this.edited = edited;
    }

    public BufferedImage getOriginalImage() {
        return original;
    }
}


