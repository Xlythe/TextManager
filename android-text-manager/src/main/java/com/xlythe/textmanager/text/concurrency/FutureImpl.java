package com.xlythe.textmanager.text.concurrency;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Represents a variable that needs to be loaded.
 */
public abstract class FutureImpl<T> implements Future<T> {
    private static final HandlerThread sBackgroundThread = new HandlerThread("FutureBackgroundThread");

    static {
        sBackgroundThread.start();
    }

    // This handler posts to the thread it's created on.
    private final Handler mForegroundHandler = new Handler();
    private final Handler mBackgroundHandler = new Handler(sBackgroundThread.getLooper());

    @Override
    public void get(final Callback<T> callback) {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                // We got the result, but we're still on the background thread.
                final T result = get();

                // Post to get back to the calling thread.
                mForegroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.get(result);
                    }
                });
            }
        });
    }

    @Override
    public String toString() {
        return get().toString();
    }
}
