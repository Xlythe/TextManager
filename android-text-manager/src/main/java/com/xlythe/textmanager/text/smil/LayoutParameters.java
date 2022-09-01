package com.xlythe.textmanager.text.smil;

public interface LayoutParameters {
    /* Layouts type definition */
    int UNKNOWN        = -1;
    int HVGA_LANDSCAPE = 10;
    int HVGA_PORTRAIT  = 11;

    /* Parameters for known layouts */
    int HVGA_LANDSCAPE_WIDTH  = 480;
    int HVGA_LANDSCAPE_HEIGHT = 320;
    int HVGA_PORTRAIT_WIDTH   = 320;
    int HVGA_PORTRAIT_HEIGHT  = 480;

    /**
     * Get the width of current layout.
     */
    int getWidth();
    /**
     * Get the height of current layout.
     */
    int getHeight();
    /**
     * Get the width of the image region of current layout.
     */
    int getImageHeight();
    /**
     * Get the height of the text region of current layout.
     */
    int getTextHeight();
    /**
     * Get the type of current layout.
     */
    int getType();
    /**
     * Get the type description of current layout.
     */
    String getTypeDescription();
}