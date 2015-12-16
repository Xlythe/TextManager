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
        // TODO: This method is called when the BroadcastReceiver is receiving

        final Bundle bundle = intent.getExtras();

        try {
            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                SmsMessage[] messages = new SmsMessage[pdusObj.length];

                for (int i = 0; i < pdusObj.length; i++) {
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);

                    //Log.d("pdu", bytesToHex((byte[]) pdusObj[i]) +"");

                    messages[i] = currentMessage;

                    mNumber = currentMessage.getDisplayOriginatingAddress();
                    mMessage = currentMessage.getDisplayMessageBody();

//                    Log.i("SmsReciver", number + ": " + message);
//                    Toast.makeText(context, number + ": " + message, Toast.LENGTH_LONG).show();
                }

                if(Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) {
                    Receive.storeMessage(context, messages, 0);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.e("SmsReceiver", "Exception smsReceiver" + e);
        }
    }

    public String getNumber() {
        return mNumber;
    }

    public String getMessage() {
        return mMessage;
    }

    //    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
//    public static String bytesToHex(byte[] bytes) {
//        char[] hexChars = new char[bytes.length * 2];
//        for ( int j = 0; j < bytes.length; j++ ) {
//            int v = bytes[j] & 0xFF;
//            hexChars[j * 2] = hexArray[v >>> 4];
//            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
//        }
//        return new String(hexChars);
//    }
}
