import tools.IOConsole;
import shapes.Shape;

public class Main {

    public static void main(String[] args) {
        IOHandler console = new IOConsole();
        console.showInfo("Welcome to the Shape calculator!");
        int option = console.getInt("1) Square\n2) Circle\n3) Triangle\n4) ", "Invalid Option");
        Shape shape = null;

        switch(option){
            case 1:
                shape = new Square(console);
                break;
            case 2:
                shape = new Circle(console);
                break;
        }


    }
}