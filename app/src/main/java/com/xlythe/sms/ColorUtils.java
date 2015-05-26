package com.xlythe.sms;

/**
 * Created by Niko on 5/26/15.
 */
public class ColorUtils {
    public static int getColor(String threadId) {
        int num = Integer.parseInt(threadId) % 13;
        switch (num) {
            case 0:
                return 0xffdb4437;
            case 1:
                return 0xffe91e63;
            case 2:
                return 0xff9c27b0;
            case 3:
                return 0xff673ab7;
            case 4:
                return 0xff3f51b5;
            case 5:
                return 0xff039be5;
            case 6:
                return 0xff4285f4;
            case 7:
                return 0xff0097a7;
            case 8:
                return 0xff009688;
            case 9:
                return 0xff0f9d58;
            case 10:
                return 0xff689f38;
            case 11:
                return 0xffef6c00;
            case 12:
                return 0xffff5722;
            default:
                return 0xff757575;
        }
    }

    public static int getDarkColor(String threadId) {
        int num = Integer.parseInt(threadId) % 13;
        switch (num) {
            case 0:
                return 0xffaf362c;
            case 1:
                return 0xffc2185b;
            case 2:
                return 0xff7b1fa2;
            case 3:
                return 0xff512da8;
            case 4:
                return 0xff303f9f;
            case 5:
                return 0xff0277bd;
            case 6:
                return 0xff346ac3;
            case 7:
                return 0xff00838f;
            case 8:
                return 0xff00796b;
            case 9:
                return 0xff0c7d46;
            case 10:
                return 0xff558b2f;
            case 11:
                return 0xffe65100;
            case 12:
                return 0xffe64a19;
            default:
                return 0xff424242;
        }
    }
}
