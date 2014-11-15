package com.xlythe.textmanager;

import java.util.List;

/**
 * A generic interface for managing messages
 */
public interface MessageManager {
    /**
     * Return all message threads
     * */
    public List<MessageThread> getThreads();

    /**
     * Return all message threads
     * */
    public void getThreads(MessageCallback<List<MessageThread>> callback);

    /**
     * Register an observer to get callbacks every time messages are added, deleted, or changed.
     * */
    public void registerObserver();

    /**
     * Get all messages involving that user.
     * */
    public List<Message> getMessages(User user);

    /**
     * Get all messages involving that user.
     * */
    public void getMessages(User user, MessageCallback<List<Message>> callback);

    /**
     * Return all messages containing the text.
     * */
    public List<Message> search(String text);

    /**
     * Return all messages containing the text.
     * */
    public void search(String text, MessageCallback<List<Message>> callback);
}
