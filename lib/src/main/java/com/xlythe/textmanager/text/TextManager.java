package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;

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

    public static TextManager getInstance(Context context) {
        if (sTextManager == null) {
            sTextManager = new TextManager(context);
        }
        return sTextManager;
    }

    private Context mContext;

    private TextManager(Context context) {
        mContext = context;
    }

    public Cursor getThreadCursor() {
        ContentResolver contentResolver = getContext().getContentResolver();
        final String[] projection = new String[]{
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
        final String order = Telephony.Sms.DEFAULT_SORT_ORDER;

        Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
        } else {
            uri = Uri.parse("content://mms-sms/conversations/");
        }

        return contentResolver.query(uri, projection, null, null, order);
    }

    /**
     * Return all message threads
     * */
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

    /**
     * Return all message threads
     * */
    @Override
    public void getThreads(MessageCallback<List<TextThread>> callback) {
        callback.onSuccess(getThreads());
    }

    /**
     * Register an observer to get callbacks every time messages are added, deleted, or changed.
     * */
    @Override
    public void registerObserver() {

    }

    /**
     * Get all messages involving that user.
     * */
    @Override
    public List<Text> getMessages(User user) {
        return null;
    }

    /**
     * Get all messages involving that user.
     * */
    @Override
    public void getMessages(User user, MessageCallback<List<Text>> callback) {
        callback.onSuccess(getMessages(user));
    }

    /**
     * Return all messages containing the text.
     * */
    @Override
    public List<Text> search(String text) {
        LinkedList<Text> messages = new LinkedList<>();
        return messages;
    }

    /**
     * Return all messages containing the text.
     * */
    @Override
    public void search(String text, MessageCallback<List<Text>> callback) {
        callback.onSuccess(search(text));
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public void send(Text text) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(text.getAddress(), null, text.getBody(), null, null);
        ContentValues values = new ContentValues();
        values.put("address", text.getAddress());
        values.put("body", text.getBody());
        getContext().getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }
}