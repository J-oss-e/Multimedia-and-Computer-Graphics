import java.util.Scanner;

public class CoordinatesCalculator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Coordinate Conversion Program");
        System.out.println("1. Polar to Cartesian");
        System.out.println("2. Cartesian to Polar");
        System.out.print("Choose an option (1 or 2): ");

        int option = scanner.nextInt();

        switch (option) {
            case 1:
                System.out.print("Enter radius (r): ");
                double r = scanner.nextDouble();

                System.out.print("Enter angle (theta in degrees): ");
                double thetaDegrees = scanner.nextDouble();

                double thetaRadians = Math.toRadians(thetaDegrees);

                double x = r * Math.cos(thetaRadians);
                double y = r * Math.sin(thetaRadians);

                System.out.printf("Cartesian Coordinates:%n");
                System.out.printf("x = %.4f%n", x);
                System.out.printf("y = %.4f%n", y);
                break;

            case 2:
                System.out.print("Enter x: ");
                double xVal = scanner.nextDouble();

                System.out.print("Enter y: ");
                double yVal = scanner.nextDouble();

                double radius = Math.sqrt(xVal * xVal + yVal * yVal);
                double theta = Math.toDegrees(Math.atan2(yVal, xVal));

                System.out.printf("Polar Coordinates:%n");
                System.out.printf("r = %.4f%n", radius);
                System.out.printf("theta = %.4f degrees%n", theta);
                break;

            default:
                System.out.println("Invalid option. Please choose 1 or 2.");
        }

        scanner.close();
    }
}
