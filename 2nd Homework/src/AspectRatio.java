import java.util.Scanner;

public class AspectRatio {

    public static int getAspectRatio(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the width: ");
        int width = scanner.nextInt();

        System.out.print("Enter the height: ");
        int height = scanner.nextInt();

        int divisor = getAspectRatio(width, height);
        int aspectWidth = width / divisor;
        int aspectHeight = height / divisor;

        System.out.println("Aspect Ratio: " + aspectWidth + ":" + aspectHeight);

        scanner.close();
    }
}
