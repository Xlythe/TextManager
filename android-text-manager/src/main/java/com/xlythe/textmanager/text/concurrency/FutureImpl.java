package com.xlythe.textmanager.text.concurrency;

import android.os.Handler;
import android.os.Looper;

/**
 * Represents a variable that needs to be loaded.
 */
public abstract class FutureImpl<T> implements Future<T> {
    // This handler posts to the thread it's created on.
    private final Handler mHandler = new Handler();

    @Override
    public void get(final Callback<T> callback) {
        new Thread() {
            @Override
            public void run() {
                // We got the result, but we're still on the background thread.
                final T result = get();

                // Post to get back to the calling thread.
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.get(result);
                    }
                });
            }
        }.start();
    }

    @Override
    public String toString() {
        return get().toString();
    }
}
