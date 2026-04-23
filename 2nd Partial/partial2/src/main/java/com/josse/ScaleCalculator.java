package com.josse;

/**
 * Target resolution constants for the output video.
 * 1080x1920 is the 9:16 portrait ratio required by Instagram Reels,
 * TikTok, and YouTube Shorts.
 */
public class ScaleCalculator {

    // 9:16 portrait — do not change without updating the DALL-E prompt size too
    private static final double WIDTH = 1080;
    private static final double HEIGHT = 1920;

    public static double getWidth()  { return WIDTH; }
    public static double getHeight() { return HEIGHT; }
}