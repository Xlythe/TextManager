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

public class SendService extends IntentService {
    private static final String TAG = SendService.class.getSimpleName();
    private static final String PREAMBLE = "com.xlythe.textmanager.text.";
    private static final String SMS_SENT = PREAMBLE + "SMS_SENT";
    private static final String SMS_DELIVERED = PREAMBLE + "SMS_DELIVERED";
    private static final String MMS_SENT = PREAMBLE + "MMS_SENT";
    public static final String TEXT_EXTRA = "text_extra";

    public SendService() {
        super("SendService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Text text = intent.getParcelableExtra(TEXT_EXTRA);

        final Uri uri;

        // Store SMS
        if (!text.isMms()) {
             uri = storeSMS(this, text);
        }

        // Store MMS
        else {
            uri = storeMMS(this, text);
        }

        intent.setData(uri);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Text text = intent.getParcelableExtra(TEXT_EXTRA);
        final Uri uri = intent.getData();

        // Send SMS
        if (!text.isMms()) {
            sendSMS(this, text, uri);
        }

        // Send MMS
        else {
            final PendingIntent sentMmsPendingIntent = newMmsSentPendingIntent(getBaseContext(), uri, text.getId());
            Network.forceDataConnection(this, new Network.Callback() {
                @Override
                public void onSuccess() {
                    sendMMS(getBaseContext(), text, sentMmsPendingIntent);
                }

                @Override
                public void onFail() {
                    sendIntent(sentMmsPendingIntent, Activity.RESULT_CANCELED);
                }
            });
        }
    }

    private static Uri storeSMS(Context context, final Text text) {
        // Put together query data
        TextManager manager = TextManager.getInstance(context);
        String address = manager.getMembers(text).get().iterator().next().getNumber();
        ContentValues values = new ContentValues();
        Uri uri = Mock.Telephony.Sms.Sent.CONTENT_URI;
        values.put(Mock.Telephony.Sms.ADDRESS, address);
        values.put(Mock.Telephony.Sms.BODY, text.getBody());
        values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
        String clause = String.format("%s = %s", Mock.Telephony.Sms._ID, text.getId());

        // Check if message exists already, if so update the data
        int rowsUpdated = context.getContentResolver().update(uri, values, clause, null);

        // Nothing was updated so insert a new one
        if (rowsUpdated == 0) {
            uri = context.getContentResolver().insert(uri, values);
        } else {
            uri = Uri.withAppendedPath(uri, text.getId());
        }
        return uri;
    }

    private static void sendSMS(Context context, Text text, Uri uri) {
        SmsManager sms = SmsManager.getDefault();
        TextManager manager = TextManager.getInstance(context);
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
            sendIntent.add(newSmsSentPendingIntent(context, uri, text.getId()));
            deliveryIntent.add(newSmsDeliveredPendingIntent(context, uri, text.getId()));
            sms.sendMultipartTextMessage(address, null, sms.divideMessage(text.getBody()), sendIntent, deliveryIntent);
        } else {
            sms.sendTextMessage(address, null, text.getBody(), newSmsSentPendingIntent(context, uri, text.getId()), newSmsDeliveredPendingIntent(context, uri, text.getId()));
        }
    }

    private static Uri storeMMS(Context context, final Text text) {
        PduPersister p = PduPersister.getPduPersister(context);

        Uri uri;
        String id = text.getId();
        if (Long.parseLong(id) != -1) {
            uri = Uri.withAppendedPath(Mock.Telephony.Mms.Sent.CONTENT_URI, id);
        } else {
            uri = Mock.Telephony.Mms.Sent.CONTENT_URI;
        }
        try {
            uri = p.persist(text.getSendRequest(context), uri, true, true, null);
        } catch (MmsException e) {
            Log.e(TAG, "persisting pdu failed", e);
            uri = null;
        }

        ContentValues values = new ContentValues();
        values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
        context.getContentResolver().update(uri, values, null, null);

        return uri;
    }

    /**
     * Send message
     */
    public static void sendMMS(Context context, final Text text, final PendingIntent sentMmsPendingIntent) {
        TextManager manager = TextManager.getInstance(context);
        Contact contact = manager.getMembers(text).get().iterator().next();
        if (contact.equals(Contact.UNKNOWN)) {
            return;
        }
        try {
            ApnDefaults.ApnParameters apnParameters = ApnDefaults.getApnParameters(context);
            HttpUtils.httpConnection(
                    context, 4444L,
                    apnParameters.getMmscUrl(),
                    text.getByteData(context),
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
    private static void sendIntent(PendingIntent pendingIntent, int result) {
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
        intent.setAction(SMS_SENT);
        intent.setData(uri);
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private static PendingIntent newSmsDeliveredPendingIntent(Context context, Uri uri, String id) {
        Intent intent = new Intent(context, SmsDeliveredReceiver.class);
        intent.setAction(SMS_DELIVERED);
        intent.setData(uri);
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private static PendingIntent newMmsSentPendingIntent(Context context, Uri uri, String id) {
        Intent intent = new Intent(context, MmsSentReceiver.class);
        intent.setAction(MMS_SENT);
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
