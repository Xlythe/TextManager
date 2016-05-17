package com.xlythe.textmanager.text;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.Log;

import com.xlythe.textmanager.MessageThread;
import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;
import com.xlythe.textmanager.text.util.Utils;

import static com.xlythe.textmanager.text.TextManager.TAG;

/**
 * An SMS conversation
 */
public final class Thread implements MessageThread<Text>, Parcelable {
    private final long mThreadId;
    private Integer mCount;
    private Integer mUnreadCount;
    private Text mText;

    // Thread ID isn't always different so need to change it here only
    // Maybe just change the conversations thread ID but that would be confusing
    private static final String THREAD_ID;
    static {
        if(android.os.Build.MANUFACTURER.equals(Mock.MANUFACTURER_SAMSUNG) && android.os.Build.VERSION.SDK_INT < 19) {
            THREAD_ID = BaseColumns._ID;
        } else {
            THREAD_ID = Mock.Telephony.Sms.Conversations.THREAD_ID;
        }
    }

    protected Thread(Cursor cursor) {
        mThreadId = cursor.getLong(cursor.getColumnIndexOrThrow(THREAD_ID));

        Cursor data2 = ((ThreadCursor) cursor).getUnreadCursor();
        int unreadCount = 0;
        if (data2 != null) {
            data2.moveToFirst();
            while (data2.moveToNext()) {
                if (data2.getLong(data2.getColumnIndex(THREAD_ID)) == mThreadId) {
                    unreadCount++;
                }
            }
        }
        mUnreadCount = unreadCount;
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
        mCount = (Integer) in.readSerializable();
        mUnreadCount = (Integer) in.readSerializable();
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
        if (mCount == null) {
            throw new IllegalStateException("getCount() is an expensive call. " +
                    "Call getCount(Context) first to load the count.");
        }
        return mCount;
    }

    private synchronized void setCount(int count) {
        mCount = count;
    }

    public synchronized Future<Integer> getCount(final Context context) {
        if (mCount != null) {
            return new Present<>(mCount);
        } else {
            return new FutureImpl<Integer>() {
                @Override
                public Integer get() {
                    String proj = String.format("%s=%s", THREAD_ID, mThreadId);
                    Uri uri = Mock.Telephony.Sms.Inbox.CONTENT_URI;
                    Cursor c = context.getContentResolver().query(uri, null, proj, null, null);
                    int count = c.getCount();
                    c.close();
                    setCount(count);
                    return count;
                }
            };
        }
    }

    @Override
    public synchronized int getUnreadCount() {
        if (mUnreadCount == null) {
            throw new IllegalStateException("getUnreadCount() is an expensive call. " +
                    "Call getUnreadCount(Context) first to load the count.");
        }
        return mUnreadCount;
    }

    private synchronized void setLatestMessage(Text text) {
        mText = text;
    }

    public synchronized Future<Text> getLatestMessage(final Context context) {
        if (mText != null) {
            return new Present<>(mText);
        } else {
            return new FutureImpl<Text>() {
                @Override
                public Text get() {
                    Text text;
                    Cursor textCursor = TextManager.getInstance(context).getMessageCursor(getId());
                    if (textCursor.moveToLast()) {
                        text = new Text(context, textCursor);
                    } else {
                        Log.w(TAG, "Failed to find a text for Thread: " + getId());
                        text = Text.EMPTY_TEXT;
                    }
                    textCursor.close();
                    setLatestMessage(text);
                    return text;
                }
            };
        }
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
        out.writeSerializable(mCount);
        out.writeSerializable(mUnreadCount);
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
        private android.database.Cursor mUnreadCursor;
        public ThreadCursor(android.database.Cursor cursor, android.database.Cursor unreadCursor) {
            super(cursor);
            mUnreadCursor = unreadCursor;
        }

        private android.database.Cursor getUnreadCursor() {
            return mUnreadCursor;
        }

        public Thread getThread() {
            return new Thread(this);
        }
    }
}