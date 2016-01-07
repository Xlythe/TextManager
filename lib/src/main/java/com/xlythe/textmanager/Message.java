package com.xlythe.textmanager;

import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Status;

import java.util.List;

/**
 * Represents a message. May contain attachments like images, voice.
 */
public interface Message {
    String getId();
    String getThreadId();
    String getBody();
    long getTimestamp();
    User getSender();
    User getRecipient();
    List<Attachment> getAttachments();
    Status getStatus(); // Status.Sending, Status.Sent, Status.Failed, Status.Read, Status.Unread
}
