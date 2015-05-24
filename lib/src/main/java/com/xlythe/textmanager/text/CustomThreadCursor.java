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
    private String mThreadId;
    private String mType;

    public CustomThreadCursor(Cursor c, String threadId) {
        super(c);
        mThreadId = threadId;
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

    public String getType(){
        mType = this.getString(this.getColumnIndex(Telephony.Sms.TYPE));
        return mType;
    }
    public int getColor() {
        int num = Integer.parseInt(mThreadId) % 12;
        switch (num) {
            case 0:
                return 0xffdb4437;
            case 1:
                return 0xffe91e63;
            case 2:
                return 0xff9c27b0;
            case 3:
                return 0xff3f51b5;
            case 4:
                return 0xff039be5;
            case 5:
                return 0xff4285f4;
            case 6:
                return 0xff0097a7;
            case 7:
                return 0xff009688;
            case 8:
                return 0xff0f9d58;
            case 9:
                return 0xff689f38;
            case 10:
                return 0xffef6c00;
            case 11:
                return 0xffff5722;
            default:
                return 0xff757575;
        }
    }
}
