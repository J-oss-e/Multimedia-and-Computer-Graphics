package tools;

import java.util.Scanner;

public class IOConsole extends IOHandler{
    private Scanner scanner;

    public IOConsole() {
        setScanner(new Scanner(System.in));
    }

    public Scanner getScanner() {
        return scanner;
    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }
    @Override
    public void showInfo(String info) {
        System.out.println(info);
    }

    @Override
    public int getInt(String info, String notValidInput) {
        boolean validInput = false;
        int data = 0;

        do{
            showInfo(info);
            try{
                String input = getScanner().nextLine();
                data = Integer.parseInt(input);
                validInput = true;
            } catch(Exception e){
                showInfo(notValidInput);
            }

        }while(!validInput);

        return data;
    }

    @Override
    public float getFloat(String info, String notValidInput) {
        boolean validInput = false;
        float data = 0;

        do{
            showInfo(info);
            try{
                String input = getScanner().nextLine();
                data = Float.parseFloat(input);
                validInput = true;
            } catch(Exception e){
                showInfo(notValidInput);
            }

        }while(!validInput);

        return data;
    }
}
