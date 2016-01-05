package com.xlythe.textmanager;

import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Status;

import java.util.List;

/**
 * Represents a message. May contain data like images, voice, or just raw bytes.
 */
public interface Message {
    String getId();
    String getThreadId();
    String getBody();
    long getTimestamp();
    User getSender();
    User getRecipient();
    Attachment getAttachment();
    Status getStatus(); // Status.Sending, Status.Sent, Status.Failed, Status.Read, Status.Unread
}
