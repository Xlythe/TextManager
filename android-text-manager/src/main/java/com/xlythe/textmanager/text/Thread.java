package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.xlythe.textmanager.MessageThread;
import com.xlythe.textmanager.text.util.Utils;

import java.util.List;

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
    static final String THREAD_ID;
    static {
        if(android.os.Build.MANUFACTURER.equals(Mock.MANUFACTURER_SAMSUNG)
                || android.os.Build.MANUFACTURER.equals(Mock.MANUFACTURER_HTC)
                || android.os.Build.MANUFACTURER.equals(Mock.MANUFACTURER_ZTE)) {
            THREAD_ID = BaseColumns._ID;
        } else {
            THREAD_ID = Mock.Telephony.Sms.Conversations.THREAD_ID;
        }
    }

    protected Thread(Cursor cursor) {
        mThreadId = cursor.getLong(cursor.getColumnIndexOrThrow(THREAD_ID));

        for (Text text : ((ThreadCursor) cursor).getTexts()) {
            if (text.getThreadIdAsLong() == mThreadId) {
                mText = text;
            }
        }

        Cursor unreadCursor = ((ThreadCursor) cursor).getUnreadCursor();
        int unreadCount = 0;
        if (unreadCursor != null) {
            unreadCursor.moveToFirst();
            while (unreadCursor.moveToNext()) {
                if (unreadCursor.getLong(unreadCursor.getColumnIndex(THREAD_ID)) == mThreadId) {
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

    int getUnreadCount() {
        return mUnreadCount;
    }

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
        private Cursor mUnreadCursor;
        private List<Text> mTexts;

        public ThreadCursor(Cursor cursor, Cursor unreadCursor, List<Text> texts) {
            super(cursor);
            mUnreadCursor = unreadCursor;
            mTexts = texts;
        }

        private Cursor getUnreadCursor() {
            return mUnreadCursor;
        }

        private List<Text> getTexts() {
            return mTexts;
        }

        public Thread getThread() {
            return new Thread(this);
        }
    }
}
