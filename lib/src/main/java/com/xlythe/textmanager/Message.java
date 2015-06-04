package com.xlythe.textmanager;

import java.util.List;

/**
 * Represents a message. May contain data like images, voice, or just raw bytes.
 */
public interface Message {
    enum Status {
        SENDING, SENT, FAILED, SEEN, READ, UNREAD;
    }

    /**
     * Returns the message status (Sending, Sent, Read)
     * */
    Status getStatus();
}
