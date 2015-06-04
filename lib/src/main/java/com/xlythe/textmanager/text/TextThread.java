package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageThread;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * An SMS conversation
 */
public class TextThread implements MessageThread<Text>, Serializable {
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
    private long mThreadId;
    private String mType;

    protected TextThread(Cursor c) {
        if (android.os.Build.VERSION.SDK_INT >= 19) {
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
            mThreadId = c.getLong(c.getColumnIndex(Telephony.Sms.THREAD_ID));
            mType = c.getString(c.getColumnIndex(Telephony.Sms.TYPE));
        }
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

    public String getFormattedDate(){
        return dateFormatter(getDate());
    }


    private String dateFormatter(String date){
        Long lDate = Long.parseLong(date);
        Long time = System.currentTimeMillis()-lDate;
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");

        if(time<60000){
            // Just now
            return "Just now";
        }
        else if(time>=60000 && time<3600000){
            // 1 min, 2 mins
            if(time/60000==1)
                return time/60000+" min";
            else
                return time/60000+" mins";
        }
        else if (time>=3600000 && time<7200000) {
            // 1 hour
            if (time / 3600000 == 1)
                return time / 3600000 + " hour";
            else
                return time / 3600000 + " hours";
        }
        else if (time>=7200000 && f.format(lDate).equals(f.format(System.currentTimeMillis()))) {
            // 3:09 PM
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
            return formatter.format(lDate);
        }
        else if (time<604800000) {
            // Mon
            SimpleDateFormat formatter = new SimpleDateFormat("EEE");
            return formatter.format(lDate);
        }
        else if (time>=604800000 && time/1000<31560000) {
            // Apr 15
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d");
            return formatter.format(lDate);
        }
        else {
            // 4/15/14
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
            return formatter.format(lDate);
        }
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

    public long getThreadId(){
        return mThreadId;
    }

    public String getType(){
        return mType;
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