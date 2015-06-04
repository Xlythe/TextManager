package com.xlythe.textmanager.text;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Telephony;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageCallback;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Either an sms or an mms
 */
public class Text implements Message {
    private String mId;
    private String mAddress;
    private String mBody;
    private String mCreator;
    private String mDate;
    private String mDateSent;
    private String mErrorCode;
    private String mLocked;
    private String mPerson;
    private boolean mRead;
    private String mReplyPathPresent;
    private String mServiceCenter;
    private boolean mSeen;
    private int mStatus;
    private String mSubject;
    private long mThreadId;
    private int mType;

    /**
     * We don't want anyone to create a text without using the builder
     * */
    private Text() {

    }

    /**
     * We don't want anyone to create a text without using the builder
     * */
    protected Text(Cursor c) {
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            mId = c.getString(c.getColumnIndex(Telephony.Sms._ID));
            mAddress = c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS));
            mBody = c.getString(c.getColumnIndex(Telephony.Sms.BODY));
            mCreator = c.getString(c.getColumnIndex(Telephony.Sms.CREATOR));
            mDate = c.getString(c.getColumnIndex(Telephony.Sms.DATE));
            mDateSent = c.getString(c.getColumnIndex(Telephony.Sms.DATE_SENT));
            mErrorCode = c.getString(c.getColumnIndex(Telephony.Sms.ERROR_CODE));
            mLocked = c.getString(c.getColumnIndex(Telephony.Sms.LOCKED));
            mPerson = c.getString(c.getColumnIndex(Telephony.Sms.PERSON));
            mRead = c.getInt(c.getColumnIndex(Telephony.Sms.READ)) == 1;
            mReplyPathPresent = c.getString(c.getColumnIndex(Telephony.Sms.REPLY_PATH_PRESENT));
            mServiceCenter = c.getString(c.getColumnIndex(Telephony.Sms.SERVICE_CENTER));
            mSeen = c.getInt(c.getColumnIndex(Telephony.Sms.SEEN)) == 1;
            mStatus = c.getInt(c.getColumnIndex(Telephony.Sms.STATUS));
            mSubject = c.getString(c.getColumnIndex(Telephony.Sms.SUBJECT));
            mThreadId = c.getLong(c.getColumnIndex(Telephony.Sms.THREAD_ID));
            mType = c.getInt(c.getColumnIndex(Telephony.Sms.TYPE));
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

    public String getCreator(){
        return mCreator;
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

    public String getDateSent(){
        return mDateSent;
    }

    public String getErrorCode(){
        return mErrorCode;
    }

    public String getPerson(){
        return mPerson;
    }

    public String getSubject(){
        return mSubject;
    }

    public long getThreadId(){
        return mThreadId;
    }

    public int getType(){
        return mType;
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

    public Status getStatus() {
        switch(mStatus) {
            case Telephony.Sms.STATUS_PENDING:
                return Status.SENDING;
            case Telephony.Sms.STATUS_FAILED:
                return Status.FAILED;
            case Telephony.Sms.STATUS_COMPLETE:
                if (mSeen) {
                    return Status.SEEN;
                } else {
                    return Status.SENT;
                }
            case Telephony.Sms.STATUS_NONE:
                if (mRead) {
                    return Status.READ;
                } else {
                    return Status.UNREAD;
                }
        }

        throw new IllegalStateException("Could not determine Message state");
    }

    /**
     * Mark this message as having been read.
     * */
    public void markAsRead() {

    }

    /**
     * Mark this message as having been read.
     * */
    public void markAsRead(MessageCallback<Void> callback) {

    }

    @Override
    public String toString() {
        return mBody;
    }

    public static class Builder {
        private final Context mContext;
        private String mMessage;
        private TextUser mRecipient;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder drawable(Drawable drawable) {
            // TODO support mms
            return this;
        }

        public Builder recipient(TextUser recipient) {
            mRecipient = recipient;
            return this;
        }

        public Builder message(String message) {
            mMessage = message;
            return this;
        }

        public Text build() {
            Text text = new Text();
            text.mBody = mMessage;
            text.mAddress = mRecipient.getPhoneNumber();
            return text;
        }
    }
}