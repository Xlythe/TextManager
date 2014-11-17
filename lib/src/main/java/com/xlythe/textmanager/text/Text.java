package com.xlythe.textmanager.text;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.User;

import java.util.List;

/**
 * Either a sms or a mms
 */
public class Text implements Message {
    private String mText;
    private long mId;
    private long mThreadId;
    private long mTimestamp;
    private Type mType;

    public enum Type {
        SMS, MMS
    }

    /**
     * We don't want anyone to create a text without using the builder
     * */
    Text() {
        mType = Type.SMS;
    }

    public long getId() {
        return mId;
    }

    protected void setId(long id) {
        mId = id;
    }

    /**
     * Return the thread id that the message belongs to.
     * */
    public String getThreadId() {
        return Long.toString(mThreadId);
    }

    protected void setThreadId(long threadId) {
        mThreadId = threadId;
    }

    /**
     * Return the message text.
     *
     * May be null if the message is nothing but data.
     * */
    public String getText() {
        return mText;
    }

    protected void setText(String text) {
        mText = text;
    }

    /**
     * Any data (voice, images, etc) is returned.
     *
     * This will likely be null in most cases.
     * */
    public byte[] getData() {
        switch(mType) {
            case SMS:
                // SMS is always null
                return null;
            case MMS:
                return null;
        }
        return null;
    }

    /**
     * Return the timestamp of the message in milliseconds
     * */
    public long getTimestamp() {
        return mTimestamp;
    }

    protected void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    /**
     * Return the person who sent the message.
     * */
    public User getSender() {
        return null;
    }

    /**
     * Return the people who received the message.
     * */
    public List<User> getRecipients() {
        return null;
    }

    /**
     * Return true if the user has already seen this message.
     * */
    public boolean isRead() {
        return true;
    }

    /**
     * Return true if this message is currently being sent.
     * */
    public boolean isSending() {
        return false;
    }

    /**
     * Return true if this message failed to send.
     * */
    public boolean notSent() {
        return false;
    }

    /**
     * Mark this message as having been read.
     * */
    public void markAsRead() {

    }

    /**
     * Mark this message as having been read.
     * */
    public void markAsRead(MessageCallback<Void> callback) {

    }

    /**
     * Deletes this message.
     * */
    public void delete() {

    }

    /**
     * Deletes this message.
     * */
    public void delete(MessageCallback<Void> callback) {

    }

    /**
     * Send this message to the recipient.
     *
     * You may throw an error if the message shouldn't be sent.
     * */
    public void send() {

    }

    /**
     * Send this message to the recipient.
     *
     * You may throw an error if the message shouldn't be sent.
     * */
    public void send(MessageCallback<Void> callback) {

    }

    @Override
    public String toString() {
        return getText();
    }
}
