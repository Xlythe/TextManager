package com.xlythe.textmanager;

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

    void send(M message);
}
