package com.xlythe.textmanager.text;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.User;

import java.util.ArrayList;
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
    private final Set<MessageObserver> mObservers = new HashSet<>();

    private final Map<Long, List<Text>> mTexts = new HashMap<>();

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

    private Cursor getCursor() {
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

    private Cursor getCursor(long threadId) {
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

    public void markRead(MessageCallback<Void> callback){

    }

    @Override
    public List<Text> getMessages(int limit) {
        return null;
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
    public void getThreads(MessageCallback<List<Thread>> callback) {
        callback.onSuccess(getThreads());
    }

    @Override
    public List<Text> getMessages(User user) {
        return null;
    }


    @Override
    public void getMessages(User user, MessageCallback<List<Text>> callback) {
        callback.onSuccess(getMessages(user));
    }

    @Override
    public List<Text> search(String text) {
        return new LinkedList<>();
    }

    @Override
    public void search(String text, MessageCallback<List<Text>> callback) {
        callback.onSuccess(search(text));
    }

    protected Context getContext() {
        return mContext;
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

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(text.getAddress(), null, text.getBody(), sentPendingIntent, deliveredPendingIntent);
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