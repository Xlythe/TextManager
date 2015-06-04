package com.xlythe.textmanager;

import android.content.Context;

import java.util.List;

/**
 * A generic interface for managing messages
 */
public interface MessageManager<M extends Message, T extends MessageThread, U extends User> {
    /**
     * Return all message threads
     * */
    List<T> getThreads();

    /**
     * Return all message threads
     * */
    void getThreads(MessageCallback<List<T>> callback);

    /**
     * Get the messages sorted by date
     * */
    List<M> getMessages(long threadId);

    /**
     * Get the {limit} most recent messages.
     * */
    List<M> getMessages(int limit);

    /**
     * Register an observer to get callbacks every time messages are added, deleted, or changed.
     * */
    void registerObserver();

    /**
     * Get all messages involving that user.
     * */
    List<M> getMessages(User user);

    /**
     * Get all messages involving that user.
     * */
    void getMessages(User user, MessageCallback<List<M>> callback);

    /**
     * Return all messages containing the text.
     * */
    List<M> search(String text);

    /**
     * Return all messages containing the text.
     * */
    void search(String text, MessageCallback<List<M>> callback);

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

    void send(M message);
}
