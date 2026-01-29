package tools;

public abstract class IOHandler {
    public abstract void showInfo(String info);
    public abstract int getInt(String info, String notValidInput);
    public abstract float getFloat(String info, String notValidInput);
}
