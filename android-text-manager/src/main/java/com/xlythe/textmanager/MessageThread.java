package com.xlythe.textmanager;

/**
 * Represents a series of messages.
 *
 * This is a conversation (Like a chain of emails).
 */
public interface MessageThread<M extends Message> {
    String getId();
    M getLatestMessage();
}
