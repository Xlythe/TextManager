package com.xlythe.textmanager;

import android.content.Context;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a series of messages.
 *
 * This is a conversation (Like a chain of emails).
 */
public interface MessageThread {
    public String getId();
    public String getAddress();
    public String getBody();
    public String getDate();
    public String getDateSent();
    public String getErrorCode();
    public String getLocked();
    public String getPerson();
    public String getRead();
    public String getReplyPathPresent();
    public String getServiceCenter();
    public String getStatus();
    public String getSubject();
    public String getThreadId();
    public String getType();

    /**
     * Get the messages sorted by date
     * */
    public List<Message> getMessages(Context context);

    /**
     * Get the {limit} most recent messages.
     * */
    public List<Message> getMessages(int limit);

    /**
     * Return the number of unread messages in this thread.
     * */
    public int getUnreadCount();

    /**
     * Mark all messages in this thread as read.
     * */
    public void markRead();

    /**
     * Mark all messages in this thread as read.
     * */
    public void markRead(MessageCallback<Void> callback);

    /**
     * Deletes this thread.
     * */
    public void delete();

    /**
     * Deletes this thread.
     * */
    public void delete(MessageCallback<Void> callback);
}
