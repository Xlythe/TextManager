package com.xlythe.textmanager.text.concurrency;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents a variable that needs to be loaded.
 */
public abstract class FutureImpl<T> implements Future<T> {
    private static final ExecutorService sExecutorService = Executors.newSingleThreadExecutor();

    // This handler posts to the thread it's created on.
    private final Handler mForegroundHandler;

    public FutureImpl() {
        if (Looper.myLooper() != null) {
            mForegroundHandler = new Handler();
        } else {
            mForegroundHandler = null;
        }
    }

    @Override
    public void get(final Callback<T> callback) {
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                // We got the result, but we're still on the background thread.
                final T result = get();

                // If we know how to, post to get back to the calling thread.
                if (mForegroundHandler != null) {
                    mForegroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.get(result);
                        }
                    });
                } else {
                    // Otherwise, return from our background thread.
                    callback.get(result);
                }
            }
        });
    }

    @Override
    public String toString() {
        return get().toString();
    }
}
