package com.xlythe.textmanager.text;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import com.xlythe.textmanager.text.util.ApnDefaults;
import com.xlythe.textmanager.text.util.HttpUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Receive {
    // Email Address Pattern.
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-]{1,256}" + "\\@" + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" + "\\." + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+"
    );

    // Name Address Email Pattern.
    private static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

    private static final String[] PROJECTION = new String[] {
            Mock.Telephony.Mms.CONTENT_LOCATION,
            Mock.Telephony.Mms.LOCKED
    };

    private static final int COLUMN_CONTENT_LOCATION = 0;

    /**
     * HTTP request to the MMSC database
     */
    protected static void getPdu(final Uri uri, final Context context, final DataCallback callback) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

            final NetworkRequest networkRequest = builder.build();
            connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                            connectivityManager.bindProcessToNetwork(network);
                        } else if (android.os.Build.VERSION.SDK_INT >= 21) {
                            ConnectivityManager.setProcessDefaultNetwork(network);
                        }
                        receive(context, uri, callback);
                        connectivityManager.unregisterNetworkCallback(this);
                    }
                }
            });
        } else {
            final int result = connectivityManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableMMS");
            if (result != 0) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();

                        if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                            return;
                        }

                        NetworkInfo mNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

                        if ((mNetworkInfo == null) || (mNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE_MMS)) {
                            return;
                        }

                        if (mNetworkInfo.isConnected()) {
                            receive(context, uri, callback);
                            context.unregisterReceiver(this);
                        }
                    }
                };
                context.getApplicationContext().registerReceiver(receiver, filter);
            } else {
                receive(context, uri, callback);
            }
        }
    }

    public static void receive(final Context context, Uri uri, final DataCallback callback){
        Cursor cursor = context.getContentResolver().query(uri, PROJECTION, null, null, null);

        final String url;

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    url = cursor.getString(COLUMN_CONTENT_LOCATION);
                } else if ((cursor.getCount() > 1) && cursor.moveToFirst()) {
                    Log.d("Receive", "unspecific column");
                    url = cursor.getString(COLUMN_CONTENT_LOCATION);
                } else {
                    Log.d("Receive", "count is not positive");
                    url = null;
                }
            } finally {
                cursor.close();
            }
        } else {
            Log.d("Receive", "no cursor");
            url = null;
        }

        if (url != null) {
            new java.lang.Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ApnDefaults.ApnParameters apnParameters = ApnDefaults.getApnParameters(context);
                        byte[] data = HttpUtils.httpConnection(
                                context, -1L,
                                url, null, HttpUtils.HTTP_GET_METHOD,
                                apnParameters.isProxySet(),
                                apnParameters.getProxyAddress(),
                                apnParameters.getProxyPort());
                        callback.onSuccess(data);
                    } catch (IOException ioe) {
                        Log.e("MMS", "download failed due to network");
                        callback.onFail();
                    }
                }
            }).start();
        }
    }

    public interface DataCallback{
        void onSuccess(byte[] result);
        void onFail();
    }

    /**
     * Store message in the content provider.
     * @param context Context
     * @param msgs Array of SMS Messages
     * @param error Error code
     */
    public static void storeMessage(Context context, SmsMessage[] msgs, int error) {
        SmsMessage sms = msgs[0];

        // Add everything but the message body
        ContentValues values = extractContentValues(sms);
        values.put(Mock.Telephony.Sms.ERROR_CODE, error);

        // Add the message body
        StringBuilder body = new StringBuilder();
        for(int i = 0; i < msgs.length; i++) {
            sms = msgs[i];
            if(sms.getDisplayMessageBody() != null) {
                body.append(sms.getDisplayMessageBody());
            }
        }
        values.put(Mock.Telephony.Sms.Inbox.BODY, replaceFormFeeds(body.toString()));

        // Make sure there is a thread id.
        Long threadId = values.getAsLong(Mock.Telephony.Sms.THREAD_ID);
        String address = values.getAsString(Mock.Telephony.Sms.ADDRESS);

        // If it doesn't exist, create a thread id.
        if(((threadId == null) || (threadId == 0)) && (address != null)) {
            threadId = getOrCreateThreadId(context, address);
            values.put(Mock.Telephony.Sms.THREAD_ID, threadId);
        }

        // Add to content provider
        context.getContentResolver().insert(Mock.Telephony.Sms.Inbox.CONTENT_URI, values);
    }

    /**
     * Some providers send formfeeds in their messages. Convert those formfeeds to newlines.
     * @param s String of the message body
     * @return The message with newlines instead of formfeeds
     */
    private static String replaceFormFeeds(String s) {
        return s == null ? "" : s.replace('\f', '\n');
    }

    /**
     * Adds values except for message body.
     * @param sms SMS message
     * @return Content values
     */
    private static ContentValues extractContentValues(SmsMessage sms) {
        ContentValues values = new ContentValues();
        values.put(Mock.Telephony.Sms.Inbox.ADDRESS, sms.getDisplayOriginatingAddress());
        values.put(Mock.Telephony.Sms.Inbox.DATE, checkDate(sms));
        values.put(Mock.Telephony.Sms.Inbox.DATE_SENT, sms.getTimestampMillis());
        values.put(Mock.Telephony.Sms.Inbox.PROTOCOL, sms.getProtocolIdentifier());
        values.put(Mock.Telephony.Sms.Inbox.READ, 0);
        values.put(Mock.Telephony.Sms.Inbox.SEEN, 0);
        if(sms.getPseudoSubject().length() > 0) {
            values.put(Mock.Telephony.Sms.Inbox.SUBJECT, sms.getPseudoSubject());
        }
        values.put(Mock.Telephony.Sms.Inbox.REPLY_PATH_PRESENT, sms.isReplyPathPresent() ? 1 : 0);
        values.put(Mock.Telephony.Sms.Inbox.SERVICE_CENTER, sms.getServiceCenterAddress());
        return values;
    }

    /**
     * Check to make sure the date isn't fake
     * @param sms SMS message
     * @return date
     */
    private static long checkDate(SmsMessage sms){
        Calendar buildDate = new GregorianCalendar(2011, 8, 18); // 18 Sep 2011
        Calendar nowDate = new GregorianCalendar();
        long now = System.currentTimeMillis();
        nowDate.setTimeInMillis(now);
        if(nowDate.before(buildDate)) {
            now = sms.getTimestampMillis();
        }
        return now;
    }

    /**
     * Create thread id.
     * @param context Context
     * @param recip Recipient
     * @return Thread id
     */
    public static long getOrCreateThreadId(Context context, String recip) {
        Set<String> recipients = new HashSet<>();
        recipients.add(recip);
        Uri.Builder uriBuilder = Uri.parse("content://mms-sms/threadID").buildUpon();
        for (String recipient : recipients) {
            if (isEmailAddress(recipient)) {
                recipient = extractAddrSpec(recipient);
            }
            uriBuilder.appendQueryParameter("recipient", recipient);
        }

        Uri uri = uriBuilder.build();
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"_id"}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                } else {

                }
            } finally {
                cursor.close();
            }
        }

        Random random = new Random();
        return random.nextLong();
    }

    /**
     * Check if it is an email address
     * @param address Address
     * @return Boolean
     */
    private static boolean isEmailAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }
        String s = extractAddrSpec(address);
        Matcher match = EMAIL_ADDRESS_PATTERN.matcher(s);
        return match.matches();
    }

    /**
     * Get the name of the address
     * @param address Address
     * @return String
     */
    private static String extractAddrSpec(String address) {
        Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

        if (match.matches()) {
            return match.group(2);
        }
        return address;
    }
}