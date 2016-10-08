package com.xlythe.textmanager.text.util;

import com.xlythe.textmanager.text.Mock;

import static android.os.Build.MANUFACTURER;

/**
 * Created by Niko on 8/18/16.
 */

public class PreKitKatUtils {

    /**
     * Check if devices are using unsupported APIs
     * As of now this is all Samsung, HTC, and ZTE manufactured phones
     */
    public static boolean requiresKitKatApis() {
        String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
        return manufacturer.equals(Mock.MANUFACTURER_SAMSUNG)
                || manufacturer.equals(Mock.MANUFACTURER_HTC)
                || manufacturer.equals(Mock.MANUFACTURER_ZTE)
                || manufacturer.equals(Mock.MANUFACTURER_SYMPHONY);
    }
}
