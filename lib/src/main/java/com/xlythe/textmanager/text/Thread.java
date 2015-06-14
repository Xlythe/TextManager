package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.provider.Telephony;

import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageThread;

import java.io.Serializable;
import java.text.SimpleDateFormat;

/**
 * An SMS conversation
 */
public class Thread implements MessageThread<Text>, Serializable {
    private String mId;
    private String mAddress;
    private String mBody;
    private long mDate;
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

    /**
     *  Protected constructor for creating Threads.
     * @param c cursor
     */
    protected Thread(Cursor c) {
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            mId = c.getString(c.getColumnIndex(Telephony.Sms._ID));
            mAddress = c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS));
            mBody = c.getString(c.getColumnIndex(Telephony.Sms.BODY));
            mDate = c.getLong(c.getColumnIndex(Telephony.Sms.DATE));
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

    public long getDate(){
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

    public long getThreadId(){
        return mThreadId;
    }

    public String getType(){
        return mType;
    }
}