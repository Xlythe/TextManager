package com.xlythe.textmanager;

import java.util.List;

/**
 * Represents a series of messages.
 *
 * This is a conversation (Like a chain of emails).
 */
public interface MessageThread {
    /**
     * A unique id that differentiates this thread from all others.
     * */
    public String getId();

    /**
     * Get the messages sorted by date
     * */
    public List<Message> getMessages();

    /**
     * Get the {limit} most recent messages.
     * */
    public List<Message> getMessages(int limit);

    /**
     * Return the number of messages in this thread.
     * */
    public int getCount();

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
