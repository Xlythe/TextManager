package com.xlythe.textmanager;

import android.database.Cursor;

import java.util.List;

/**
 * A generic interface for managing messages
 */
public interface MessageManager<M extends Message, T extends MessageThread, U extends User> {
    List<M> getMessages(String threadId);
    Cursor getMessagesCursor(String threadId);
    List<M> getMessages(T thread);
    Cursor getMessagesCursor(T thread);
    M getMessage(String messageId);

    List<T> getThreads();
    Cursor getThreadsCursor();
    T getThread(String threadId);

    void deleteMessage(String messageId);
    void deleteMessages(String... messageIds);
    void deleteMessage(M message);
    void deleteMessages(M... messages);

    void deleteThread(String threadId);
    void deleteThreads(String... threadIds);
    void deleteThread(T thread);
    void deleteThreads(T... threads);

    void MarkMessageAsRead(String messageId);
    void MarkMessagesAsRead(String... messageId);
    void MarkMessageAsRead(M message);
    void MarkMessagesAsRead(M... message);

    void MarkThreadAsRead(String threadId);
    void MarkThreadsAsRead(String... threadId);
    void MarkThreadAsRead(T thread);
    void MarkThreadsAsRead(T... thread);

    void send(M message);
}
