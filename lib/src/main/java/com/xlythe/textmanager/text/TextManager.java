package com.xlythe.textmanager.text;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.User;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages sms and mms messages
 */
public class TextManager implements MessageManager<Text, TextThread, TextUser> {
    private static TextManager sTextManager;
    private Context mContext;

    public static TextManager getInstance(Context context) {
        if (sTextManager == null) {
            sTextManager = new TextManager(context);
        }
        return sTextManager;
    }

    private TextManager(Context context) {
        mContext = context;
    }

    public CustomThreadCursor getThreadCursor() {
        ContentResolver contentResolver = getContext().getContentResolver();
        final String[] projection;
        final Uri uri;
        final String order;

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            projection = new String[]{
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.DATE_SENT,
                    Telephony.Sms.ERROR_CODE,
                    Telephony.Sms.LOCKED,
                    Telephony.Sms.PERSON,
                    Telephony.Sms.READ,
                    Telephony.Sms.REPLY_PATH_PRESENT,
                    Telephony.Sms.SERVICE_CENTER,
                    Telephony.Sms.STATUS,
                    Telephony.Sms.SUBJECT,
                    Telephony.Sms.THREAD_ID,
                    Telephony.Sms.TYPE,
            };
            uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
            order = Telephony.Sms.DEFAULT_SORT_ORDER;
        }
        else {
            projection = new String[]{};
            uri = Uri.parse("content://mms-sms/conversations/");
            order = "date DESC";
        }

        return new CustomThreadCursor(contentResolver.query(uri, null, null, null, order));
    }

    @Override
    public List<TextThread> getThreads() {
        List<TextThread> mt = new ArrayList<>();
        Cursor c = getThreadCursor();
        if (c.moveToFirst()) {
            do {
                mt.add(new TextThread(c));
            } while (c.moveToNext());
        }
        c.close();
        return mt;
    }

    @Override
    public void getThreads(MessageCallback<List<TextThread>> callback) {
        callback.onSuccess(getThreads());
    }

    public CustomTextCursor getTextCursor(long threadId) {
        ContentResolver contentResolver = getContext().getContentResolver();
        final String[] projection;
        final Uri uri;
        final String order = "date ASC";

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            projection = new String[]{
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.CREATOR,
                    Telephony.Sms.DATE,
                    Telephony.Sms.DATE_SENT,
                    Telephony.Sms.ERROR_CODE,
                    Telephony.Sms.LOCKED,
                    Telephony.Sms.PERSON,
                    Telephony.Sms.READ,
                    Telephony.Sms.REPLY_PATH_PRESENT,
                    Telephony.Sms.SERVICE_CENTER,
                    Telephony.Sms.SEEN,
                    Telephony.Sms.STATUS,
                    Telephony.Sms.SUBJECT,
                    Telephony.Sms.THREAD_ID,
                    Telephony.Sms.TYPE,
                    Telephony.Mms._ID
            };
            uri = Uri.parse("content://mms-sms/conversations/" + threadId);
        }
        else {
            projection = new String[]{};
            uri = Uri.parse("content://mms-sms/conversations/" + threadId);
        }
        return new CustomTextCursor(contentResolver.query(uri, projection, null, null, order));
    }

    public List<Text> getMessages(long threadId) {
        List<Text> list = new ArrayList<>();
        Cursor c = getTextCursor(threadId);
        if (c.moveToFirst()) {
            do {
                list.add(new Text(c));
            } while (c.moveToNext());
        }
        c.close();
        return list;
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

    public void markRead(TextThread thread) {
        ContentValues values = new ContentValues();
        values.put("read", true);
        Uri uri =Uri.parse("content://mms-sms/conversations/");
        String clausole = "thread_id=" + thread.getThreadId() + " AND read=0";
        mContext.getContentResolver().update(uri, values, clausole, null);
    }

    public void markRead(MessageCallback<Void> callback){

    }

    public List<Text> getMessages(int limit) {
        return null;
    }

    @Override
    public void registerObserver() {

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
                        Uri.withAppendedPath(uri, Uri.encode(text.getId()));
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
                        Uri.withAppendedPath(uri, Uri.encode(text.getId()));
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
                        Uri.withAppendedPath(uri, Uri.encode(text.getId()));
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
                        Uri.withAppendedPath(uri, Uri.encode(text.getId()));
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
                        Uri.withAppendedPath(uri, Uri.encode(text.getId()));
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

    public Cursor getContactCursor(TextThread textThread) {
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

        uri = Uri.withAppendedPath(uri, Uri.encode(textThread.getAddress()));
        return contentResolver.query(uri, null, null, null, null);
    }

    public TextUser getSender(Text text) {
        return new TextUser(getContactCursor(text), text.getAddress());
    }
    public TextUser getSender(TextThread textThread) {
        String address = textThread.getAddress();
        return new TextUser(getContactCursor(textThread), address);
    }
}