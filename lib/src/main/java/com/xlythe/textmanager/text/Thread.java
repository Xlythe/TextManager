package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.xlythe.textmanager.MessageThread;

import java.io.Serializable;

/**
 * An SMS conversation
 */
public class Thread implements MessageThread<Text>, Serializable {

    long mThreadId;
    int mCount;
    int mUnreadCount;
    Text mText;

    protected Thread(Context context, Cursor cursor) {
        mThreadId = cursor.getLong(cursor.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.THREAD_ID));
        mCount = 0;
        mUnreadCount = 0;
        buildLastMessage(context, mThreadId);
    }

    public void buildLastMessage(Context context, long threadId) {
        ContentResolver contentResolver = context.getContentResolver();
        final String[] projection = TextManager.PROJECTION;
        final Uri uri = Uri.parse(Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI +"/"+ threadId);
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
        // TODO: getUnreadCount()
        return 0;
    }

    @Override
    public Text getLatestMessage() {
        return mText;
    }
}