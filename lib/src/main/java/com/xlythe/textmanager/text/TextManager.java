package com.xlythe.textmanager.text;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.User;
import com.xlythe.textmanager.text.smil.SmilHelper;
import com.xlythe.textmanager.text.smil.SmilXmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages sms and mms messages
 */
public class TextManager implements MessageManager<Text, Thread, Contact> {
    @SuppressLint("NewApi")
    public static final String[] PROJECTION = new String[] {
            // Determine if message is SMS or MMS
            Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN,
            // Base item ID
            BaseColumns._ID,
            // Conversation (thread) ID
            Telephony.Sms.Conversations.THREAD_ID,
            // Date values
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            // For SMS only
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE,
            // For MMS only
            Telephony.Mms.SUBJECT,
            Telephony.Mms.MESSAGE_BOX
    };

    public static final String[] PROJECTION_PRE_LOLLIPOP = new String[] {
            // Determine if message is SMS or MMS
            "type_discriminator_column",
            // Base item ID
            "_id",
            // Conversation (thread) ID
            "thread_id",
            // Date values
            "date",
            "date_sent",
            // For SMS only
            "address",
            "body",
            "type",
            // For MMS only
            "subject",
            "message_box"
    };

    private String mDeviceNumber;
    private static TextManager sTextManager;
    private Context mContext;
    private ConnectivityManager mConnMgr;
    private boolean mAlreadySending;
    private final Set<MessageObserver> mObservers = new HashSet<>();

    //private final Map<Long, List<Text>> mTexts = new HashMap<>();

    public static TextManager getInstance(Context context) {
        if (sTextManager == null) {
            sTextManager = new TextManager(context);
        }
        return sTextManager;
    }

    private TextManager(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mDeviceNumber = manager.getLine1Number();
        mContext = context;
        context.getContentResolver().registerContentObserver(Uri.parse("content://mms-sms/conversations/"), true, new TextObserver(new Handler()));
    }

    public Cursor getCursor() {
        ContentResolver contentResolver = getContext().getContentResolver();

        final Uri uri;
        final String order;

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
            order = Telephony.Sms.DEFAULT_SORT_ORDER;
        }
        else {
            uri = Uri.parse("content://mms-sms/conversations/");
            order = "date DESC";
        }
        return contentResolver.query(uri, null, null, null, order);
    }

    public Cursor getCursor(long threadId) {
        ContentResolver contentResolver = getContext().getContentResolver();
        final String[] projection;
        final Uri uri;
        final String order;

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            projection = PROJECTION;
            uri = ContentUris.withAppendedId(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, threadId);
            //order = "date ASC";
            order = "normalized_date ASC";
        }
        else {
            projection = PROJECTION_PRE_LOLLIPOP;
            uri = Uri.parse("content://mms-sms/conversations/" + threadId);
            //order = "date ASC";
            order = "normalized_date ASC";
        }
        return contentResolver.query(uri, projection, null, null, order);
    }

    @Override
    public List<Thread> getThreads() {
        List<Thread> threads = new ArrayList<>();
        Cursor c = getCursor();
        if (c.moveToFirst()) {
            do {
                threads.add(new Thread(getContext(), c, mDeviceNumber));
            } while (c.moveToNext());
        }
        c.close();
        Collections.sort(threads);
        return threads;
    }

    @Override
    public void getThreads(final MessageCallback<List<Thread>> callback) {
        // Create a handler so we call back on the same thread we were called on
        final Handler handler = new Handler();

        // Then start a background thread
        new java.lang.Thread() {
            @Override
            public void run() {
                // getThreads is a long running operation
                final List<Thread> threads = getThreads();

                // Return the list in the callback
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(threads);
                    }
                });
            }
        }.start();
    }

    @Override
    public List<Text> getMessages(long threadId) {
        List<Text> messages = new ArrayList<>();
        Cursor c = getCursor(threadId);
        if (c.moveToFirst()) {
            do {
                messages.add(new Text(getContext(), c, mDeviceNumber));
            } while (c.moveToNext());
        }
        c.close();
        //Collections.sort(messages);
        return messages;
    }

    @Override
    public void getMessages(final long threadId, final MessageCallback<List<Text>> callback) {
        // Create a handler so we call back on the same thread we were called on
        final Handler handler = new Handler();

        // Then start a background thread
        new java.lang.Thread() {
            @Override
            public void run() {
                // getMessages is a long running operation
                final List<Text> threads = getMessages(threadId);

                // Return the list in the callback
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(threads);
                    }
                });
            }
        }.start();
    }

    /**
     *  A version of getMessages that reuses the Texts from the expired list. Saves memory for large lists.
     * */
    public List<Text> getMessages(long threadId, List<Text> expiredList) {
        Cursor c = getCursor(threadId);

        // Throw away anything extra
        while (c.getCount() < expiredList.size()) {
            expiredList.remove(expiredList.size() - 1);
        }

        if (c.moveToFirst()) {
            do {
                if (c.getPosition() < expiredList.size()) {
                    // If we can, reuse the Text from the expired list
                    Text text = expiredList.get(c.getPosition());
                    text.invalidate(getContext(), c, mDeviceNumber);
                } else {
                    // Otherwise, just create a new one
                    expiredList.add(new Text(getContext(), c, mDeviceNumber));
                }
            } while (c.moveToNext());
        }
        c.close();

        Collections.sort(expiredList);
        return expiredList;
    }

    public void delete(Text text) {
        String clausole = "_ID = ";
        clausole = clausole + text.getId();
        Uri uri = Uri.parse("content://mms-sms/conversations/" + text.getThreadId());
        mContext.getContentResolver().delete(uri, clausole, null);
    }

    public void deleteTexts(List<Text> texts) {
        String clausole = "";
        for (int i=0; i<texts.size(); i++) {
            if(texts.size()==i+1){
                clausole = clausole + "_ID = " + texts.get(i).getId();
            }
            else {
                clausole = clausole + "_ID = " + texts.get(i).getId() + " OR ";
            }
        }
        Uri uri = Uri.parse("content://mms-sms/conversations/" + texts.get(0).getThreadId());
        mContext.getContentResolver().delete(uri, clausole, null);
    }

    public void delete(Thread thread) {
        String clausole = "_ID = ";
        clausole = clausole + thread.getId();
        Uri uri = Uri.parse("content://mms-sms/conversations/");
        mContext.getContentResolver().delete(uri, clausole, null);
    }

    public void deleteThreads(List<Thread> threads) {
        String clausole = "";
        for (int i=0; i<threads.size(); i++) {
            if(threads.size()==i+1){
                clausole = clausole + "_ID = " + threads.get(i).getId();
            }
            else {
                clausole = clausole + "_ID = " + threads.get(i).getId() + " OR ";
            }
        }
        Uri uri = Uri.parse("content://mms-sms/conversations/");
        mContext.getContentResolver().delete(uri, clausole, null);
    }

    public void markRead(Thread thread) {
        ContentValues values = new ContentValues();
        values.put("read", true);
        Uri uri =Uri.parse("content://mms-sms/conversations/");
        String clausole = "thread_id=" + thread.getThreadId() + " AND read=0";
        mContext.getContentResolver().update(uri, values, clausole, null);
    }

    @Override
    public void registerObserver(MessageObserver observer) {
        mObservers.add(observer);
    }

    @Override
    public void unregisterObserver(MessageObserver observer) {
        mObservers.remove(observer);
    }

    @Override
    public List<Text> search(String text) {
        return new LinkedList<>();
    }

    @Override
    public void search(final String text, final MessageCallback<List<Text>> callback) {
        // Create a handler so we call back on the same thread we were called on
        final Handler handler = new Handler();

        // Then start a background thread
        new java.lang.Thread() {
            @Override
            public void run() {
                // search is a long running operation
                final List<Text> threads = search(text);

                // Return the list in the callback
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(threads);
                    }
                });
            }
        }.start();
    }

    protected Context getContext() {
        return mContext;
    }

    public static byte[] bitmapToByteArray(Bitmap image) {
        if (image == null) {
            Log.v("Message", "image is null, returning byte array of size 0");
            return new byte[0];
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }

    /**
     * Send message over data, even when connected to WiFi
     * @param address
     * @param subject
     * @param body
     * @param attachments
     * @param sentPendingIntent
     * @param deliveredPendingIntent
     */
    public void sendMediaMessage(final String address,
                                 final String subject,
                                 final String body,
                                 final ArrayList<Bitmap> attachments,
                                 PendingIntent sentPendingIntent,
                                 PendingIntent deliveredPendingIntent){
        final ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest.Builder builder = new NetworkRequest.Builder();

        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

        final NetworkRequest networkRequest = builder.build();
        new java.lang.Thread(new Runnable() {
            public void run() {
                connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        ConnectivityManager.setProcessDefaultNetwork(network);
                        ArrayList<MMSPart> data = new ArrayList<>();

                        for (int i = 0; i < attachments.size(); i++) {
                            // turn bitmap into byte array to be stored
                            byte[] imageBytes = bitmapToByteArray(attachments.get(i));

                            MMSPart part = new MMSPart();
                            part.MimeType = "image/jpeg";
                            part.Name = "image" + i;
                            part.Data = imageBytes;
                            data.add(part);
                        }

                        if (!body.isEmpty()) {
                            // add text to the end of the part and send
                            MMSPart part = new MMSPart();
                            part.Name = "text";
                            part.MimeType = "text/plain";
                            part.Data = body.getBytes();
                            data.add(part);
                        }

                        byte[] pdu = getBytes(getContext(), address.split(" "), data.toArray(new MMSPart[data.size()]), subject);

                        try {
                            ApnDefaults.ApnParameters apnParameters = ApnDefaults.getApnParameters(getContext());
                            HttpUtils.httpConnection(
                                    getContext(), 4444L,
                                    apnParameters.getMmscUrl(), pdu, HttpUtils.HTTP_POST_METHOD,
                                    apnParameters.isProxySet(),
                                    apnParameters.getProxyAddress(),
                                    apnParameters.getProxyPort());
                        } catch (IOException ioe) {
                            Log.d("in", "failed");
                        }
                    connectivityManager.unregisterNetworkCallback(this);
                    }
                });
            }
        }).start();
    }

    public static byte[] getBytes(Context context, String[] recipients, MMSPart[] parts, String subject) {
        final SendReq sendRequest = new SendReq();
        // create send request addresses
        for (int i = 0; i < recipients.length; i++) {
            final EncodedStringValue[] phoneNumbers = EncodedStringValue.extract(recipients[i]);
            Log.d("send", recipients[i] + "");
            if (phoneNumbers != null && phoneNumbers.length > 0) {
                sendRequest.addTo(phoneNumbers[0]);
            }
        }
        if (subject != null) {
            sendRequest.setSubject(new EncodedStringValue(subject));
        }
        sendRequest.setDate(Calendar.getInstance().getTimeInMillis() / 1000L);
        try {
            //TODO: add number
            sendRequest.setFrom(new EncodedStringValue("2163138473"));
        } catch (RuntimeException re) {
            Log.d("bad number","bad number");
        }
        final PduBody pduBody = new PduBody();
        // assign parts to the pdu body which contains sending data
        long size = 0;
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                MMSPart part = parts[i];
                if (part != null) {
                    try {
                        PduPart partPdu = new PduPart();
                        partPdu.setName(part.Name.getBytes());
                        partPdu.setContentType(part.MimeType.getBytes());
                        if (part.MimeType.startsWith("text")) {
                            partPdu.setCharset(CharacterSets.UTF_8);
                        }
                        partPdu.setData(part.Data);
                        pduBody.addPart(partPdu);
                        size += (part.Name.getBytes().length + part.MimeType.getBytes().length + part.Data.length);
                    } catch (Exception e) {
                        Log.d("bad part","failed");
                    }
                }
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(SmilHelper.createSmilDocument(pduBody), out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        pduBody.addPart(0, smilPart);
        sendRequest.setBody(pduBody);
        Log.d("send", "setting message size to " + size + " bytes");
        sendRequest.setMessageSize(size);
        // create byte array which will actually be sent
        final PduComposer composer = new PduComposer(context, sendRequest);
        final byte[] bytesToSend;
        bytesToSend = composer.make();
        return bytesToSend;
    }

    public class MMSPart {
        public String Name = "";
        public String MimeType = "";
        public byte[] Data;
        public Uri Path;
    }

    @Override
    public void send(final Text text) {
        String SMS_SENT = "SMS_SENT";
        String SMS_DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(SMS_SENT), 0);
        PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(SMS_DELIVERED), 0);

        // For when the SMS has been sent
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ContentValues values = new ContentValues();
                Uri uri;
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        if (android.os.Build.VERSION.SDK_INT >= 19) {
                            values.put(Telephony.Sms.Sent.STATUS, Telephony.Sms.Sent.STATUS_COMPLETE);
                            uri = Telephony.Sms.Sent.CONTENT_URI;
                        }
                        else {
                            values.put("status", "0");
                            uri = Uri.parse("content://sms/sent");
                        }
                        Uri.withAppendedPath(uri, Uri.encode(Long.toString(text.getId())));
                        getContext().getContentResolver().insert(uri, values);
                        Toast.makeText(getContext(), "SMS sent successfully", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        if (android.os.Build.VERSION.SDK_INT >= 19) {
                            values.put(Telephony.Sms.Sent.STATUS, Telephony.Sms.Sent.STATUS_FAILED);
                            uri = Telephony.Sms.Sent.CONTENT_URI;
                        }
                        else {
                            values.put("status", "64");
                            uri = Uri.parse("content://sms/sent");
                        }
                        Uri.withAppendedPath(uri, Uri.encode(Long.toString(text.getId())));
                        getContext().getContentResolver().insert(uri, values);
                        Toast.makeText(getContext(), "Generic failure cause", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        if (android.os.Build.VERSION.SDK_INT >= 19) {
                            values.put(Telephony.Sms.Sent.STATUS, Telephony.Sms.Sent.STATUS_FAILED);
                            uri = Telephony.Sms.Sent.CONTENT_URI;
                        }
                        else {
                            values.put("status", "64");
                            uri = Uri.parse("content://sms/sent");
                        }
                        Uri.withAppendedPath(uri, Uri.encode(Long.toString(text.getId())));
                        getContext().getContentResolver().insert(uri, values);
                        Toast.makeText(getContext(), "Service is currently unavailable", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        if (android.os.Build.VERSION.SDK_INT >= 19) {
                            values.put(Telephony.Sms.Sent.STATUS, Telephony.Sms.Sent.STATUS_FAILED);
                            uri = Telephony.Sms.Sent.CONTENT_URI;
                        }
                        else {
                            values.put("status", "64");
                            uri = Uri.parse("content://sms/sent");
                        }
                        Uri.withAppendedPath(uri, Uri.encode(Long.toString(text.getId())));
                        getContext().getContentResolver().insert(uri, values);
                        Toast.makeText(getContext(), "No pdu provided", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        if (android.os.Build.VERSION.SDK_INT >= 19) {
                            values.put(Telephony.Sms.Sent.STATUS, Telephony.Sms.Sent.STATUS_FAILED);
                            uri = Telephony.Sms.Sent.CONTENT_URI;
                        }
                        else {
                            values.put("status", "64");
                            uri = Uri.parse("content://sms/sent");
                        }
                        Uri.withAppendedPath(uri, Uri.encode(Long.toString(text.getId())));
                        getContext().getContentResolver().insert(uri, values);
                        Toast.makeText(getContext(), "Radio was explicitly turned off", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SMS_SENT));

        // For when the SMS has been delivered
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SMS_DELIVERED));

        if (!text.isMms()) {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(text.getAddress(), null, text.getBody(), sentPendingIntent, deliveredPendingIntent);
        }
        else {
            Log.e("HI", "should log something!!!!!!!!");
            sendMediaMessage(text.getAddress(), "no subject", text.getBody(), text.getAttachments(), sentPendingIntent, deliveredPendingIntent);
        }

        ContentValues values = new ContentValues();
        Uri uri;

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            values.put(Telephony.Sms.Sent.ADDRESS, text.getAddress());
            values.put(Telephony.Sms.Sent.BODY, text.getBody());
            values.put(Telephony.Sms.Sent.STATUS, Telephony.Sms.Sent.STATUS_PENDING);
            uri = Telephony.Sms.Sent.CONTENT_URI;
        }
        else {
            values.put("address", text.getAddress());
            values.put("body", text.getBody());
            values.put("status", "32");
            uri = Uri.parse("content://sms/sent");
        }
        getContext().getContentResolver().insert(uri, values);
    }

    public Cursor getContactCursor(Text text) {
        ContentResolver contentResolver = getContext().getContentResolver();
        final String[] projection;
        Uri uri;

        if (android.os.Build.VERSION.SDK_INT >= 5) {
            uri = Uri.parse("content://com.android.contacts/phone_lookup");
            projection = new String[] { "display_name" };
        }
        else {
            uri = Uri.parse("content://contacts/phones/filter");
            projection = new String[] { "name" };
        }

        String s = text.getAddress();
        uri = Uri.withAppendedPath(uri, Uri.encode(text.getAddress()));
        return contentResolver.query(uri, null, null, null, null);
    }

    public Cursor getContactCursor(Thread textThread) {
        ContentResolver contentResolver = getContext().getContentResolver();
        Uri uri = Uri.parse("content://com.android.contacts/phone_lookup");
        uri = Uri.withAppendedPath(uri, Uri.encode(textThread.getAddress()));
        return contentResolver.query(uri, null, null, null, null);
    }

    public Contact getSender(Text text) {
        return new Contact(getContactCursor(text), text.getAddress());
    }

    public Contact getSender(Thread textThread) {
        String address = textThread.getAddress();
        return new Contact(getContactCursor(textThread), address);
    }

    private class TextObserver extends ContentObserver {
        TextObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            for (MessageObserver observer : mObservers) {
                observer.notifyDataChanged();
            }
        }
    }
}