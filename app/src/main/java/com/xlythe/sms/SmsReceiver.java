package com.xlythe.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {
    public SmsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //throw new UnsupportedOperationException("Not yet implemented");

        final Bundle bundle = intent.getExtras();

        try {
            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                SmsMessage[] messages = new SmsMessage[pdusObj.length];

                for (int i = 0; i < pdusObj.length; i++) {
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);

                    messages[i] = currentMessage;

                    String number = currentMessage.getDisplayOriginatingAddress();
                    String message = currentMessage.getDisplayMessageBody();

                    Log.i("SmsReciver", number + ": " + message);
                    Toast.makeText(context, number + ": " + message, Toast.LENGTH_LONG).show();
                }

                if(Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) {
                    Receive.storeMessage(context, messages, 0);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.e("SmsReciver", "Exception smsReciver" + e);
        }
    }
}
