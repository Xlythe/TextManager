package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.Telephony;

/**
 * Created by Niko on 5/23/15.
 */
public class CustomThreadCursor extends CursorWrapper {

    private String mId;
    private String mAddress;
    private String mBody;
    private String mCreator;
    private String mDate;
    private String mDateSent;
    private String mErrorCode;
    private String mPerson;
    private String mSubject;
    private long mThreadId;
    private String mType;

    public CustomThreadCursor(Cursor c) {
        super(c);
    }

    public String getId(){
        mId = this.getString(this.getColumnIndex(Telephony.Sms._ID));
        return mId;
    }

    public String getAddress(){
        mAddress = this.getString(this.getColumnIndex(Telephony.Sms.ADDRESS));
        return mAddress;
    }

    public String getBody(){
        mBody = this.getString(this.getColumnIndex(Telephony.Sms.BODY));
        return mBody;
    }

    public String getCreator(){
        mCreator = this.getString(this.getColumnIndex(Telephony.Sms.CREATOR));
        return mCreator;
    }

    public String getDate(){
        mDate = this.getString(this.getColumnIndex(Telephony.Sms.DATE));
        return mDate;
    }

    public String getDateSent(){
        mDateSent = this.getString(this.getColumnIndex(Telephony.Sms.DATE_SENT));
        return mDateSent;
    }

    public String getErrorCode(){
        mErrorCode = this.getString(this.getColumnIndex(Telephony.Sms.ERROR_CODE));
        return mErrorCode;
    }

    public String getPerson(){
        mPerson = this.getString(this.getColumnIndex(Telephony.Sms.PERSON));
        return mPerson;
    }

    public String getSubject(){
        mSubject = this.getString(this.getColumnIndex(Telephony.Sms.SUBJECT));
        return mSubject;
    }

    public long getThreadId(){
        mThreadId = this.getLong(this.getColumnIndex(Telephony.Sms.THREAD_ID));
        return mThreadId;
    }

    public String getType(){
        mType = this.getString(this.getColumnIndex(Telephony.Sms.TYPE));
        return mType;
    }
}
