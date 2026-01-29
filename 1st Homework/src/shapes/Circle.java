package shapes;

import tools.IOHandler;

public class Circle  extends Shape {
    private float radius;

    public Circle(IOHandler console) {
        super(console);
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    protected void obtainParameters() {
        setRadius(getInputOutput().getFloat("Enter Radius:", "Invalid input" ));

    }

    @Override
    public float getPerimeter() {
        return 2f * (float) Math.PI * getRadius();
    }

    @Override
    public float getArea() {
        return (float) Math.PI * getRadius() * getRadius();
    }
}
