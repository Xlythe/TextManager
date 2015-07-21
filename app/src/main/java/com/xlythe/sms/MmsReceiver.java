package com.xlythe.sms;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

import java.util.HashMap;

public class MmsReceiver extends BroadcastReceiver {
    private final String DEBUG_TAG = getClass().getSimpleName().toString();
    private Context mContext;

    public MmsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        mContext = context;
        final Bundle bundle = intent.getExtras();
        Log.d(DEBUG_TAG, "bundle " + bundle);

        try {
            if (bundle != null) {

//                byte[] buffer = bundle.getByteArray("data");
//                Log.d(DEBUG_TAG, "buffer " + buffer);
//                String incomingNumber = new String(buffer);
//                int indx = incomingNumber.indexOf("/TYPE");
//                if (indx > 0 && (indx - 15) > 0) {
//                    int newIndx = indx - 15;
//                    incomingNumber = incomingNumber.substring(newIndx, indx);
//                    indx = incomingNumber.indexOf("+");
//                    if (indx > 0) {
//                        incomingNumber = incomingNumber.substring(indx);
//                        Log.d(DEBUG_TAG, "Mobile Number: " + incomingNumber);
//                    }
//                }
//
//                int transactionId = bundle.getInt("transactionId");
//                Log.d(DEBUG_TAG, "transactionId " + transactionId);
//
//                int pduType = bundle.getInt("pduType");
//                Log.d(DEBUG_TAG, "pduType " + pduType);
//
//                byte[] buffer2 = bundle.getByteArray("header");
//                String header = new String(buffer2);
//                Log.d(DEBUG_TAG, "header " + header);
//
//                byte[] buffer3 = bundle.getByteArray("data");
//                String data = new String(buffer3);
//                Log.d(DEBUG_TAG, "data " + data);

                // Get raw PDU push-data from the message and parse it.
                byte[] pushData = intent.getByteArrayExtra("data");
                PduParser parser = new PduParser(pushData, true);
                GenericPdu pdu = parser.parse();

                if (null == pdu) {
                    Log.e(DEBUG_TAG, "Invalid PUSH data");
                }
                else {
                    Log.d(DEBUG_TAG, "success");
                }

                PduPersister p = PduPersister.getPduPersister(mContext);
                Uri uri = p.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, false, false, null);
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.e("MmsReceiver", "Exception mmsReceiver" + e);
        }
    }
}
