package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.provider.Telephony;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.User;

import java.util.List;

/**
 * Either a sms or a mms
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
    private String mRead;
    private String mReplyPathPresent;
    private String mServiceCenter;
    private String mSeen;
    private String mStatus;
    private String mSubject;
    private String mThreadId;
    private String mType;

    /**
     * We don't want anyone to create a text without using the builder
     * */
    protected Text(Cursor c) {
        mId = c.getString(c.getColumnIndex(Telephony.Sms._ID));
        mAddress = c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS));
        mBody = c.getString(c.getColumnIndex(Telephony.Sms.BODY));
        mCreator = c.getString(c.getColumnIndex(Telephony.Sms.CREATOR));
        mDate = c.getString(c.getColumnIndex(Telephony.Sms.DATE));
        mDateSent = c.getString(c.getColumnIndex(Telephony.Sms.DATE_SENT));
        mErrorCode = c.getString(c.getColumnIndex(Telephony.Sms.ERROR_CODE));
        mLocked = c.getString(c.getColumnIndex(Telephony.Sms.LOCKED));
        mPerson = c.getString(c.getColumnIndex(Telephony.Sms.PERSON));
        mRead =c.getString(c.getColumnIndex(Telephony.Sms.READ));
        mReplyPathPresent = c.getString(c.getColumnIndex(Telephony.Sms.REPLY_PATH_PRESENT));
        mServiceCenter = c.getString(c.getColumnIndex(Telephony.Sms.SERVICE_CENTER));
        mSeen = c.getString(c.getColumnIndex(Telephony.Sms.SEEN));
        mStatus = c.getString(c.getColumnIndex(Telephony.Sms.STATUS));
        mSubject = c.getString(c.getColumnIndex(Telephony.Sms.SUBJECT));
        mThreadId = c.getString(c.getColumnIndex(Telephony.Sms.THREAD_ID));
        mType = c.getString(c.getColumnIndex(Telephony.Sms.TYPE));
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

    public String getSeen(){
        return mSeen;
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
     * Return true if the user has already seen this message.
     * */
    public boolean isRead() {
        return true;
    }

    /**
     * Return true if this message is currently being sent.
     * */
    public boolean isSending() {
        return false;
    }

    /**
     * Return true if this message failed to send.
     * */
    public boolean notSent() {
        return false;
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

    /**
     * Deletes this message.
     * */
    public void delete() {

    }

    /**
     * Deletes this message.
     * */
    public void delete(MessageCallback<Void> callback) {

    }

    /**
     * Send this message to the recipient.
     *
     * You may throw an error if the message shouldn't be sent.
     * */
    public void send() {

    }

    /**
     * Send this message to the recipient.
     *
     * You may throw an error if the message shouldn't be sent.
     * */
    public void send(MessageCallback<Void> callback) {

    }

    @Override
    public String toString() {
        return "fix this shit";
    }
}