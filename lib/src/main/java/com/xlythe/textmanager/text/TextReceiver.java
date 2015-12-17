package com.xlythe.textmanager.text;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import static android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static android.provider.Telephony.Sms.Intents.SMS_DELIVER_ACTION;

/**
 * Created by Niko on 12/16/15.
 */
public abstract class TextReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WAP_PUSH_DELIVER_ACTION.equals(intent.getAction()) && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            Log.v(TAG, "Received PUSH Intent: " + intent);

            // Hold a wake lock for 5 seconds, enough to give any
            // services we start time to take their own wake locks.
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS PushReceiver");
            wl.acquire(5000);
            new ReceivePushTask(context).execute(intent);
        }
        else if (SMS_DELIVER_ACTION.equals(intent.getAction())) {
            final Bundle bundle = intent.getExtras();

            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                SmsMessage[] messages = new SmsMessage[pdusObj.length];

                for (int i = 0; i < pdusObj.length; i++) {
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);

                    messages[i] = currentMessage;

                    String number = currentMessage.getDisplayOriginatingAddress();
                    String message = currentMessage.getDisplayMessageBody();
                    onMessageReceived(context, new Text.Builder()
                            .message(message)
                            .recipient(number)
                            .build());

                }
                Receive.storeMessage(context, messages, 0);
            }
        }
    }

    private class ReceivePushTask extends AsyncTask<Intent, Void, Void> {
        Context mContext;
        public ReceivePushTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS StoreMedia");
            wl.acquire();
            Intent intent = intents[0];

            byte[] pushData = intent.getByteArrayExtra("data");
            String pd = new String(pushData);
            Log.d("pushData", pd + "");

            final PduParser parser = new PduParser(pushData, true);
            GenericPdu pdu = parser.parse();

            if (null == pdu) {
                Log.e(TAG, "Invalid PUSH data");
                return null;
            }
            PduPersister p = PduPersister.getPduPersister(mContext);
            Uri uri = null;
            try {
                uri = p.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, false, true, null);
            } catch (MmsException e) {

            }

            Receive.getPdu(uri, mContext, new Receive.DataCallback(){
                @Override
                public void onSuccess(byte[] result){
                    RetrieveConf retrieveConf = (RetrieveConf) new PduParser(result, true).parse();
                    if (null == retrieveConf) {
                        Log.d("receiver", "failed");
                    }
                    PduBody body;
                    if (retrieveConf != null) {
                        body = retrieveConf.getBody();
                        // Start saving parts if necessary.
                        if (body != null) {
                            int partsNum = body.getPartsNum();
                            if (partsNum > 2) {
                                // mms
                            }
                            for (int i = 0; i < partsNum; i++) {
                                PduPart part = body.getPart(i);
                                byte[] bitmapdata = part.getData();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapdata, 0, bitmapdata.length);
                                onMessageReceived(mContext, new Text.Builder()
                                        .message("")
                                        .recipient("")
                                        .attach(bitmap)
                                        .build());
                            }
                        }
                    }

                    PduPersister persister = PduPersister.getPduPersister(mContext);
                    Uri msgUri;
                    try {
                        msgUri = persister.persist(retrieveConf, Telephony.Mms.Inbox.CONTENT_URI, true, true, null);

                        // Use local time instead of PDU time
                        ContentValues values = new ContentValues(1);
                        values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
                        mContext.getContentResolver().update(msgUri, values, null, null);
                    } catch (MmsException e) {

                    }
                }
            });
            wl.release();
            return null;
        }
    }

    public abstract void onMessageReceived(Context context, Text text);
}
