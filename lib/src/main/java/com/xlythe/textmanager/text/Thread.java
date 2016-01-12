package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageThread;
import com.xlythe.textmanager.text.util.Utils;

import java.io.Serializable;
import java.util.List;

/**
 * An SMS conversation
 */
public final class Thread implements MessageThread<Text>, Parcelable {
    private final long mThreadId;
    private int mCount = -1; // Lazy loading
    private int mUnreadCount = -1; // Lazy loading
    private final Text mText;

    // Thread id isn't always different so need to change it here only
    // Maybe just change the conversations thread id but that would be confusing
    private static final String THREAD_ID;
    static {
        if(android.os.Build.MANUFACTURER.equals(Mock.MANUFACTURER_SAMSUNG) && android.os.Build.VERSION.SDK_INT < 19) {
            THREAD_ID = BaseColumns._ID;
        } else {
            THREAD_ID = Mock.Telephony.Sms.Conversations.THREAD_ID;
        }
    }

    protected Thread(Context context, Cursor cursor) {
        mThreadId = cursor.getLong(cursor.getColumnIndexOrThrow(THREAD_ID));
        Cursor textCursor = TextManager.getInstance(context).getMessageCursor(Long.toString(mThreadId));
        textCursor.moveToLast();
        mText = new Text(context, textCursor);
    }

    /**
     * @VisibleForTesting
     * */
    Thread(long id, int count, int unreadCount, Text text) {
        mThreadId = id;
        mCount = count;
        mUnreadCount = unreadCount;
        mText = text;
    }

    private Thread(Parcel in) {
        mThreadId = in.readLong();
        mCount = in.readInt();
        mUnreadCount = in.readInt();
        mText = in.readParcelable(Text.class.getClassLoader());
    }

    @Override
    public String getId(){
        return Long.toString(mThreadId);
    }

    public long getIdAsLong() {
        return mThreadId;
    }

    @Override
    public int getCount() {
        if (mCount == -1) {
            throw new IllegalStateException("getCount() is an expensive call. " +
                    "Call getCount(Context) first to load the count.");
        }
        return mCount;
    }

    public int getCount(Context context) {
        String proj = String.format("%s=%s", THREAD_ID, mThreadId);
        Uri uri = Mock.Telephony.Sms.Inbox.CONTENT_URI;
        Cursor c = context.getContentResolver().query(uri, null, proj, null, null);
        mCount = c.getCount();
        c.close();
        return getCount();
    }

    @Override
    public synchronized int getUnreadCount() {
        if (mUnreadCount == -1) {
            throw new IllegalStateException("getUnreadCount() is an expensive call. " +
                    "Call getUnreadCount(Context) first to load the count.");
        }
        return mUnreadCount;
    }

    private synchronized void setUnreadCount(int count) {
        mUnreadCount = count;
    }

    public int getUnreadCount(Context context) {
        String proj = String.format("%s=%s AND %s=%s",
                THREAD_ID, mThreadId,
                Mock.Telephony.Sms.READ, 0);
        Uri uri = Mock.Telephony.Sms.Inbox.CONTENT_URI;
        Cursor c = context.getContentResolver().query(uri, null, proj, null, null);
        setUnreadCount(c.getCount());
        c.close();
        return getUnreadCount();
    }

    public void getUnreadCount(final Context context, final MessageCallback<Integer> callback) {
        // Create a handler so we call back on the same thread we were called on
        final Handler handler = new Handler();

        // Then start a background thread
        new java.lang.Thread() {
            @Override
            public void run() {
                // getThreads is a long running operation
                final int count = getUnreadCount(context);

                // Return the list in the callback
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(count);
                    }
                });
            }
        }.start();
    }

    public boolean hasLoadedUnreadCount() {
        return mUnreadCount != -1;
    }

    @Override
    public Text getLatestMessage() {
        return mText;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Thread) {
            Thread a = (Thread) o;
            return Utils.equals(mThreadId, a.mThreadId)
                    && Utils.equals(mCount, a.mCount)
                    && Utils.equals(mUnreadCount, a.mUnreadCount)
                    && Utils.equals(mText, a.mText);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(mThreadId)
                + Utils.hashCode(mCount)
                + Utils.hashCode(mUnreadCount)
                + Utils.hashCode(mText);
    }

    @Override
    public String toString() {
        return String.format("Thread{id=%s, count=%s, unread_count=%s, text=%s}",
                mThreadId, mCount, mUnreadCount, mText);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mThreadId);
        out.writeInt(mCount);
        out.writeInt(mUnreadCount);
        out.writeParcelable(mText, Utils.describeContents(mText));
    }

    public static final Parcelable.Creator<Thread> CREATOR = new Parcelable.Creator<Thread>() {
        public Thread createFromParcel(Parcel in) {
            return new Thread(in);
        }

        public Thread[] newArray(int size) {
            return new Thread[size];
        }
    };

    public static class ThreadCursor extends CursorWrapper {
        private final Context mContext;

        public ThreadCursor(Context context, android.database.Cursor cursor) {
            super(cursor);
            mContext = context.getApplicationContext();
        }

        public Thread getThread() {
            return new Thread(mContext, this);
        }
    }
}