package com.xlythe.sms;

import android.telephony.SmsManager;

/**
 * Util methods for PduParser
 */
public class PduParserUtil {
    /**
     * Get the config of whether Content-Disposition header is supported
     * for default carrier using new SmsManager API
     *
     * @return true if supported, false otherwise
     */
    public static boolean shouldParseContentDisposition() {
        return true;
    }
}