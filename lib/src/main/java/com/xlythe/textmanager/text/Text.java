package com.xlythe.textmanager.text;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageCallback;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;

/**
 * Either an sms or an mms
 */
public class Text implements Message {
    private static final String TAG = Text.class.getSimpleName();

    private long mId;
    private String mAddress;
    private String mBody;
    private String mCreator;
    private long mDate;
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

    private Uri mMmsImageUri;

    /**
     * We don't want anyone to create a text without using the builder
     */
    private Text() {

    }

    /**
     * Protected constructor for creating Threads.
     *
     * @param c cursor
     */
    protected Text(Cursor c) {
        if (!isMMS(c)) {
            Log.d(TAG, "I'm a SMS!");
            mId = c.getLong(c.getColumnIndex(Telephony.Sms._ID));
            mAddress = c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS));
            mBody = c.getString(c.getColumnIndex(Telephony.Sms.BODY));
            mCreator = c.getString(c.getColumnIndex(Telephony.Sms.CREATOR));
            mDate = c.getLong(c.getColumnIndex(Telephony.Sms.DATE));
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
            Log.d(TAG, String.format("Spew... id=%s, creator=%s, date=%s, " +
                            "dateSent=%s, locked=%s, read=%s, seen=%s, status=%s, " +
                            "subject=%s, threadId=%s", mId, mCreator, mDate, mDateSent, mLocked,
                    mRead, mSeen, mStatus, mSubject, mThreadId));
        } else {
            Log.d(TAG, "I'm a MMS!");
            mId = c.getLong(c.getColumnIndex(Telephony.Mms._ID));
            mCreator = c.getString(c.getColumnIndex(Telephony.Mms.CREATOR));
            mDate = c.getLong(c.getColumnIndex(Telephony.Mms.DATE)) * 1000;
            mDateSent = c.getString(c.getColumnIndex(Telephony.Mms.DATE_SENT));
            mLocked = c.getString(c.getColumnIndex(Telephony.Mms.LOCKED));
            mRead = c.getInt(c.getColumnIndex(Telephony.Mms.READ)) == 1;
            mSeen = c.getInt(c.getColumnIndex(Telephony.Mms.SEEN)) == 1;
            mStatus = c.getInt(c.getColumnIndex(Telephony.Mms.STATUS));
            mSubject = c.getString(c.getColumnIndex(Telephony.Mms.SUBJECT));
            mThreadId = c.getLong(c.getColumnIndex(Telephony.Mms.THREAD_ID));
            Log.d(TAG, String.format("Spew... id=%s, creator=%s, date=%s, " +
                    "dateSent=%s, locked=%s, read=%s, seen=%s, status=%s, " +
                    "subject=%s, threadId=%s", mId, mCreator, mDate, mDateSent, mLocked,
                    mRead, mSeen, mStatus, mSubject, mThreadId));
        }
    }

    private static boolean isMMS(Cursor query) {
        int index = query.getColumnIndex(Telephony.Mms.CONTENT_TYPE);
        if (index != -1) {
            return "application/vnd.wap.multipart.related".equals(query.getString(index));
        }
        return false;
    }

    private static String getMmsAddress(Context context, long id) {
        String selectionAdd = new String("msg_id=" + id);
        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        Cursor cursor = context.getContentResolver().query(uriAddress, null, selectionAdd, null, null);
        String name = null;
        if(cursor.moveToFirst()) {
            String number = cursor.getString(cursor.getColumnIndex("address"));
            if(number != null) {
                try {
                    Long.parseLong(number.replace("-", ""));
                    name = number;
                }
                catch(NumberFormatException nfe) {
                    if(name == null) {
                        name = number;
                    }
                }
            }
        }
        cursor.close();
        return name;
    }

    public long getId() {
        return mId;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getBody() {
        return mBody;
    }

    public String getCreator() {
        return mCreator;
    }

    public long getDate() {
        return mDate;
    }

    public String getDateSent() {
        return mDateSent;
    }

    public String getErrorCode() {
        return mErrorCode;
    }

    public String getLocked() {
        return mLocked;
    }

    public String getPerson() {
        return mPerson;
    }

    public String getReplyPathPresent() {
        return mReplyPathPresent;
    }

    public String getServiceCenter() {
        return mServiceCenter;
    }


    public String getSubject() {
        return mSubject;
    }

    public long getThreadId() {
        return mThreadId;
    }

    public int getType() {
        return mType;
    }

    public boolean sentByUser() {
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
        switch (mStatus) {
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

    public Uri getMmsImageUri(Context context){
        setMmsImage(context);
        return mMmsImageUri;
    }

    private void setMmsImage(Context context) {
        String selectionPart = "mid=" + mId;
        Uri uri = Uri.parse("content://mms/part");
        Cursor cPart = context.getContentResolver().query(uri, null, selectionPart, null, null);
        if (cPart.moveToFirst()) {
            do {
                String partId = cPart.getString(cPart.getColumnIndex("_id"));
                String type = cPart.getString(cPart.getColumnIndex("ct"));
                if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
                        "image/gif".equals(type) || "image/jpg".equals(type) ||
                        "image/png".equals(type)) {
                    mMmsImageUri = Uri.parse("content://mms/part/" + partId);
                }
            } while (cPart.moveToNext());
        }
        cPart.close();
    }

    @Override
    public String toString() {
        return mBody;
    }

    public static class Builder {
        private String mMessage;
        private String mRecipient;

        public Builder() {

        }

        public Builder drawable(Drawable drawable) {
            // TODO support mms
            return this;
        }

        public Builder recipient(String recipient) {
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
            text.mAddress = mRecipient;
            return text;
        }
    }
}