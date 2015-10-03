package com.xlythe.textmanager.text;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.util.Log;

import com.xlythe.textmanager.Message;

import java.util.ArrayList;

/**
 * Either an sms or an mms
 */
public class Text implements Message, Comparable {
    private static final String TYPE_SMS = "sms";
    private static final String TYPE_MMS = "mms";
    private static final long SEC_TO_MILLI = 1000;

    @SuppressLint("NewApi")
    static final String[] MMS_PROJECTION = new String[]{
            BaseColumns._ID,
            Telephony.Mms.Part.CONTENT_TYPE,
            Telephony.Mms.Part.TEXT,
            Telephony.Mms.Part._DATA
    };

    static final String[] MMS_PROJECTION_PRE_LOLLIPOP = new String[]{
            BaseColumns._ID,
            "ct",
            "text",
            "_data"
    };

    private long mId;
    private long mThreadId;
    private long mDate;
    private String mAddress;
    private String mBody;
    private boolean mIncoming;
    private Uri mAttachment;
    private ArrayList<Bitmap> mAttachments = new ArrayList<>();
    private Context mContext;
    private boolean mIsMms = false;
    private long mMmsId;
    private String mMyNumber;

    private Text() { }

    public Text(Context context, Cursor cursor, String myNumber) {
        invalidate(context, cursor, myNumber);
    }

    void invalidate(Context context, Cursor cursor, String myNumber) {
        mId = -1;
        mThreadId = -1;
        mDate = -1;
        mAddress = null;
        mBody = null;
        mAttachment = null;
        mMmsId = -1;
        mMyNumber = null;

        mContext = context.getApplicationContext();
        String type = getMessageType(cursor);
        if (TYPE_SMS.equals(type)){
            mIsMms = false;
            parseSmsMessage(cursor);
        }
        else if (TYPE_MMS.equals(type)){
            mIsMms = true;
            parseMmsMessage(context, cursor, myNumber);
        }
        else {
            Log.w("TelephonyProvider", "Unknown Message Type");
        }
    }

    private String getMessageType(Cursor cursor) {
        int typeIndex;
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            typeIndex = cursor.getColumnIndex(Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN);
        } else {
            typeIndex = cursor.getColumnIndex("transport_type");
        }
        if (typeIndex < 0) {
            // Type column not in projection, use another discriminator
            String cType;
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                cType = cursor.getString(cursor.getColumnIndex(Telephony.Mms.CONTENT_TYPE));
            } else {
                cType = cursor.getString(cursor.getColumnIndex("ct"));
            }
            // If content type is present, this is an MMS message
            if (cType != null) {
                return TYPE_MMS;
            } else {
                return TYPE_SMS;
            }
        }
        else {
            return cursor.getString(typeIndex);
        }
    }

    private void parseSmsMessage(Cursor data) {
        mId = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID));
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            mThreadId = data.getLong(data.getColumnIndexOrThrow(Telephony.Sms.Conversations.THREAD_ID));
            mDate = data.getLong(data.getColumnIndexOrThrow(Telephony.Sms.Conversations.DATE));
            mAddress = data.getString(data.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
            mBody = data.getString(data.getColumnIndexOrThrow(Telephony.Sms.BODY));
        } else {
            mThreadId = data.getLong(data.getColumnIndexOrThrow("thread_id"));
            mDate = data.getLong(data.getColumnIndexOrThrow("date"));
            mAddress = data.getString(data.getColumnIndexOrThrow("address"));
            mBody = data.getString(data.getColumnIndexOrThrow("body"));
        }
        mIncoming = isIncomingMessage(data, true);
    }

    private void parseMmsMessage(Context context, Cursor data, String myNumber) {
        mId = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID));
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            mThreadId = data.getLong(data.getColumnIndexOrThrow(Telephony.Sms.Conversations.THREAD_ID));
            mDate = data.getLong(data.getColumnIndexOrThrow(Telephony.Sms.Conversations.DATE)) * SEC_TO_MILLI;
        } else {
            mThreadId = data.getLong(data.getColumnIndexOrThrow("thread_id"));
            mDate = data.getLong(data.getColumnIndexOrThrow("date")) * SEC_TO_MILLI;
        }
        mIncoming = isIncomingMessage(data, false);
        mMmsId = data.getLong(data.getColumnIndex(Telephony.Mms._ID));
        mMyNumber = myNumber;
    }

    private static boolean isIncomingMessage(Cursor cursor, boolean isSMS) {
        int boxId;
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            if (isSMS) {
                boxId = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                return (boxId == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX ||
                        boxId == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_ALL);
            }
            else {
                boxId = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX));
                return (boxId == Telephony.Mms.MESSAGE_BOX_INBOX ||
                        boxId == Telephony.Mms.MESSAGE_BOX_ALL);
            }
        } else {
            if (isSMS) {
                boxId = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                return (boxId == 0x00000001 || boxId == 0x00000000);
            }
            else {
                boxId = cursor.getInt(cursor.getColumnIndexOrThrow("msg_box"));
                return (boxId == 0x00000001 || boxId == 0x00000000);
            }
        }
    }

    public boolean isMms() {
        return mIsMms;
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

    public ArrayList<Bitmap> getAttachments() {
        return mAttachments;
    }

    public long getDate() {
        return mDate;
    }

    public long getThreadId() {
        return mThreadId;
    }

    public Uri getAttachment() {
        if (mIsMms && mAttachment == null) {
            // Query all the MMS parts associated with this message
            Cursor inner;
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                Uri messageUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, mId + "/part");
                inner = mContext.getContentResolver().query(
                        messageUri,
                        MMS_PROJECTION,
                        Telephony.Mms.Part.MSG_ID + " = ?",
                        new String[]{String.valueOf(mMmsId)},
                        null
                );
            } else {
                Uri messageUri = Uri.parse("content://" + mId + "/part");
                inner = mContext.getContentResolver().query(
                        messageUri,
                        MMS_PROJECTION_PRE_LOLLIPOP,
                        "mid" + " = ?",
                        new String[]{String.valueOf(mMmsId)},
                        null
                );
            }

            while (inner.moveToNext()) {
                String contentType;
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    contentType = inner.getString(inner.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE));
                } else {
                    contentType = inner.getString(inner.getColumnIndex("ct"));
                }
                if (contentType == null) {
                    continue;
                }

                if (contentType.matches("image/.*")) {
                    // Find any part that is an image attachment
                    long partId = inner.getLong(inner.getColumnIndex(BaseColumns._ID));
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        mAttachment = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part/" + partId);
                    } else {
                        mAttachment = Uri.parse("content://part/" + partId);
                    }
                } else if (contentType.matches("text/.*")) {
                    // Find any part that is text data
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        mBody = inner.getString(inner.getColumnIndex(Telephony.Mms.Part.TEXT));
                    } else {
                        mBody = inner.getString(inner.getColumnIndex("text"));
                    }
                }
            }
            inner.close();
        }
        return mAttachment;
    }

    public boolean getIncoming() {
        return mIncoming;
    }

    @Override
    public int compareTo(Object o) {
        float myDate = getDate();
        float theirDate = ((Text) o).getDate();
        if(myDate > theirDate) {
            return 1;
        }
        if(theirDate > myDate) {
            return -1;
        }
        return 0;
    }

    public static class Builder {
        private String mMessage;
        private String mRecipient;
        private ArrayList<Bitmap> mAttachments = new ArrayList<>();

        public Builder() {
        }

        public Builder recipient(String recipient) {
            mRecipient = recipient;
            return this;
        }

        public Builder attach(Bitmap attachment) {
            mAttachments.add(attachment);
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
            if(mAttachments.size()>0){
                text.mIsMms = true;
                text.mAttachments.addAll(mAttachments);
            }
            text.mDate = System.currentTimeMillis();
            return text;
        }
    }
}