package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.xlythe.textmanager.MessageThread;

import java.io.Serializable;

/**
 * An SMS conversation
 */

public class Thread implements MessageThread<Text>, Parcelable {

    long mThreadId;
    int mCount;
    int mUnreadCount;
    Text mText;

    protected Thread(Context context, Cursor cursor) {
        mThreadId = cursor.getLong(cursor.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.THREAD_ID));
        mCount = 0;
        buildLastMessage(context);
        final Uri uri = Uri.parse("content://sms/inbox");
        Cursor c = context.getContentResolver().query(uri,
                null,
                "read = 0 AND thread_id = " + mThreadId,
                null,
                null);
        mUnreadCount = c.getCount();
        c.close();
    }

    private void buildLastMessage(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        final String[] projection = TextManager.PROJECTION;
        final Uri uri = Uri.parse(Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI +"/"+ mThreadId);
        final String order = "normalized_date ASC";
        Cursor c = contentResolver.query(uri, projection, null, null, order);
        if (c!=null && c.moveToLast()) {
            mText = new Text(context, c);
            c.close();
        }
    }

    @Override
    public String getId(){
        return Long.toString(mThreadId);
    }

    @Override
    public int getCount() {
        // TODO: getCount()
        return 0;
    }

    @Override
    public int getUnreadCount() {
        return mUnreadCount;
    }

    @Override
    public Text getLatestMessage() {
        return mText;
    }

    private Thread(Parcel in) {
        mThreadId = in.readLong();
        mCount = in.readInt();
        mUnreadCount = in.readInt();
        mText = in.readParcelable(Text.class.getClassLoader());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mThreadId);
        out.writeInt(mCount);
        out.writeInt(mUnreadCount);
        out.writeParcelable(mText, flags);
    }

    public static final Parcelable.Creator<Thread> CREATOR = new Parcelable.Creator<Thread>() {
        public Thread createFromParcel(Parcel in) {
            return new Thread(in);
        }

        public Thread[] newArray(int size) {
            return new Thread[size];
        }
    };
}