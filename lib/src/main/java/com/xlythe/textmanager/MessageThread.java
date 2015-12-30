package com.xlythe.textmanager;

import android.content.Context;

import java.util.List;

/**
 * Represents a series of messages.
 *
 * This is a conversation (Like a chain of emails).
 */
public interface MessageThread<M extends Message> {
    String getId();
    M getLatestMessage();
    int getUnreadCount();
    int getCount();
}
