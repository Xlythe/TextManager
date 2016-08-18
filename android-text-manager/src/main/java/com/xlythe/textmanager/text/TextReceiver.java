package com.xlythe.textmanager.text;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.telephony.SmsMessage;
import android.util.Log;

import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.GenericPdu;
import com.xlythe.textmanager.text.pdu.NotificationInd;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.RetrieveConf;
import com.xlythe.textmanager.text.util.ContentType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static android.provider.Telephony.Sms.Intents.SMS_DELIVER_ACTION;
import static android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static android.provider.Telephony.Sms.Intents.getMessagesFromIntent;

public abstract class TextReceiver extends BroadcastReceiver {
    private static final String TAG = TextReceiver.class.getSimpleName();

    public static final String ACTION_TEXT_RECEIVED = "com.xlythe.textmanager.text.ACTION_TEXT_RECEIVED";
    public static final String EXTRA_TEXT = "text";

    private static final int TIMEOUT = 2; // Minutes

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive{action=" + intent.getAction() + "}");
        if (ACTION_TEXT_RECEIVED.equals(intent.getAction())) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TEXT_RECEIVED");
            wl.acquire(1000);
            onMessageReceived(context, (Text) intent.getParcelableExtra(EXTRA_TEXT));
        } else if (WAP_PUSH_DELIVER_ACTION.equals(intent.getAction()) && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            // Hold a wake lock for 5 seconds, enough to give any
            // services we start time to take their own wake locks.
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS PushReceiver");
            wl.acquire(5000);
            new ReceivePushTask(context).execute(intent);
        } else if (SMS_DELIVER_ACTION.equals(intent.getAction())) {
            SmsMessage[] messages = getMessagesFromIntent(intent);
            Text text = Receive.storeMessage(context, messages, 0);
            onMessageReceived(context, text);
        }
//        else if (android.os.Build.VERSION.SDK_INT < 19 && SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            //TODO: Build notifications for pre 19
//            onMessageReceived(context, text);
//        }
    }

    private class ReceivePushTask extends AsyncTask<Intent, Void, Void> {
        Context mContext;
        public ReceivePushTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS StoreMedia");
            wl.acquire();
            try {
                Intent intent = intents[0];

                byte[] pushData = intent.getByteArrayExtra("data");

                final PduParser parser = new PduParser(pushData, true);
                final GenericPdu pdu = parser.parse();
                NotificationInd notif = (NotificationInd) pdu;

                if (pdu == null) {
                    Log.e(TAG, "Invalid PUSH data");
                    return null;
                }

                byte[] location = notif.getContentLocation();
                final String loc = new String (location);
                final CountDownLatch pduDownloaded = new CountDownLatch(1);
                Network.forceDataConnection(mContext, new Network.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Download Success");
                        byte[] data = Receive.receive(mContext, loc);
                        if (data == null) {
                            onFail();
                            return;
                        }
                        RetrieveConf retrieveConf = (RetrieveConf) new PduParser(data, true).parse();
                        PduPersister persister = PduPersister.getPduPersister(mContext);
                        Uri msgUri;
                        try {
                            msgUri = persister.persist(retrieveConf, Mock.Telephony.Mms.Inbox.CONTENT_URI, true, true, null);

                            // Use local time instead of PDU time
                            ContentValues values = new ContentValues(1);
                            values.put(Mock.Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
                            mContext.getContentResolver().update(msgUri, values, null, null);

                            Cursor textCursor = mContext.getContentResolver().query(msgUri, null, null, null, null);
                            if (textCursor == null) return;
                            textCursor.moveToFirst();

                            final String[] mmsProjection = new String[]{
                                    BaseColumns._ID,
                                    Mock.Telephony.Mms.Part.CONTENT_TYPE,
                                    Mock.Telephony.Mms.Part.TEXT,
                                    Mock.Telephony.Mms.Part._DATA,
                                    Mock.Telephony.Mms.Part.MSG_ID
                            };
                            Uri mmsUri = Uri.withAppendedPath(Mock.Telephony.Mms.CONTENT_URI, "/part");
                            Cursor mmsCursor = mContext.getContentResolver().query(mmsUri, mmsProjection, null, null, null);
                            if (mmsCursor == null) return;
                            mmsCursor.moveToFirst();

                            Text text = new Text(textCursor, mmsCursor);
                            textCursor.close();
                            mmsCursor.close();
                            onMessageReceived(mContext, text);
                        } catch (MmsException e) {
                            Log.e(TAG, "Unable to persist message", e);
                            onFail();
                            return;
                        }
                        pduDownloaded.countDown();
                    }

                    @Override
                    public void onFail() {
                        PduPersister p = PduPersister.getPduPersister(mContext);
                        try {
                            Uri uri = p.persist(pdu, Mock.Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
                            // can't the status via pdu, so update with the content resolver
                            ContentValues values = new ContentValues(2);
                            values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
                            values.put(Mock.Telephony.Mms.DATE, System.currentTimeMillis());
                            mContext.getContentResolver().update(uri, values, null, null);

                        } catch (MmsException e) {
                            Log.e(TAG, "Persisting pdu failed", e);
                        }
                        pduDownloaded.countDown();
                    }
                });

                pduDownloaded.await(TIMEOUT, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                java.lang.Thread.currentThread().interrupt();
            } finally {
                wl.release();
            }
            return null;
        }
    }

    public abstract void onMessageReceived(Context context, Text text);
}
