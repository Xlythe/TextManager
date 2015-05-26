package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.Telephony;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    public String getFormattedDate(){
        mDate = this.getString(this.getColumnIndex(Telephony.Sms.DATE));
        return dateFormatter(mDate);
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

    public String getThreadId(){
        return mThreadId;
    }

    public String getType(){
        mType = this.getString(this.getColumnIndex(Telephony.Sms.TYPE));
        return mType;
    }

    private String dateFormatter(String date){
        Long time = System.currentTimeMillis()-Long.parseLong(date);
        if(time<60000){
            // Now
            return "Now";
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
        else if (time>=7200000 && time<86400000) {
            // 3:09 PM
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
            return formatter.format(time);
        }
        else if (time>=86400000 && time/1000<31560000) {
            // Apr 15, 3:09PM
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, h:mma");
            return formatter.format(time);
        }
        else {
            // 4/15/14 3:09PM
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy h:mma");
            return formatter.format(time);
        }
    }
}
