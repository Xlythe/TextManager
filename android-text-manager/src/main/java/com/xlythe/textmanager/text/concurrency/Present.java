package com.xlythe.textmanager.text.concurrency;

import androidx.annotation.NonNull;

/**
 * Represents a variable that has already been loaded.
 */
public class Present<T> implements Future<T> {
    private final T mInstance;

    public Present(T instance) {
        mInstance = instance;
    }

    @Override
    public T get() {
        return mInstance;
    }

    @Override
    public void get(Future.Callback<T> callback) {
        callback.get(mInstance);
    }

    @NonNull
    @Override
    public String toString() {
        return get().toString();
    }
}
