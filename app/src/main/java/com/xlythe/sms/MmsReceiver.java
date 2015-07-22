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
import android.os.PowerManager;
import android.provider.Telephony;
import android.util.Log;

public class MmsReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getSimpleName().toString();

    private class ReceivePushTask extends AsyncTask<Intent,Void,Void> {
        private Context mContext;
        public ReceivePushTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            Intent intent = intents[0];

            // Get raw PDU push-data from the message and parse it
            byte[] pushData = intent.getByteArrayExtra("data");
            PduParser parser = new PduParser(
                    pushData, true);
            GenericPdu pdu = parser.parse();

            if (null == pdu) {
                Log.e(TAG, "Invalid PUSH data");
                return null;
            }

            PduPersister p = PduPersister.getPduPersister(mContext);
            ContentResolver cr = mContext.getContentResolver();
            int type = pdu.getMessageType();
            long threadId = -1;

            try {
                switch (type) {
                    case MESSAGE_TYPE_DELIVERY_IND:
                    case MESSAGE_TYPE_READ_ORIG_IND: {
                        threadId = findThreadId(mContext, pdu, type);
                        if (threadId == -1) {
                            // The associated SendReq isn't found, therefore skip
                            // processing this PDU.
                            break;
                        }

                        Uri uri = p.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, true,
                                true, null);
                        // Update thread ID for ReadOrigInd & DeliveryInd.
                        ContentValues values = new ContentValues(1);
                        values.put(Telephony.Mms.THREAD_ID, threadId);
                        cr.update(uri, values, null, null);
                        break;
                    }
                    case MESSAGE_TYPE_NOTIFICATION_IND: {
                        NotificationInd nInd = (NotificationInd) pdu;

//                        if (MmsConfig.getTransIdEnabled()) {
//                            byte [] contentLocation = nInd.getContentLocation();
//                            if ('=' == contentLocation[contentLocation.length - 1]) {
//                                byte [] transactionId = nInd.getTransactionId();
//                                byte [] contentLocationWithId = new byte [contentLocation.length
//                                        + transactionId.length];
//                                System.arraycopy(contentLocation, 0, contentLocationWithId,
//                                        0, contentLocation.length);
//                                System.arraycopy(transactionId, 0, contentLocationWithId,
//                                        contentLocation.length, transactionId.length);
//                                nInd.setContentLocation(contentLocationWithId);
//                            }
//                        }

                        if (!isDuplicateNotification(mContext, nInd)) {
                            // Save the pdu. If we can start downloading the real pdu immediately,
                            // don't allow persist() to create a thread for the notificationInd
                            // because it causes UI jank.
                            Uri uri = p.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, false, true, null);

                            // Start service to finish the notification transaction.
                            Intent svc = new Intent(mContext, TransactionService.class);
                            svc.putExtra("uri", uri.toString());
                            svc.putExtra("type", 0);
                            mContext.startService(svc);
                        } else{
                            Log.v(TAG, "Skip downloading duplicate message: " + new String(nInd.getContentLocation()));
                        }
                        break;
                    }
                    default:
                        Log.e(TAG, "Received unrecognized PDU.");
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Unexpected RuntimeException.", e);
            }
            Log.v(TAG, "PUSH Intent processed.");
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
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MMS PushReceiver");
            wl.acquire(5000);
            new ReceivePushTask(context).execute(intent);
        }
    }

    private static long findThreadId(Context context, GenericPdu pdu, int type) {
        String messageId;

        if (type == MESSAGE_TYPE_DELIVERY_IND) {
            messageId = new String(((DeliveryInd) pdu).getMessageId());
        } else {
            messageId = new String(((ReadOrigInd) pdu).getMessageId());
        }

        StringBuilder sb = new StringBuilder('(');
        sb.append(Telephony.Mms.MESSAGE_ID);
        sb.append('=');
        sb.append(DatabaseUtils.sqlEscapeString(messageId));
        sb.append(" AND ");
        sb.append(Telephony.Mms.MESSAGE_TYPE);
        sb.append('=');
        sb.append(PduHeaders.MESSAGE_TYPE_SEND_REQ);
        // TODO ContentResolver.query() appends closing ')' to the selection argument
        // sb.append(')');

        Cursor cursor = context.getContentResolver().query(Telephony.Mms.CONTENT_URI, new String[]{Telephony.Mms.THREAD_ID},
                sb.toString(), null, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        return -1;
    }

    private static boolean isDuplicateNotification(
            Context context, NotificationInd nInd) {
        byte[] rawLocation = nInd.getContentLocation();
        if (rawLocation != null) {
            String location = new String(rawLocation);
            String selection = Telephony.Mms.CONTENT_LOCATION + " = ?";
            String[] selectionArgs = new String[] { location };
            Cursor cursor = context.getContentResolver().query(
                    Telephony.Mms.CONTENT_URI, new String[]{Telephony.Mms._ID},
                    selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        // We already received the same notification before.
                        return true;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return false;
    }
}
