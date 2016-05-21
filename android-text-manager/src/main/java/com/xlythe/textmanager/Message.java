package com.xlythe.textmanager;

import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Status;

/**
 * Represents a message. May contain attachments like images, voice.
 */
public interface Message<U extends User> {
    String getId();
    String getThreadId();
    String getBody();
    long getTimestamp();
    Attachment getAttachment();
    Status getStatus(); // Status.Sending, Status.Sent, Status.Failed, Status.Read, Status.Unread
}
