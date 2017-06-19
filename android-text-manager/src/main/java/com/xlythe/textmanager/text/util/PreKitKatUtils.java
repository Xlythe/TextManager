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

        // Before Android N, HTC required this hack. At least for the Pixel phones, this hack
        // is no longer needed. I'm going to assume it's fixed on all HTC phones until proven
        // otherwise.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                && manufacturer.equals(Mock.MANUFACTURER_HTC)) {
            return true;
        }

        return manufacturer.equals(Mock.MANUFACTURER_SAMSUNG)
                || manufacturer.equals(Mock.MANUFACTURER_ZTE)
                || manufacturer.equals(Mock.MANUFACTURER_SYMPHONY);
    }
}
