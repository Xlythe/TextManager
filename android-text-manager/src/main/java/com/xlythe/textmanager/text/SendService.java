package com.xlythe.textmanager.text;

import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.util.ApnDefaults;
import com.xlythe.textmanager.text.util.HttpUtils;

import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

public class SendService extends IntentService {
    private static final String TAG = SendService.class.getSimpleName();
    private static final String ACTION_SMS_SENT = "com.xlythe.textmanager.text.SMS_SENT";
    private static final String ACTION_SMS_DELIVERED = "com.xlythe.textmanager.text.SMS_DELIVERED";
    private static final String ACTION_MMS_SENT = "com.xlythe.textmanager.text.MMS_SENT";
    private static final String EXTRA_TEXT = "text";

    static void schedule(Context context, Text text) {
        Intent intent = new Intent(context, SendService.class);
        intent.putExtra(EXTRA_TEXT, text);
        context.startService(intent);
    }

    public SendService() {
        super("SendService");
    }

    @WorkerThread
    @Override
    protected void onHandleIntent(Intent intent) {
        Text text = intent.getParcelableExtra(EXTRA_TEXT);
        if (text.isMms()) {
            Uri uri = storeMMS(text);
            PendingIntent sentMmsPendingIntent = newMmsSentPendingIntent(this, uri, text.getId());
            if (Network.forceDataConnection(this)) {
                sendMMS(text, sentMmsPendingIntent);
            } else {
                sendIntent(sentMmsPendingIntent, Activity.RESULT_CANCELED);
            }
        } else {
            Uri uri = storeSMS(text);
            sendSMS(text, uri);
        }
    }

    private Uri storeSMS(Text text) {
        // Put together query data
        TextManager manager = TextManager.getInstance(this);
        String address = manager.getMembers(text).get().iterator().next().getNumber();
        ContentValues values = new ContentValues();
        Uri uri = Mock.Telephony.Sms.Sent.CONTENT_URI;
        values.put(Mock.Telephony.Sms.ADDRESS, address);
        values.put(Mock.Telephony.Sms.BODY, text.getBody());
        values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
        String clause = String.format("%s = %s", Mock.Telephony.Sms._ID, text.getId());

        // Check if message exists already, if so update the data
        int rowsUpdated = getContentResolver().update(uri, values, clause, null);

        // Nothing was updated so insert a new one
        if (rowsUpdated == 0) {
            uri = getContentResolver().insert(uri, values);
        } else {
            uri = Uri.withAppendedPath(uri, text.getId());
        }
        return uri;
    }

    private void sendSMS(Text text, Uri uri) {
        SmsManager sms = SmsManager.getDefault();
        TextManager manager = TextManager.getInstance(this);
        Contact contact = manager.getMembers(text).get().iterator().next();
        if (contact.equals(Contact.UNKNOWN)) {
            return;
        }
        String address = contact.getNumber();

        ArrayList<String> messages = sms.divideMessage(text.getBody());
        if (messages.size() > 1) {
            // TODO: not sure if this works for everyone
            ArrayList<PendingIntent> sendIntent = new ArrayList<>();
            ArrayList<PendingIntent> deliveryIntent = new ArrayList<>();
            sendIntent.add(newSmsSentPendingIntent(this, uri, text.getId()));
            deliveryIntent.add(newSmsDeliveredPendingIntent(this, uri, text.getId()));
            sms.sendMultipartTextMessage(address, null, sms.divideMessage(text.getBody()), sendIntent, deliveryIntent);
        } else {
            sms.sendTextMessage(address, null, text.getBody(), newSmsSentPendingIntent(this, uri, text.getId()), newSmsDeliveredPendingIntent(this, uri, text.getId()));
        }
    }

    @Nullable
    private Uri storeMMS(Text text) {
        PduPersister p = PduPersister.getPduPersister(this);

        Uri uri;
        String id = text.getId();
        if (Long.parseLong(id) != -1) {
            uri = Uri.withAppendedPath(Mock.Telephony.Mms.Sent.CONTENT_URI, id);
        } else {
            uri = Mock.Telephony.Mms.Sent.CONTENT_URI;
        }
        try {
            uri = p.persist(text.getSendRequest(this), uri, true, true, null);
        } catch (MmsException e) {
            Log.e(TAG, "persisting pdu failed", e);
            return null;
        }

        ContentValues values = new ContentValues();
        values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
        getContentResolver().update(uri, values, null, null);

        return uri;
    }

    /**
     * Send message
     */
    private void sendMMS(Text text, PendingIntent sentMmsPendingIntent) {
        TextManager manager = TextManager.getInstance(this);
        Contact contact = manager.getMembers(text).get().iterator().next();
        if (contact.equals(Contact.UNKNOWN)) {
            return;
        }
        try {
            ApnDefaults.ApnParameters apnParameters = ApnDefaults.getApnParameters(this);
            HttpUtils.httpConnection(
                    this, 4444L,
                    apnParameters.getMmscUrl(),
                    text.getByteData(this),
                    HttpUtils.HTTP_POST_METHOD,
                    apnParameters.isProxySet(),
                    apnParameters.getProxyAddress(),
                    apnParameters.getProxyPort());
            sendIntent(sentMmsPendingIntent, Activity.RESULT_OK);
        } catch(IOException e){
            Log.e(TAG, "Failed to connect to the MMS server", e);
            sendIntent(sentMmsPendingIntent, Activity.RESULT_CANCELED);
        }
    }

    /**
     * Notify receivers that something has happened with sending a message
     *
     * @param pendingIntent intent to notify
     * @param result result to be updated
     */
    private void sendIntent(PendingIntent pendingIntent, int result) {
        try {
            pendingIntent.send(result);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Failed to notified mms sent", e);
        }
    }

    /**
     * Pending Intents to notify when an MMS or SMS has been sent or failed to send
     */
    private static PendingIntent newSmsSentPendingIntent(Context context, Uri uri, String id) {
        Intent intent = new Intent(context, SmsSentReceiver.class);
        intent.setAction(ACTION_SMS_SENT);
        intent.setData(uri);
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private static PendingIntent newSmsDeliveredPendingIntent(Context context, Uri uri, String id) {
        Intent intent = new Intent(context, SmsDeliveredReceiver.class);
        intent.setAction(ACTION_SMS_DELIVERED);
        intent.setData(uri);
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private static PendingIntent newMmsSentPendingIntent(Context context, Uri uri, String id) {
        Intent intent = new Intent(context, MmsSentReceiver.class);
        intent.setAction(ACTION_MMS_SENT);
        intent.setData(uri);
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT);
    }

    /**
     * Receivers to update content provided when an MMS or SMS has been sent or failed to send
     */
    public static final class SmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri uri = intent.getData();
            ContentValues values = new ContentValues();
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Log.d(TAG, "SMS sent");
                    values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_COMPLETE);
                    break;
                default:
                    Log.d(TAG, "SMS failed to send");
                    values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
                    break;
            }
            context.getContentResolver().update(uri, values, null, null);
        }
    }

    public static final class SmsDeliveredReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Log.d(TAG, "SMS delivered");
                    break;
                case Activity.RESULT_CANCELED:
                    Log.d(TAG, "SMS not delivered");
                    break;
            }
        }
    }

    public static final class MmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri uri = intent.getData();
            ContentValues values = new ContentValues();
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Log.d(TAG, "MMS sent");
                    values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_COMPLETE);
                    break;
                default:
                    Log.d(TAG, "MMS failed to send");
                    values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
                    break;
            }
            context.getContentResolver().update(uri, values, null, null);
        }
    }
}
