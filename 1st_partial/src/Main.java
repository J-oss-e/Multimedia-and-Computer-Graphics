import java.util.Scanner;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);

        ImageLoader loader = new ImageLoader();
        ImageOperations operations = new ImageOperations();

        BufferedImage original = loader.loadImage("C:\\Users\\Angel\\Documents\\Up ISGC\\4to Semestre\\Multimedia-and-Computer-Graphics\\1st_partial\\src\\img.png");
        BufferedImage copy = loader.createCopy(original);

        EditableImage editable = new EditableImage(original, copy);

        boolean running = true;

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
                    operations.invertColors(editable.getEditedImage());
                    System.out.println("Colors inverted.");
                    break;

                case 2:
                    System.out.println("Enter x y width height:");
                    int x = scanner.nextInt();
                    int y = scanner.nextInt();
                    int width = scanner.nextInt();
                    int height = scanner.nextInt();

                    BufferedImage cropped =
                            operations.cropImage(editable.getEditedImage(), x, y, width, height);

                    editable.setEditedImage(cropped);

                    System.out.println("Image cropped.");
                    break;

                case 3:
                    System.out.println("Enter x y width height:");
                    int rx = scanner.nextInt();
                    int ry = scanner.nextInt();
                    int rwidth = scanner.nextInt();
                    int rheight = scanner.nextInt();

                    System.out.println("Enter angle (90, 180, 270):");
                    int angle = scanner.nextInt();

                    operations.rotateImage(editable.getEditedImage(), rx, ry, rwidth, rheight, angle);

                    System.out.println("Region rotated.");
                    break;

                case 4:
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