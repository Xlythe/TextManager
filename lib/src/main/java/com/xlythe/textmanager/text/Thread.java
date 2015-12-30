package com.xlythe.textmanager.text;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageCallback;
import com.xlythe.textmanager.MessageThread;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;

/**
 * An SMS conversation
 */
public class Thread implements MessageThread<Text>, Serializable {
    @SuppressLint("NewApi")
    static final String[] MMS_PROJECTION = new String[] {
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
    private static final String TYPE_SMS = "sms";
    private static final String TYPE_MMS = "mms";
    private static final long MILLI_TO_SEC = 1000;



    private long mId;
    private long mThreadId;
    private long mDate;
    private String mAddress;
    private String mBody;
    private Uri mAttachment;



    protected Thread(Context context, Cursor cursor) {
        String type = getMessageType(cursor);
        if (TYPE_SMS.equals(type)){
            parseSmsMessage(cursor);
        }
        else if (TYPE_MMS.equals(type)){
            parseMmsMessage(context, cursor);
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
    }

    private void parseMmsMessage(Context context, Cursor data) {
        mId = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID));
        long _id = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID));
        Uri addressUri;
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            mThreadId = data.getLong(data.getColumnIndexOrThrow(Telephony.Sms.Conversations.THREAD_ID));
            mDate = data.getLong(data.getColumnIndexOrThrow(Telephony.Sms.Conversations.DATE)) * MILLI_TO_SEC;
            addressUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, _id + "/addr");
        } else {
            mThreadId = data.getLong(data.getColumnIndexOrThrow("thread_id"));
            mDate = data.getLong(data.getColumnIndexOrThrow("date")) * MILLI_TO_SEC;
            addressUri = Uri.parse("content://" + _id + "/addr");
        }

        // Query the address information for this message
        Cursor addr = context.getContentResolver().query(
                addressUri,
                null,
                null,
                null,
                null
        );
        HashSet<String> recipients = new HashSet<>();
        while (addr.moveToNext()) {
            String address;
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                address = addr.getString(addr.getColumnIndex(Telephony.Mms.Addr.ADDRESS));
            } else {
                address = addr.getString(addr.getColumnIndex("address"));
            }
            // Don't add our own number to the displayed list
//            if (myNumber == null || !address.contains(myNumber)) {
                recipients.add(address);
//            }
        }
        mAddress = TextUtils.join(",", recipients);
        addr.close();

        // Query all the MMS parts associated with this message
        Cursor inner;
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            Uri messageUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, _id + "/part");
            inner = context.getContentResolver().query(
                    messageUri,
                    MMS_PROJECTION,
                    Telephony.Mms.Part.MSG_ID + " = ?",
                    new String[]{String.valueOf((data.getLong(data.getColumnIndex(Telephony.Mms._ID))))},
                    null
            );
        } else {
            Uri messageUri = Uri.parse("content://" + _id + "/part");
            inner = context.getContentResolver().query(
                    messageUri,
                    MMS_PROJECTION_PRE_LOLLIPOP,
                    "mid" + " = ?",
                    new String[]{String.valueOf((data.getLong(data.getColumnIndex(Telephony.Mms._ID))))},
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
            else if (contentType.matches("image/.*")) {
                // Find any part that is an image attachment
                long partId = inner.getLong(inner.getColumnIndex(BaseColumns._ID));
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    mAttachment = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part/" + partId);
                } else {
                    mAttachment = Uri.parse("content://part/" + partId);
                }
            }
            else if (contentType.matches("text/.*")) {
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

    @Override
    public String getId(){
        return Long.toString(mThreadId);
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public int getUnreadCount() {
        return 0;
    }

    @Override
    public Text getLatestMessage() {
        return null;
    }
}