package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.Telephony;
import android.util.Log;

import java.text.SimpleDateFormat;

/**
 * Created by Niko on 5/23/15.
 */
public class CustomManagerCursor extends CursorWrapper {

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

    public CustomManagerCursor(Cursor c) {
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
        mThreadId = this.getString(this.getColumnIndex(Telephony.Sms.THREAD_ID));
        return mThreadId;
    }

    public String getType(){
        mType = this.getString(this.getColumnIndex(Telephony.Sms.TYPE));
        return mType;
    }

    public int getColor() {
        int num = Integer.parseInt(getThreadId()) % 12;
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

    private String dateFormatter(String date){
        Long dateLong = Long.parseLong(date);
        Long now = System.currentTimeMillis();
        Long time = now-dateLong;
        Log.v("time", time + "");
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
        else if (time>=7200000 && time<86400000) {
            // 3:09 PM
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
            return formatter.format(time);
        }
        else if (time>=86400000 && time<604800000) {
            // Mon
            SimpleDateFormat formatter = new SimpleDateFormat("EEE");
            return formatter.format(time);
        }
        else if (time>=604800000 && time/1000<31560000) {
            // Apr 15
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d");
            return formatter.format(time);
        }
        else {
            // 4/15/14
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
            return formatter.format(time);
        }
    }
}
