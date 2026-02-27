import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageOperations {
    public static void invertColors(BufferedImage image, int x, int y, int width, int height) {

        if (x < 0 || y < 0 ||
                x + width > image.getWidth() ||
                y + height > image.getHeight()) {

            throw new IllegalArgumentException("Invert area out of bounds");
        }

        //Iterate through the pixel selected by user in the image
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                // Retrieve current pixel color
                Color C =  new Color(image.getRGB(x + i, y + j));
                // Create inverted color
                Color invertedC = new Color(255 - C.getRed(), 255 - C.getGreen(), 255 - C.getBlue());
                //Replace original pixel with inverted one
                image.setRGB(x + i, j+y, invertedC.getRGB());
            }
        }
    }

    public void rotateImage(BufferedImage editable, int x, int y, int width, int height, int angle) {

        BufferedImage image = editable;

        // Validate region bounds
        if (x < 0 || y < 0 ||
                x + width > image.getWidth() ||
                y + height > image.getHeight()) {

            throw new IllegalArgumentException("Rotation area out of bounds");
        }

        // Validate angle
        if (angle != 90 && angle != 180 && angle != 270) {
            throw new IllegalArgumentException("Angle must be 90, 180 or 270");
        }

        BufferedImage region = new BufferedImage(width, height, image.getType());

        // Extract region into temporary image
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                region.setRGB(i, j, image.getRGB(x + i, y + j));
            }
        }

        // Create rotated image with correct dimensions
        BufferedImage rotated;

        if (angle == 90 || angle == 270) {
            rotated = new BufferedImage(height, width, image.getType());
        } else {
            rotated = new BufferedImage(width, height, image.getType());
        }

        // Apply coordinate transformation
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                int pixel = region.getRGB(i, j);

                switch (angle) {

                    case 90:
                        rotated.setRGB(height - 1 - j, i, pixel);
                        break;

                    case 180:
                        rotated.setRGB(width - 1 - i, height - 1 - j, pixel);
                        break;

                    case 270:
                        rotated.setRGB(j, width - 1 - i, pixel);
                        break;
                }
            }
        }

        // Clear original region (fill with white)
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                image.setRGB(x + i, y + j, Color.white.getRGB());
            }
        }

        // Place rotated region back into original image
        for (int j = 0; j < rotated.getHeight(); j++) {
            for (int i = 0; i < rotated.getWidth(); i++) {

                if (x + i < image.getWidth() && y + j < image.getHeight()) {
                    image.setRGB(x + i, y + j, rotated.getRGB(i, j));
                }
            }
        }
    }

    /**
     * Crops a selected rectangular region from the image.
     * Creates a new BufferedImage with the selected dimensions
     * and manually copies each pixel from the original image.
     */
    public static BufferedImage cropImage(BufferedImage image, int x, int y, int width, int height) {

        BufferedImage cropped = new BufferedImage(width, height, image.getType());

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int originalPixel = image.getRGB(x + i, y + j);
                cropped.setRGB(i, j, originalPixel);
            }
        }

        return cropped;
    }

}
