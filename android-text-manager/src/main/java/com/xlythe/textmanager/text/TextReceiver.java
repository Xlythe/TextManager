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
import android.provider.MediaStore;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.util.ContentType;
import com.xlythe.textmanager.text.pdu.GenericPdu;
import com.xlythe.textmanager.text.pdu.PduBody;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPart;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.RetrieveConf;
import com.xlythe.textmanager.text.util.EncodedStringValue;

import java.util.Objects;

import static android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
import static android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static android.provider.Telephony.Sms.Intents.SMS_DELIVER_ACTION;
import static android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;
import static android.provider.Telephony.Sms.Intents.getMessagesFromIntent;

public abstract class TextReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction()) || WAP_PUSH_DELIVER_ACTION.equals(intent.getAction()))
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            Log.v(TAG, "Received PUSH Intent: " + intent);

            // Hold a wake lock for 5 seconds, enough to give any
            // services we start time to take their own wake locks.
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS PushReceiver");
            wl.acquire(5000);
            new ReceivePushTask(context).execute(intent);
        } else if (SMS_DELIVER_ACTION.equals(intent.getAction())) {
            SmsMessage[] messages = getMessagesFromIntent(intent);
            Receive.storeMessage(context, messages, 0);
            buildNotification(context, intent);
        } else if (android.os.Build.VERSION.SDK_INT < 19 && SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            buildNotification(context, intent);
        }
    }

    public void buildNotification(Context context, Intent intent) {
        SmsMessage[] messages = getMessagesFromIntent(intent);
        for (SmsMessage currentMessage : messages) {
            String number = currentMessage.getDisplayOriginatingAddress();
            String message = currentMessage.getDisplayMessageBody();
            onMessageReceived(context, new NotificationText(number, message, null));
        }
    }

    public void buildMmsNotification(Context context, RetrieveConf retrieveConf) {
        if (retrieveConf != null) {
            PduBody body = retrieveConf.getBody();
            EncodedStringValue encodedSender = retrieveConf.getFrom();
            String sender = encodedSender.toString();
            if (body != null) {
                int partsNum = body.getPartsNum();
                for (int i = 0; i < partsNum; i++) {
                    PduPart part = body.getPart(i);
                    byte[] bitmapdata = part.getData();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapdata, 0, bitmapdata.length);
                    onMessageReceived(context, new NotificationText(sender, "", bitmap));
                }
            }
        }
    }

    public static class NotificationText {
        private String mSender;
        private String mMessage;
        private Bitmap mBitmap;
        protected NotificationText(String sender, String message, Bitmap bitmap) {
            mSender = sender;
            mMessage = message;
            mBitmap = bitmap;
        }

        public String getSender() {
            return mSender;
        }

        public String getMessage() {
            return mMessage;
        }

        public Bitmap getBitmap() {
            return mBitmap;
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
            final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS StoreMedia");
            wl.acquire();
            Intent intent = intents[0];

            byte[] pushData = intent.getByteArrayExtra("data");

            final PduParser parser = new PduParser(pushData, true);
            GenericPdu pdu = parser.parse();

            if (pdu == null) {
                Log.e(TAG, "Invalid PUSH data");
                return null;
            }

            try {
                final PduPersister persister = PduPersister.getPduPersister(mContext);
                Uri uri;
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    uri = persister.persist(pdu, Mock.Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
                } else {
                    uri = Mock.Telephony.Mms.Inbox.CONTENT_URI;
                }
                Receive.getPdu(uri, mContext, new Receive.DataCallback() {
                    @Override
                    public void onSuccess(byte[] result) {
                        RetrieveConf retrieveConf = (RetrieveConf) new PduParser(result, true).parse();
                        if (android.os.Build.VERSION.SDK_INT >= 19) {
                            try {
                                Uri msgUri = persister.persist(retrieveConf, Mock.Telephony.Mms.Inbox.CONTENT_URI, true, true, null);

                                // Use local time instead of PDU time
                                ContentValues values = new ContentValues(1);
                                values.put(Mock.Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
                                mContext.getContentResolver().update(msgUri, values, null, null);
                            } catch (MmsException e) {
                                Log.e("MMS", "unable to persist message");
                                onFail();
                            }
                        }
                        buildMmsNotification(mContext, retrieveConf);
                        wl.release();
                    }

                    @Override
                    public void onFail() {
                        // this maybe useful
                        wl.release();
                    }
                });
            } catch (MmsException e) {
                Log.e("Text Receiver","persisting pdu failed");
                e.printStackTrace();
            }

            return null;
        }
    }

    public abstract void onMessageReceived(Context context, NotificationText text);
}
