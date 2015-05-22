package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageThread;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An SMS conversation
 */
public class TextThread implements MessageThread<Text>, Serializable {

    public static TextThread parse(Cursor cursor) {
        return new TextThread(cursor);
    }

    private String mId;
    private String mAddress;
    private String mBody;
    private String mDate;
    private String mDateSent;
    private String mErrorCode;
    private String mLocked;
    private String mPerson;
    private String mRead;
    private String mReplyPathPresent;
    private String mServiceCenter;
    private String mStatus;
    private String mSubject;
    private String mThreadId;
    private String mType;

    protected TextThread(Cursor c) {
        mId = c.getString(c.getColumnIndex(Telephony.Sms._ID));
        mAddress = c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS));
        mBody = c.getString(c.getColumnIndex(Telephony.Sms.BODY));
        mDate = c.getString(c.getColumnIndex(Telephony.Sms.DATE));
        mDateSent = c.getString(c.getColumnIndex(Telephony.Sms.DATE_SENT));
        mErrorCode = c.getString(c.getColumnIndex(Telephony.Sms.ERROR_CODE));
        mLocked = c.getString(c.getColumnIndex(Telephony.Sms.LOCKED));
        mPerson = c.getString(c.getColumnIndex(Telephony.Sms.PERSON));
        mRead =c.getString(c.getColumnIndex(Telephony.Sms.READ));
        mReplyPathPresent = c.getString(c.getColumnIndex(Telephony.Sms.REPLY_PATH_PRESENT));
        mServiceCenter = c.getString(c.getColumnIndex(Telephony.Sms.SERVICE_CENTER));
        mStatus = c.getString(c.getColumnIndex(Telephony.Sms.STATUS));
        mSubject = c.getString(c.getColumnIndex(Telephony.Sms.SUBJECT));
        mThreadId = c.getString(c.getColumnIndex(Telephony.Sms.THREAD_ID));
        mType = c.getString(c.getColumnIndex(Telephony.Sms.TYPE));
    }

    public Cursor getTextCursor(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        final String[] projection = new String[]{
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.CREATOR,
                Telephony.Sms.DATE,
                Telephony.Sms.DATE_SENT,
                Telephony.Sms.ERROR_CODE,
                Telephony.Sms.LOCKED,
                Telephony.Sms.PERSON,
                Telephony.Sms.READ,
                Telephony.Sms.REPLY_PATH_PRESENT,
                Telephony.Sms.SERVICE_CENTER,
                Telephony.Sms.SEEN,
                Telephony.Sms.STATUS,
                Telephony.Sms.SUBJECT,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.TYPE,
        };
        final String order = Telephony.Sms.DEFAULT_SORT_ORDER;

        Uri uri = Uri.parse("content://mms-sms/conversations/" + mThreadId);

        return contentResolver.query(uri, projection, null, null, order);
    }

    public List<Text> getMessages(Context context){
        List<Text> list = new ArrayList<>();
        Cursor c = getTextCursor(context);
        if (c.moveToFirst()) {
            do {
                list.add(new Text(c));
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public String getId(){
        return mId;
    }

    public String getAddress(){
        return mAddress;
    }

    public String getBody(){
        return mBody;
    }

    public String getDate(){
        return mDate;
    }

    public String getDateSent(){
        return mDateSent;
    }

    public String getErrorCode(){
        return mErrorCode;
    }

    public String getLocked(){
        return mLocked;
    }

    public String getPerson(){
        return mPerson;
    }

    public String getRead(){
        return mRead;
    }

    public String getReplyPathPresent(){
        return mReplyPathPresent;
    }

    public String getServiceCenter(){
        return mServiceCenter;
    }

    public String getStatus(){
        return mStatus;
    }

    public String getSubject(){
        return mSubject;
    }

    public String getThreadId(){
        return mThreadId;
    }

    public String getType(){
        return mType;
    }

    /**
     * Get the {limit} most recent messages.
     * */
    public List<Text> getMessages(int limit) {
        return null;
    }

    /**
     * Return the number of unread messages in this thread.
     * */
    public int getUnreadCount() {
        return 0;
    }

    /**
     * Mark all messages in this thread as read.
     * */
    public void markRead() {
//        ContentValues values = new ContentValues();
//        values.put("read", true);
//        mContext.getContentResolver().update(Uri.parse("content://sms/inbox"), values,
//                "thread_id=" + mThreadId + " AND read=0", null);
    }

    /**
     * Mark all messages in this thread as read.
     * */
    public void markRead(MessageCallback<Void> callback) {

    }

    /**
     * Deletes this thread.
     * */
    public void delete() {

    }

    /**
     * Deletes this thread.
     * */
    public void delete(MessageCallback<Void> callback) {

    }
    @Override
    public String toString() {
        return "fix this shit";
    }
}