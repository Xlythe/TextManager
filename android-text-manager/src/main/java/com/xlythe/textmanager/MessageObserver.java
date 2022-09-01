package com.xlythe.textmanager;

/**
 * Will notify you whenever messages are changed.
 */
public interface MessageObserver {
    /**
     * There has been a change, and you need to refresh the ui.
     * */
    void notifyDataChanged();
}
