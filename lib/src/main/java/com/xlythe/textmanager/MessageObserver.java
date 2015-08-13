package com.xlythe.textmanager;

import com.xlythe.textmanager.text.Text;

/**
 * Will notify you whenever messages are changed.
 */
public interface MessageObserver {
    /**
     * There has been a change, and you need to refresh the ui.
     * */
    public void dataAdded(Text text);
    public void dataUpdated(int position, Text text);
    public void dataRemoved(int position);
}
