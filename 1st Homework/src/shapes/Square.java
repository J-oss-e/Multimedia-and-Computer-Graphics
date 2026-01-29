package shapes;

import tools.IOHandler;

public class Square extends Shape {
    private float side;

    public Square(IOHandler console) {
        super(console);
    }

    public float getSide() {
        return side;
    }

    public void setSide(float side) {
        this.side = side;
    }

    @Override
    protected void obtainParameters() {
        setSide(getInputOutput().getFloat("Please provide the blablabla", "Input not valid"));
    }

    @Override
    public float getArea() {
        return 0;
    }

    @Override
    public float getPerimeter() {
        return 0;
    }
}
