package com.xlythe.textmanager.text.concurrency;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

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
    public void get(Callback<T> callback) {
        sExecutorService.submit(() -> {
            // We got the result, but we're still on the background thread.
            T result = get();

            // If we know how to, post to get back to the calling thread.
            if (mForegroundHandler != null) {
                mForegroundHandler.post(() -> callback.get(result));
            } else {
                // Otherwise, return from our background thread.
                callback.get(result);
            }
        });
    }

    @NonNull
    @Override
    public String toString() {
        return get().toString();
    }
}
