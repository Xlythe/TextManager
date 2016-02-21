package com.xlythe.textmanager;

import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a message. May contain attachments like images, voice.
 */
public interface Message<U extends User> {
    String getId();
    String getThreadId();
    String getBody();
    long getTimestamp();
    User getSender();
    Set<U> getMembers();
    Attachment getAttachment();
    Status getStatus(); // Status.Sending, Status.Sent, Status.Failed, Status.Read, Status.Unread
}
