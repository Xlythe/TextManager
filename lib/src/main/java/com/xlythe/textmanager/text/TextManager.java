package com.xlythe.textmanager.text;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.MessageThread;
import com.xlythe.textmanager.User;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages sms and mms messages
 */
public class TextManager implements MessageManager {
    /**
     * Return all message threads
     * */
    public List<MessageThread> getThreads() {
        return new ArrayList<MessageThread>();
    }

    /**
     * Return all message threads
     * */
    public void getThreads(MessageCallback<List<MessageThread>> callback) {}

    /**
     * Register an observer to get callbacks every time messages are added, deleted, or changed.
     * */
    public void registerObserver() {

    }

    /**
     * Get all messages involving that user.
     * */
    public List<Message> getMessages(User user) {
        return new ArrayList<Message>();
    }

    /**
     * Get all messages involving that user.
     * */
    public void getMessages(User user, MessageCallback<List<Message>> callback) {

    }

    /**
     * Return all messages containing the text.
     * */
    public List<Message> search(String text) {
        LinkedList<Message> messages = new LinkedList<Message>();
        for(MessageThread t : getThreads()) {
            for(Message m : t.getMessages()) {
                if(m.getText() != null) {
                    if(m.getText().contains(text)) {
                        messages.add(m);
                    }
                }
            }
        }
        return messages;
    }

    /**
     * Return all messages containing the text.
     * */
    public void search(String text, MessageCallback<List<Message>> callback) {

    }
}
