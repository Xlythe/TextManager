package com.xlythe.textmanager;

/**
 * Used for delayed retrieval of messages
 */
public interface MessageCallback<T> {
    /**
     * The operation was successful
     * */
    public void onSuccess(T t);

    /**
     * An error occurred.
     * */
    public void onFailure(Exception e);
}
