package com.xlythe.textmanager;

/**
 * Used for delayed retrieval of messages
 */
public interface MessageCallback<T> {
    /**
     * The operation was successful
     * */
    void onSuccess(T t);

    /**
     * An error occurred.
     * */
    void onFailure(Exception e);
}
