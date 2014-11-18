package com.xlythe.textmanager.text;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageThread;

import java.util.ArrayList;
import java.util.List;

/**
 * An SMS conversation
 */
public class TextThread implements MessageThread {
    private Context mContext;
    private long mThreadId;

    public TextThread(Context context) {
        mContext = context;
    }

    /**
     * A unique id that differentiates this thread from all others.
     * */
    public String getId() {
        return Long.toString(mThreadId);
    }

    /**
     * Get the messages sorted by date
     * */
    public List<Message> getMessages() {
        Text text = new Text(); //Text message has been created
        List<Message> messages = new ArrayList<Message>();
        messages.add(text);
        return messages; //returning that text message to the list/thread
        // ADDED FOR NULL ERROR FIX: TextListFragment.java:34
    }

    /**
     * Get the {limit} most recent messages.
     * */
    public List<Message> getMessages(int limit) {
        Text text = new Text(); //Text message has been created
        List<Message> messages = new ArrayList<Message>();
        messages.add(text);
        return messages; //returning that text message to the list/thread
    }

    /**
     * Return the number of messages in this thread.
     * */
    public int getCount() {
        return 0;
    }

    /**
     * Return the number of unread messages in this thread.
     * */
    public int getUnreadCount() {
        return 0;
    }

    /**
     * Mark all messages in this thread as read.
     * */
    public void markRead() {
        ContentValues values = new ContentValues();
        values.put("read", true);
        mContext.getContentResolver().update(Uri.parse("content://sms/inbox"), values,
                "thread_id=" + mThreadId + " AND read=0", null);
    }

    /**
     * Mark all messages in this thread as read.
     * */
    public void markRead(MessageCallback<Void> callback) {

    }

    /**
     * Deletes this thread.
     * */
    public void delete() {

    }

    /**
     * Deletes this thread.
     * */
    public void delete(MessageCallback<Void> callback) {

    }

    @Override
    public String toString() {
        Message mostRecent = getMessages(1).get(0);
        return mostRecent.getSender() + ": " + mostRecent.getText();
    }
}
