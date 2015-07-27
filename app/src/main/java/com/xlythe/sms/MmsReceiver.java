package com.xlythe.sms;

import static android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static com.xlythe.sms.PduHeaders.MESSAGE_TYPE_DELIVERY_IND;
import static com.xlythe.sms.PduHeaders.MESSAGE_TYPE_READ_ORIG_IND;
import static com.xlythe.sms.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Telephony;
import android.util.Log;

import java.io.IOException;

public class MmsReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getSimpleName();

    private class ReceivePushTask extends AsyncTask<Intent,Void,Void> {
        private Context mContext;
        public ReceivePushTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            Intent intent = intents[0];

            byte[] pushData = intent.getByteArrayExtra("data");
            String pd = new String(pushData);
            Log.d("pushData", pd+"");

            PduParser parser = new PduParser(pushData, true);
            GenericPdu pdu = parser.parse();

            if (null == pdu) {
                Log.e(TAG, "Invalid PUSH data");
                return null;
            }
            PduPersister p = PduPersister.getPduPersister(mContext);
            Uri uri=null;
            try {
                uri = p.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, false, true, null);
            } catch (Exception e){

            }

            TestMms test = new TestMms(mContext);
            try {
                byte[] resp = test.getPdu(uri);
                String resp2 = new String(resp);
                Log.d("resp", resp2+"");
                RetrieveConf retrieveConf = (RetrieveConf) new PduParser(resp, true).parse();
                if (null == retrieveConf) {
                    Log.d("receiver","failed");
                }
                PduPersister persister = PduPersister.getPduPersister(mContext);
                Uri msgUri;
                try {
                    msgUri = persister.persist(retrieveConf, Telephony.Mms.Inbox.CONTENT_URI, true, true, null);

                    // Use local time instead of PDU time
                    ContentValues values = new ContentValues(1);
                    values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
                    mContext.getContentResolver().update(
                            msgUri, values, null, null);
                } catch (Exception e){

                }
            }catch (IOException ioe){

            }
            return null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(WAP_PUSH_DELIVER_ACTION)  && ContentType.MMS_MESSAGE.equals(intent.getType())) {
                Log.v(TAG, "Received PUSH Intent: " + intent);

            // Hold a wake lock for 5 seconds, enough to give any
            // services we start time to take their own wake locks.
            PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS PushReceiver");
            wl.acquire(5000);
            new ReceivePushTask(context).execute(intent);
        }
    }
}
