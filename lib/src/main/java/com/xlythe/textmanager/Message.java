package com.xlythe.textmanager;

import java.util.List;

/**
 * Represents a message. May contain data like images, voice, or just raw bytes.
 */
public interface Message {
    public String getId();
    public String getAddress();
    public String getBody();
    public String getCreator();
    public String getDate();
    public String getDateSent();
    public String getErrorCode();
    public String getLocked();
    public String getPerson();
    public String getRead();
    public String getReplyPathPresent();
    public String getServiceCenter();
    public String getSeen();
    public String getStatus();
    public String getSubject();
    public String getThreadId();
    public String getType();

    /**
     * Return true if the user has already seen this message.
     * */
    public boolean isRead();

    /**
     * Return true if this message is currently being sent.
     * */
    public boolean isSending();

    /**
     * Return true if this message failed to send.
     * */
    public boolean notSent();

    /**
     * Mark this message as having been read.
     * */
    public void markAsRead();

    /**
     * Mark this message as having been read.
     * */
    public void markAsRead(MessageCallback<Void> callback);

    /**
     * Deletes this message.
     * */
    public void delete();

    /**
     * Deletes this message.
     * */
    public void delete(MessageCallback<Void> callback);

    /**
     * Send this message to the recipient.
     *
     * You may throw an error if the message shouldn't be sent.
     * */
    public void send();

    /**
     * Send this message to the recipient.
     *
     * You may throw an error if the message shouldn't be sent.
     * */
    public void send(MessageCallback<Void> callback);
}
