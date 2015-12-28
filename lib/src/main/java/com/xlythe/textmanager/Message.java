package com.xlythe.textmanager;

import java.util.List;

/**
 * Represents a message. May contain data like images, voice, or just raw bytes.
 */
public interface Message {
    /**
     * Return the thread id that the message belongs to.
     * */
    String getThreadId();

    /**
     * Return the message text.
     *
     * May be null if the message is nothing but data.
     * */
    String getText();

    /**
     * Any data (voice, images, etc) is returned.
     *
     * This will likely be null in most cases.
     * */
    byte[] getData();

    /**
     * Return the timestamp of the message in milliseconds
     * */
    long getTimestamp();

    /**
     * Return the person who sent the message.
     * */
    User getSender();

    /**
     * Return the people who received the message.
     * */
    List<User> getRecipients();

    /**
     * Return true if the user has already seen this message.
     * */
    boolean isRead();

    /**
     * Return true if this message is currently being sent.
     * */
    boolean isSending();

    /**
     * Return true if this message failed to send.
     * */
    boolean notSent();

    /**
     * Mark this message as having been read.
     * */
    void markAsRead();

    /**
     * Mark this message as having been read.
     * */
    void markAsRead(MessageCallback<Void> callback);

    /**
     * Deletes this message.
     * */
    void delete();

    /**
     * Deletes this message.
     * */
    void delete(MessageCallback<Void> callback);

    /**
     * Send this message to the recipient.
     *
     * You may throw an error if the message shouldn't be sent.
     * */
    void send();

    /**
     * Send this message to the recipient.
     *
     * You may throw an error if the message shouldn't be sent.
     * */
    void send(MessageCallback<Void> callback);

}
