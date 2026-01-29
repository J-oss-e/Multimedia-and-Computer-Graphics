package shapes;

import tools.IOHandler;

public abstract class Shape {
    IOHandler inputOutput;

    public Shape(IOHandler console){
        setInputOutput(console);
    }

    public IOHandler getInputOutput() {
        return inputOutput;
    }

    public void setInputOutput(IOHandler inputOutput) {
        this.inputOutput = inputOutput;
    }

    protected abstract void obtainParameters();
    public abstract float getArea();
    public abstract float getPerimeter();
}
