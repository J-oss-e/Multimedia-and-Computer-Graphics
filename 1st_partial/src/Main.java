import java.util.Scanner;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        // Scanner used to read user input from console
        Scanner scanner = new Scanner(System.in);

        // Object responsible for loading and saving images, and the other responsible for imageOperations
        ImageLoader loader = new ImageLoader();
        ImageOperations operations = new ImageOperations();

        // Load the original image from disk
        BufferedImage original = loader.loadImage("C:\\Users\\usuario\\Documents\\Up ISGC\\4to Semestre\\Multimedios y graficas\\1st_partial\\src\\img.png");
        // Create a copy of the original image that will be modified
        BufferedImage copy = loader.createCopy(original);

        // EditableImage encapsulates both original and edited images
        EditableImage editable = new EditableImage(original, copy);

        // Controls whether the program keeps running
        boolean running = true;

        // Main loop: allows the user to apply multiple operations
        while (running) {

            System.out.println("\nChoose an operation:");
            System.out.println("1 - Invert Colors");
            System.out.println("2 - Crop");
            System.out.println("3 - Rotate");
            System.out.println("4 - Save Image");
            System.out.println("5 - Exit");

            int choice = scanner.nextInt();

            switch (choice) {

                case 1:
                    // Inverts all colors of the edited image
                    operations.invertColors(editable.getEditedImage());
                    System.out.println("Colors inverted.");
                    break;

                case 2:
                    // Reads crop parameters from user
                    System.out.println("Enter x y width height:");
                    int x = scanner.nextInt();
                    int y = scanner.nextInt();
                    int width = scanner.nextInt();
                    int height = scanner.nextInt();

                    // Crop returns a new image, so we update the edited image
                    BufferedImage cropped =
                            operations.cropImage(editable.getEditedImage(), x, y, width, height);

                    editable.setEditedImage(cropped);

                    System.out.println("Image cropped.");
                    break;

                case 3:
                    // Reads rotation parameters
                    System.out.println("Enter x y width height:");
                    int rx = scanner.nextInt();
                    int ry = scanner.nextInt();
                    int rwidth = scanner.nextInt();
                    int rheight = scanner.nextInt();

                    System.out.println("Enter angle (90, 180, 270):");
                    int angle = scanner.nextInt();

                    // Rotates the specified region of the edited image
                    operations.rotateImage(editable.getEditedImage(), rx, ry, rwidth, rheight, angle);

                    System.out.println("Region rotated.");
                    break;

                case 4:
                    // Saves the current edited image to disk
                    loader.saveImage(editable.getEditedImage(), "output.jpg");
                    System.out.println("Image saved as output.jpg");
                    break;

                case 5:
                    running = false;
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }

        scanner.close();
    }
}