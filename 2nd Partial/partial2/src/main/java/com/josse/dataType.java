package com.josse;

/**
 * Discriminates between static images and video clips throughout the pipeline.
 * Used to branch ffmpeg commands: photos need -loop 1 + -t, videos do not.
 */
public enum dataType {
    VIDEO,
    PHOTO
}