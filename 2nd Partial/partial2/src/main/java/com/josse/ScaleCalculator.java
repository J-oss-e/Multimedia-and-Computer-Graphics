package com.josse;

public class ScaleCalculator{

    private static final double width = 1080;
    private static final double height = 1920;

    public ScaleCalculator(){}

    public double[] portraitImage(double widthOrigen, double heightOrigen){
        double escalaX = ScaleCalculator.width/widthOrigen;
        double escalaY = ScaleCalculator.height/heightOrigen;

        return new double[]{escalaX, escalaY};
    }

    public static double getWidth(){
        return width;
    }
    public static double getHeight(){
        return height;
    }    
}