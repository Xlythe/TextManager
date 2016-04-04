package com.xlythe.textmanager.text.concurrency;

/**
 * Represents a variable that may or may not have been loaded yet. Text, Thread, and Contact
 * have their data spread out over several databases and so not all of their variables may have
 * been initialized.
 */
public interface Future<T> {
    /**
     * Returns the value with a blocking call.
     */
    T get();

    /**
     * Returns the value some time in the future.
     */
    void get(Future.Callback<T> callback);

    /**
     * A callback for once the value has loaded.
     */
    interface Callback<T> {
        void get(T instance);
    }
}
