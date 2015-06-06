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

        return new CustomThreadCursor(contentResolver.query(uri, projection, null, null, order));
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
    public void send(Text text) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(text.getAddress(), null, text.getBody(), null, null);
        ContentValues values = new ContentValues();
        values.put("address", text.getAddress());
        values.put("body", text.getBody());
        getContext().getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }

    public TextUser getSender(Text text) {
        TextUser user = new TextUser(text, getContext());
        return user;
    }
    public TextUser getSender(TextThread textThread) {
        TextUser user = new TextUser(textThread, getContext());
        return user;
    }
}