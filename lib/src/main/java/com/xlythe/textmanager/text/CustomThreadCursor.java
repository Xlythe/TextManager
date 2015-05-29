package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.Telephony;
import android.util.Log;

import com.xlythe.textmanager.Message;

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
    private long mThreadId;
    private int mType;
    private int mStatus;
    private boolean mRead;
    private boolean mSeen;

    public CustomThreadCursor(Cursor c, long threadId) {
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

    public Message.Status getStatus() {
        mStatus = this.getInt(this.getColumnIndex(Telephony.Sms.STATUS));
        mRead = this.getInt(this.getColumnIndex(Telephony.Sms.READ)) == 1;
        mSeen = this.getInt(this.getColumnIndex(Telephony.Sms.SEEN)) == 1;
        switch(mStatus) {
            case Telephony.Sms.STATUS_PENDING:
                return Message.Status.SENDING;
            case Telephony.Sms.STATUS_FAILED:
                return Message.Status.FAILED;
            case Telephony.Sms.STATUS_COMPLETE:
                if (mSeen) {
                    return Message.Status.SEEN;
                } else {
                    return Message.Status.SENT;
                }
            case Telephony.Sms.STATUS_NONE:
                if (mRead) {
                    return Message.Status.READ;
                } else {
                    return Message.Status.UNREAD;
                }
        }
        throw new IllegalStateException("Could not determine Message state");
    }

    public boolean sentByUser(){
        mType = getType();
        switch (mType) {
            case Telephony.Sms.MESSAGE_TYPE_INBOX:
                return false;
            case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
            case Telephony.Sms.MESSAGE_TYPE_FAILED:
            case Telephony.Sms.MESSAGE_TYPE_QUEUED:
            case Telephony.Sms.MESSAGE_TYPE_SENT:
            case Telephony.Sms.MESSAGE_TYPE_DRAFT:
                return true;
        }
        return false;
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

    public long getThreadId(){
        return mThreadId;
    }

    public int getType(){
        mType = this.getInt(this.getColumnIndex(Telephony.Sms.TYPE));
        return mType;
    }

    private String dateFormatter(String date){
        Long lDate = Long.parseLong(date);
        Long time = System.currentTimeMillis()-lDate;
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");

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
        else if (time>=7200000 && f.format(lDate).equals(f.format(System.currentTimeMillis()))) {
            // 3:09 PM
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
            return formatter.format(lDate);
        }
        else if (time<604800000) {
            //Mon 3:09PM
            SimpleDateFormat formatter = new SimpleDateFormat("EEE h:mma");
            return formatter.format(lDate);
        }
        else if (time>=604800000 && time/1000<31560000) {
            // Apr 15, 3:09PM
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, h:mma");
            return formatter.format(lDate);
        }
        else {
            // 4/15/14 3:09PM
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy h:mma");
            return formatter.format(lDate);
        }
    }
}
