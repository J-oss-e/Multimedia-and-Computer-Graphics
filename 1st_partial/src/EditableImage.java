import java.awt.image.BufferedImage;

public class EditableImage {

    // Original image reference (never modified)
    private final BufferedImage original;
    // Editable copy that is modified during execution
    private BufferedImage edited;

    public EditableImage(BufferedImage original, BufferedImage edited) {
        this.original = original;
        this.edited = edited;
    }

    //Returns the current edited image.
    public BufferedImage getEditedImage() {
        return edited;
    }

    /**
     * Updates the edited image.
     *
     * This method is used when operations like crop
     * generate a completely new BufferedImage.
     */
    public void setEditedImage(BufferedImage edited) {
        this.edited = edited;
    }
    //Returns the original unmodified image.
    public BufferedImage getOriginalImage() {
        return original;
    }
}


