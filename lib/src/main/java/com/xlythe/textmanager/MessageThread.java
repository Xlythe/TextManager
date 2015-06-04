package com.xlythe.textmanager;

import android.content.Context;

import java.util.List;

/**
 * Represents a series of messages.
 *
 * This is a conversation (Like a chain of emails).
 */
public interface MessageThread<M extends Message> {
    /**
     * Return the number of unread messages in this thread.
     * */
    int getUnreadCount();

    /**
     * Mark all messages in this thread as read.
     * */
    void markRead();

    /**
     * Mark all messages in this thread as read.
     * */
    void markRead(MessageCallback<Void> callback);

    /**
     * Deletes this thread.
     * */
    void delete();

    /**
     * Deletes this thread.
     * */
    void delete(MessageCallback<Void> callback);
}
