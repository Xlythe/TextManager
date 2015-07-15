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

                // Get raw PDU push-data from the message and parse it
                byte[] pushData = intent.getByteArrayExtra("data");
                PduParser parser = new PduParser(pushData, PduParserUtil.shouldParseContentDisposition());
                GenericPdu pdu = parser.parse();

                if (null == pdu) {
                    Log.e(DEBUG_TAG, "Invalid PUSH data");
                }
                else {
                    Log.e(DEBUG_TAG, "PUSH data" + pdu);
                }

                NotificationInd nInd = (NotificationInd) pdu;

                PduPersister p = PduPersister.getPduPersister(mContext);
                ContentResolver cr = mContext.getContentResolver();
                int type = pdu.getMessageType();
                long threadId = -1;

                byte [] contentLocation = nInd.getContentLocation();
                if ('=' == contentLocation[contentLocation.length - 1]) {
                    byte [] transactionId = nInd.getTransactionId();
                    byte [] contentLocationWithId = new byte [contentLocation.length + transactionId.length];
                    System.arraycopy(contentLocation, 0, contentLocationWithId, 0, contentLocation.length);
                    System.arraycopy(transactionId, 0, contentLocationWithId, contentLocation.length, transactionId.length);
                    nInd.setContentLocation(contentLocationWithId);
                }

                Uri uri = p.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, false, false, null);

                Intent svc = new Intent(mContext, TransactionService.class);
                svc.putExtra(TransactionBundle.URI, uri.toString());
                svc.putExtra(TransactionBundle.TRANSACTION_TYPE, Transaction.NOTIFICATION_TRANSACTION);
                mContext.startService(svc);
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.e("MmsReceiver", "Exception mmsReceiver" + e);
        }
    }
}
