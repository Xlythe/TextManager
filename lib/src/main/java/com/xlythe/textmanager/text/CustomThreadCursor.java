package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;

import java.text.SimpleDateFormat;

/**
 * Created by Niko on 5/23/15.
 */
public class CustomThreadCursor extends CursorWrapper {

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
    private String mName;

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

    public String getDate(){
        mDate = this.getString(this.getColumnIndex(Telephony.Sms.DATE));
        return mDate;
    }

    public String getFormattedDate(){
        return dateFormatter(getDate());
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
}
