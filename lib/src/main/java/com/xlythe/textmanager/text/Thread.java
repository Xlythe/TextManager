package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.xlythe.textmanager.MessageThread;
import com.xlythe.textmanager.text.util.Utils;

import java.io.Serializable;

/**
 * An SMS conversation
 */
public final class Thread implements MessageThread<Text>, Parcelable {
    private final long mThreadId;
    private final int mCount;
    private final int mUnreadCount;
    private final Text mText;

    protected Thread(Context context, Cursor cursor) {
        mThreadId = cursor.getLong(cursor.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.THREAD_ID));
        mCount = 0;
        mText = new Text(context, cursor);
        String proj = String.format("%s=%s AND %s=%s",
                Mock.Telephony.Sms.READ, 0,
                Mock.Telephony.Sms.THREAD_ID, mThreadId);
        Uri uri = Mock.Telephony.Sms.Inbox.CONTENT_URI;
        Cursor c = context.getContentResolver().query(uri, null, proj, null, null);
        mUnreadCount = c.getCount();
        c.close();
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
        return mCount;
    }

    @Override
    public int getUnreadCount() {
        return mUnreadCount;
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