package com.xlythe.textmanager.text;

import static android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.Telephony;
import android.util.Log;


public class MmsReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getSimpleName();
    private Bitmap mBitmap;
    private Context mContext;
    private Intent mIntent;

    private class ReceivePushTask extends AsyncTask<Intent, Void, Void> {
        private OnReceiveCallback mOnReceive;
        public ReceivePushTask(OnReceiveCallback onReceive) {
            mOnReceive = onReceive;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
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
                                mBitmap = BitmapFactory.decodeByteArray(bitmapdata , 0, bitmapdata .length);
                                mOnReceive.onSuccess(mBitmap);
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
            return null;
        }
    }

    public void onMessageReceived(OnReceiveCallback onReceive){
        if (mIntent.getAction().equals(WAP_PUSH_DELIVER_ACTION) && ContentType.MMS_MESSAGE.equals(mIntent.getType())) {
            Log.v(TAG, "Received PUSH Intent: " + mIntent);

            // Hold a wake lock for 5 seconds, enough to give any
            // services we start time to take their own wake locks.
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS PushReceiver");
            wl.acquire(5000);
            new ReceivePushTask(onReceive).execute(mIntent);
        }
    }

    public interface OnReceiveCallback{
        void onSuccess(Bitmap bitmap);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
    }
}