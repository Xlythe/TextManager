package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.MessageThread;
import com.xlythe.textmanager.User;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages sms and mms messages
 */
public class TextManager implements MessageManager {
    private List<MessageThread> mt;

    public TextManager(Context context) {
        populateThreads(context);
    }

    /**
     * Return all message threads
     * */
    public List<MessageThread> getThreads() {
        return mt;
    }

    private void populateThreads(Context context){
        mt = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        //final String[] projection = new String[]{"_id", "body", "address","date","person","thread_id" };
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
        //Uri uri = Uri.parse("content://mms-sms/conversations/");
        Uri uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
        Cursor c = contentResolver.query(uri, projection, null, null, order);
        if (c.moveToFirst()) {
            do {
                mt.add(new TextThread(c));
            } while (c.moveToNext());
        }
        c.close();
    }

    /**
     * Return all message threads
     * */
    public void getThreads(MessageCallback<List<MessageThread>> callback) {}

    /**
     * Register an observer to get callbacks every time messages are added, deleted, or changed.
     * */
    public void registerObserver() {

    }

    /**
     * Get all messages involving that user.
     * */
    public List<Message> getMessages(User user) {
        return null;
    }

    /**
     * Get all messages involving that user.
     * */
    public void getMessages(User user, MessageCallback<List<Message>> callback) {

    }

    /**
     * Return all messages containing the text.
     * */
    public List<Message> search(String text) {
        LinkedList<Message> messages = new LinkedList<Message>();
        return messages;
    }

    /**
     * Return all messages containing the text.
     * */
    public void search(String text, MessageCallback<List<Message>> callback) {

    }
}