package com.xlythe.textmanager.text;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.provider.BaseColumns;
import android.telephony.SmsMessage;
import android.util.Log;

import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.GenericPdu;
import com.xlythe.textmanager.text.pdu.NotificationInd;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.RetrieveConf;
import com.xlythe.textmanager.text.util.ContentType;

import androidx.annotation.WorkerThread;

import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.SMS_DELIVER_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.getMessagesFromIntent;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

public abstract class TextReceiver extends BroadcastReceiver {
    private static final String TAG = TextReceiver.class.getSimpleName();
    private static final String TAG_TEXT = "sms:text";
    private static final String TAG_MMS = "sms:mms";

    private static final long TIMEOUT_TEXT = 1000;
    private static final long TIMEOUT_MMS = 5000;

    public static final String ACTION_TEXT_RECEIVED = "com.xlythe.textmanager.text.ACTION_TEXT_RECEIVED";
    public static final String EXTRA_TEXT = "text";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive{action=" + intent.getAction() + "}");
        if (ACTION_TEXT_RECEIVED.equals(intent.getAction())) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_TEXT);
            wl.acquire(TIMEOUT_TEXT);
            onMessageReceived(context, intent.getParcelableExtra(EXTRA_TEXT));
        } else if (WAP_PUSH_DELIVER_ACTION.equals(intent.getAction()) && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            // Hold a wake lock for 5 seconds, enough to give any
            // services we start time to take their own wake locks.
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_MMS);
            wl.acquire(TIMEOUT_MMS);

            // TODO(will): This should be a foreground service. We have no guarantee that the process
            // will stay alive long enough to download this otherwise.
            new DownloadMmsTask(context).execute(intent);
        } else if (SMS_DELIVER_ACTION.equals(intent.getAction())) {
            SmsMessage[] messages = getMessagesFromIntent(intent);
            Text text = Receive.storeMessage(context, messages, 0);
            onMessageReceived(context, text);
        } else if (Build.VERSION.SDK_INT < 19 && SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            // This is purely to display the notification! because main sms app stores the sms
            SmsMessage[] messages = getMessagesFromIntent(intent);
            onMessageReceived(context, new Text.Converter().toText(context, messages));
        }
    }

    private class DownloadMmsTask extends AsyncTask<Intent, Void, Void> {
        private final Context mContext;

        DownloadMmsTask(Context context) {
            mContext = context;
        }

        @WorkerThread
        @Override
        protected Void doInBackground(Intent... intents) {
            PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_MMS);

            try {
                wakelock.acquire(TIMEOUT_MMS);

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

                if (!Network.forceDataConnection(mContext)) {
                    markAsFailed(pdu);
                    return null;
                }

                byte[] data = Receive.receive(mContext, loc);
                if (data == null) {
                    markAsFailed(pdu);
                    return null;
                }

                RetrieveConf retrieveConf = (RetrieveConf) new PduParser(data, true).parse();
                PduPersister persister = PduPersister.getPduPersister(mContext);
                Uri msgUri;
                try {
                    msgUri = persister.persist(retrieveConf, Mock.Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
                } catch (MmsException e) {
                    markAsFailed(pdu);
                    return null;
                }

                // Use local time instead of PDU time
                ContentValues values = new ContentValues(1);
                values.put(Mock.Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
                mContext.getContentResolver().update(msgUri, values, null, null);

                Cursor textCursor = mContext.getContentResolver().query(msgUri, null, null, null, null);
                if (textCursor == null) {
                    markAsFailed(pdu);
                    return null;
                }
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
                if (mmsCursor == null) {
                    textCursor.close();
                    markAsFailed(pdu);
                    return null;
                }

                mmsCursor.moveToFirst();

                Text text = new Text(textCursor, mmsCursor);
                textCursor.close();
                mmsCursor.close();
                onMessageReceived(mContext, text);
            } finally {
                wakelock.release();
            }

            return null;
        }

        private void markAsFailed(GenericPdu pdu) {
            try {
                PduPersister p = PduPersister.getPduPersister(mContext);
                Uri uri = p.persist(pdu, Mock.Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
                // can't the status via pdu, so update with the content resolver
                ContentValues values = new ContentValues(2);
                values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
                values.put(Mock.Telephony.Mms.DATE, System.currentTimeMillis());
                mContext.getContentResolver().update(uri, values, null, null);
            } catch (MmsException e) {
                Log.e(TAG, "Persisting pdu failed", e);
            }
        }
    }

    public abstract void onMessageReceived(Context context, Text text);
}
