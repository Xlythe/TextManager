package com.xlythe.textmanager.text.util;

import android.os.Build;

import com.xlythe.textmanager.text.Mock;

public class PreKitKatUtils {

    /**
     * Check if devices are using unsupported APIs
     * As of now this is all Samsung, HTC, and ZTE manufactured phones
     */
    public static boolean requiresKitKatApis() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.equals(Mock.MANUFACTURER_SAMSUNG)
                || manufacturer.equals(Mock.MANUFACTURER_HTC)
                || manufacturer.equals(Mock.MANUFACTURER_ZTE)
                || manufacturer.equals(Mock.MANUFACTURER_SYMPHONY);
    }
}
