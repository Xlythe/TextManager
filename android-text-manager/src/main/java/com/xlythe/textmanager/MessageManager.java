package com.xlythe.textmanager;

import android.database.Cursor;

import java.util.List;

/**
 * A generic interface for managing messages
 */
public interface MessageManager<M extends Message, T extends MessageThread, U extends User> {

    /**
     * Returns all messages for the given thread
     * */
    List<M> getMessages(T thread);

    /**
     * Returns all messages for the given thread
     * */
    void getMessages(T thread, MessageCallback<List<M>> callback);

    /**
     * Returns a message cursor for the given thread
     * */
    Cursor getMessageCursor(T thread);

    /**
     * Returns a message given an id
     * */
    M getMessage(String threadId, String id);

    /**
     * Returns a message given an id
     * */
    void getMessage(String id, MessageCallback<M> callback);

    /**
     * Return all threads
     * */
    List<T> getThreads();

    /**
     * Return all threads
     * */
    void getThreads(MessageCallback<List<T>> callback);

    /**
     * Returns a thread cursor
     * */
    Cursor getThreadCursor();

    /**
     * Returns a thread given an id
     * */
    T getThread(String id);

    /**
     * Returns a thread given an id
     * */
    void getThread(String threadId, MessageCallback<T> callback);

    /**
     * Deletes a message
     * */
    void delete(M message);

    /**
     * Deletes a thread
     * */
    void delete(T thread);

    /**
     * Marks a message as read
     * */
    void markAsRead(M message);

    /**
     * Marks all messages in a thread as read
     * */
    void markAsRead(T thread);

    /**
     * Sends a message
     * */
    void send(M message);

    /**
     * Return all messages containing the text.
     * */
    List<M> search(String text);

    /**
     * Return all messages containing the text.
     * */
    void search(String text, MessageCallback<List<M>> callback);

    /**
     * Register an observer to get callbacks every time messages are added, deleted, or changed.
     * */
    void registerObserver(MessageObserver observer);

    /**
     * Remove a registered observer
     * */
    void unregisterObserver(MessageObserver observer);
}
