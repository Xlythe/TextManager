package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;

import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.MessageObserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Manages sms and mms messages
 */
public class TextManager implements MessageManager<Text, Thread, Contact> {
    public static final String[] PROJECTION = new String[] {
            // Determine if message is SMS or MMS
            Mock.Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN,
            // Base item ID
            BaseColumns._ID,
            // Conversation (thread) ID
            Mock.Telephony.Sms.Conversations.THREAD_ID,
            // Date values
            Mock.Telephony.Sms.DATE,
            Mock.Telephony.Sms.DATE_SENT,
            // For SMS only
            Mock.Telephony.Sms.ADDRESS,
            Mock.Telephony.Sms.BODY,
            Mock.Telephony.Sms.TYPE,
            // For MMS only
            Mock.Telephony.Mms.SUBJECT,
            Mock.Telephony.Mms.MESSAGE_BOX
    };

    private static TextManager sTextManager;
    private Context mContext;
    private final Set<MessageObserver> mObservers = new HashSet<>();

    public static TextManager getInstance(Context context) {
        if (sTextManager == null) {
            sTextManager = new TextManager(context);
        }
        return sTextManager;
    }

    private TextManager(Context context) {
        mContext = context;
        context.getContentResolver().registerContentObserver(Uri.parse("content://mms-sms/conversations/"), true, new TextObserver(new Handler()));
    }



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

    public void getMessages(final long threadId, final MessageCallback<List<Text>> callback) {
        // Create a handler so we call back on the same thread we were called on
        final Handler handler = new Handler();

        // Then start a background thread
        new java.lang.Thread() {
            @Override
            public void run() {
                // getMessages is a long running operation
                final List<Text> threads = getMessages(Long.toString(threadId));

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

    public void registerObserver(MessageObserver observer) {
        mObservers.add(observer);
    }

    public void unregisterObserver(MessageObserver observer) {
        mObservers.remove(observer);
    }

    public List<Text> search(String text) {
        return new LinkedList<>();
    }

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



    @Override
    public void send(final Text text) {
        ManagerUtils.send(mContext, text);
    }



    @Override
    public List<Text> getMessages(String threadId) {
        List<Text> messages = new ArrayList<>();
        Cursor c = getMessagesCursor(threadId);
        if (c.moveToFirst()) {
            do {
                messages.add(new Text(mContext, c));
            } while (c.moveToNext());
        }
        c.close();
        return messages;
    }

    @Override
    public Cursor getMessagesCursor(String threadId) {
        ContentResolver contentResolver = mContext.getContentResolver();
        final String[] projection = PROJECTION;
        final Uri uri = Uri.parse(Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI + threadId);
        final String order = "normalized_date ASC";
        return contentResolver.query(uri, projection, null, null, order);
    }

    @Override
    public List<Text> getMessages(Thread thread) {
        return null;
    }

    @Override
    public Cursor getMessagesCursor(Thread thread) {
        return null;
    }

    @Override
    public Text getMessage(String messageId) {
        return null;
    }



    @Override
    public List<Thread> getThreads() {
        List<Thread> threads = new ArrayList<>();
        Cursor c = getThreadsCursor();
        if (c.moveToFirst()) {
            do {
                threads.add(new Thread(mContext, c));
            } while (c.moveToNext());
        }
        c.close();
        return threads;
    }

    @Override
    public Cursor getThreadsCursor() {
        ContentResolver contentResolver = mContext.getContentResolver();
        final Uri uri = Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
        final String order = Mock.Telephony.Sms.DEFAULT_SORT_ORDER;
        return contentResolver.query(uri, null, null, null, order);
    }

    @Override
    public Thread getThread(String threadId) {
        return null;
    }



    @Override
    public void deleteMessage(String messageId) {

    }

    @Override
    public void deleteMessages(String... messageIds) {

    }

    @Override
    public void deleteMessage(Text message) {
        String clausole = "_ID = ";
        clausole = clausole + message.getId();
        Uri uri = Uri.parse("content://mms-sms/conversations/" + message.getThreadId());
        mContext.getContentResolver().delete(uri, clausole, null);
    }

    @Override
    public void deleteMessages(Text... messages) {
        String clausole = "";
        for (int i=0; i<messages.length; i++) {
            if(messages.length==i+1){
                clausole = clausole + "_ID = " + messages[i].getId();
            }
            else {
                clausole = clausole + "_ID = " + messages[i].getId() + " OR ";
            }
        }
        Uri uri = Uri.parse("content://mms-sms/conversations/" + messages[0].getThreadId());
        mContext.getContentResolver().delete(uri, clausole, null);
    }



    @Override
    public void deleteThread(String threadId) {

    }

    @Override
    public void deleteThreads(String... threadIds) {

    }

    @Override
    public void deleteThread(Thread thread) {
        String clausole = "_ID = ";
        clausole = clausole + thread.getId();
        Uri uri = Uri.parse("content://mms-sms/conversations/");
        mContext.getContentResolver().delete(uri, clausole, null);
    }

    @Override
    public void deleteThreads(Thread... threads) {
        String clausole = "";
        for (int i=0; i<threads.length; i++) {
            if(threads.length==i+1){
                clausole = clausole + "_ID = " + threads[i].getId();
            }
            else {
                clausole = clausole + "_ID = " + threads[i].getId() + " OR ";
            }
        }
        Uri uri = Uri.parse("content://mms-sms/conversations/");
        mContext.getContentResolver().delete(uri, clausole, null);
    }



    @Override
    public void MarkMessageAsRead(String messageId) {

    }

    @Override
    public void MarkMessagesAsRead(String... messageId) {

    }

    @Override
    public void MarkMessageAsRead(Text message) {

    }

    @Override
    public void MarkMessagesAsRead(Text... message) {

    }



    @Override
    public void MarkThreadAsRead(String threadId) {

    }

    @Override
    public void MarkThreadsAsRead(String... threadId) {

    }

    @Override
    public void MarkThreadAsRead(Thread thread) {
        ContentValues values = new ContentValues();
        values.put("read", true);
        Uri uri =Uri.parse("content://mms-sms/conversations/");
        String clausole = "thread_id=" + thread.getId() + " AND read=0";
        mContext.getContentResolver().update(uri, values, clausole, null);
    }

    @Override
    public void MarkThreadsAsRead(Thread... thread) {

    }
}