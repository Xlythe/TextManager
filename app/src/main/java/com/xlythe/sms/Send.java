package com.xlythe.sms;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.telephony.SmsManager;

/**
 * Created by Niko on 5/20/15.
 */
public class Send {
    /**
     * Sends a text message to supplied number
     * @param number Number of recipient
     * @param message Message the user wants to send
     */
    public static void sendSMS(Context context, String number, String message)
    {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(number, null, message, null, null);
        ContentValues values = new ContentValues();
        values.put("address", number);
        values.put("body", message);
        context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }
}
