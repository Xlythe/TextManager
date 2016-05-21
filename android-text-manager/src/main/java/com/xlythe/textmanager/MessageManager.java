package com.xlythe.textmanager;

import android.database.Cursor;

import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.concurrency.Future;

import java.util.List;
import java.util.Set;

/**
 * A generic interface for managing messages
 */
public interface MessageManager<M extends Message, T extends MessageThread, U extends User> {

    /**
     * Returns all messages for the given thread
     */
    Future<List<M>> getMessages(T thread);

    /**
     * Returns a message cursor for the given thread
     */
    Cursor getMessageCursor(T thread);

    /**
     * Returns a message given an id
     */
    Future<M> getMessage(String id);

    /**
     * Return all threads
     */
    Future<List<T>> getThreads();

    /**
     * Returns a thread cursor
     */
    Cursor getThreadCursor();

    /**
     * Returns a thread given an id
     */
    Future<T> getThread(String id);

    /**
     * Deletes a message
     */
    void delete(M... message);

    /**
     * Deletes a thread
     */
    void delete(T... thread);

    /**
     * Marks a message as read
     */
    void markAsRead(M message);

    /**
     * Marks all messages in a thread as read
     */
    void markAsRead(T thread);

    /**
     * Return all messages containing the text.
     */
    Future<List<M>> search(String text);

    /**
     * Register an observer to get callbacks every time messages are added, deleted, or changed.
     */
    void registerObserver(MessageObserver observer);

    /**
     * Remove a registered observer
     */
    void unregisterObserver(MessageObserver observer);

    int getUnreadCount(T thread);
    int getCount(T thread);
    Future<U> getSender(M message);
    Future<Set<U>> getMembers(M message);
}
