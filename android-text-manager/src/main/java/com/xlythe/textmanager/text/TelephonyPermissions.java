package com.xlythe.textmanager.text;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/** Centralizes runtime permission checks used by background telephony work. */
final class TelephonyPermissions {
    private TelephonyPermissions() {}

    static boolean has(Context context, String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return context.getPackageManager().checkPermission(permission, context.getPackageName())
                    == PackageManager.PERMISSION_GRANTED;
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean canSendSms(Context context) {
        return has(context, Manifest.permission.SEND_SMS);
    }

    static boolean canReceiveMms(Context context) {
        return has(context, Manifest.permission.RECEIVE_MMS);
    }

    static boolean canReadPhoneNumber(Context context) {
        if (has(context, Manifest.permission.READ_SMS)
                || has(context, Manifest.permission.READ_PHONE_STATE)) {
            return true;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && has(context, Manifest.permission.READ_PHONE_NUMBERS);
    }
}
