package com.xlythe.textmanager.text;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.util.ApnDefaults;
import com.xlythe.textmanager.text.util.HttpUtils;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    protected void onHandleIntent(Intent intent) {
        Text text = intent.getParcelableExtra(TEXT_EXTRA);

        // Send SMS
        if (!text.isMms()) {
            Uri uri = storeSMS(this, text);
            SendSMS(this, text, uri);
        }

        // Send MMS
        else {
            Uri uri = storeMMS(this, text);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                sendMMS(this, text, uri);
            } else {
                sendMMSLegacy(this, text, uri);
            }
        }
    }

    private static Uri storeSMS(Context context, final Text text) {
        // Put together query data
        String address = text.getMembers(context).get().iterator().next().getNumber(context).get();
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

    private static void SendSMS(Context context, Text text, Uri uri) {
        SmsManager sms = SmsManager.getDefault();
        String address = text.getMembers(context).get().iterator().next().getNumber(context).get();
        sms.sendTextMessage(address, null, text.getBody(), newSmsSentPendingIntent(context, uri, text.getId()), newSmsDeliveredPendingIntent(context, uri, text.getId()));
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

    @TargetApi(19)
    public static void sendMMSLegacy(final Context context, final Text text, final Uri uri) {
        final PendingIntent sentMmsPendingIntent = newMmsSentPendingIntent(context, uri, text.getId());

        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final int result = connMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableMMS");

        if (result != 0) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            final CountDownLatch latch = new CountDownLatch(1);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, Intent intent) {
                    String action = intent.getAction();
                    if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        return;
                    }

                    NetworkInfo mNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

                    if ((mNetworkInfo == null) || (mNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE_MMS)) {
                        return;
                    }

                    if (!mNetworkInfo.isConnected()) {
                        return;
                    } else {
                        Log.d(TAG, "mms connected");
                        new java.lang.Thread(new Runnable() {
                            public void run() {
                                sendData(context, text, sentMmsPendingIntent);
                                latch.countDown();
                            }
                        }).start();
                    }
                }
            };
            context.registerReceiver(receiver, filter);
            try {
                latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            context.unregisterReceiver(receiver);
        } else {
            Log.i(TAG, "mms already established");
            new java.lang.Thread(new Runnable() {
                public void run() {
                    sendData(context, text, sentMmsPendingIntent);
                }
            }).start();
        }
    }

    @TargetApi(21)
    public static void sendMMS(final Context context, final Text text, final Uri uri) {
        final PendingIntent sentMmsPendingIntent = newMmsSentPendingIntent(context, uri, text.getId());

        // Request a data connection
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        // Use a countdownlatch because this may never return, and we want to mark the MMS
        // as failed in that case.
        final CountDownLatch latch = new CountDownLatch(1);
        boolean success = false;
        Log.d(TAG, "Network callback");
        new java.lang.Thread(new Runnable() {
            public void run() {
                connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        latch.countDown();
                        ConnectivityManager.setProcessDefaultNetwork(network);
                        sendData(context, text, sentMmsPendingIntent);
                        connectivityManager.unregisterNetworkCallback(this);
                    }
                });
            }
        }).start();
        try {
            success = latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!success) {
            notify(sentMmsPendingIntent, Activity.RESULT_CANCELED);
        }
    }

    /**
     * Send message
     */
    public static void sendData(Context context, final Text text, final PendingIntent sentMmsPendingIntent) {
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
            notify(sentMmsPendingIntent, Activity.RESULT_OK);
        } catch(IOException e){
            Log.e(TAG, "Failed to connect to the MMS server", e);
            notify(sentMmsPendingIntent, Activity.RESULT_CANCELED);
        }
    }

    /**
     * Notify receivers that something has happened with sending a message
     *
     * @param pendingIntent intent to notify
     * @param result result to be updated
     */
    private static void notify(PendingIntent pendingIntent, int result) {
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
