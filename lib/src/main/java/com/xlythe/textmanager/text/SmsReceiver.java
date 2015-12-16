package com.xlythe.textmanager.text;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.xlythe.textmanager.MessageObserver;

import java.util.HashSet;
import java.util.Set;

public class SmsReceiver extends BroadcastReceiver {
    private String mNumber = "not set";
    private String mMessage = "not set";

    public SmsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        final Bundle bundle = intent.getExtras();

        if (bundle != null) {
            final Object[] pdusObj = (Object[]) bundle.get("pdus");
            SmsMessage[] messages = new SmsMessage[pdusObj.length];

            for (int i = 0; i < pdusObj.length; i++) {
                SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);

                messages[i] = currentMessage;

                mNumber = currentMessage.getDisplayOriginatingAddress();
                mMessage = currentMessage.getDisplayMessageBody();

            }

            if (Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) {
                Receive.storeMessage(context, messages, 0);
            }
        }
    }

    public String getNumber() {
        return mNumber;
    }

    public String getMessage() {
        return mMessage;
    }
}
