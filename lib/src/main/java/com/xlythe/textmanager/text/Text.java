package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.util.Log;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.User;

import java.util.ArrayList;

/**
 * Either an sms or an mms
 */
public class Text implements Message, Parcelable {
    private static final String[] MMS_PROJECTION = new String[]{
            BaseColumns._ID,
            Mock.Telephony.Mms.Part.CONTENT_TYPE,
            Mock.Telephony.Mms.Part.TEXT,
            Mock.Telephony.Mms.Part._DATA
    };
    private static final String TYPE_SMS = "sms";
    private static final String TYPE_MMS = "mms";
    private static final long SEC_TO_MILLI = 1000;

    private long mId;
    private long mThreadId;
    private long mDate;
    private long mMmsId;
    private String mAddress;
    private String mBody;
    private boolean mIncoming;
    private boolean mIsMms = false;
    private Uri mAttachment;
    private Contact mSender;
    private ArrayList<Attachment> mAttachments = new ArrayList<>();

    private Text() {}

    protected Text(Context context, Cursor cursor) {
        String type = getMessageType(cursor);
        if (TYPE_SMS.equals(type)){
            mIsMms = false;
            parseSmsMessage(cursor);
        }
        else if (TYPE_MMS.equals(type)){
            mIsMms = true;
            parseMmsMessage(cursor, context);
        }
        else {
            Log.w("TelephonyProvider", "Unknown Message Type");
        }
        buildContact(context);
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

    private void parseMmsMessage(Cursor data, Context context) {
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

        if (mIsMms && mAttachment == null) {
            // Query all the MMS parts associated with this message
            Cursor inner;
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                Uri messageUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, mId + "/part");
                inner = context.getContentResolver().query(
                        messageUri,
                        MMS_PROJECTION,
                        Telephony.Mms.Part.MSG_ID + " = ?",
                        new String[]{String.valueOf(mMmsId)},
                        null
                );
            } else {
                Uri messageUri = Uri.parse("content://" + mId + "/part");
                inner = context.getContentResolver().query(
                        messageUri,
                        MMS_PROJECTION,
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

    private void buildContact(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri;

        if (android.os.Build.VERSION.SDK_INT >= 5) {
            uri = Uri.parse("content://com.android.contacts/phone_lookup");
        }
        else {
            uri = Uri.parse("content://contacts/phones/filter");
        }

        uri = Uri.withAppendedPath(uri, Uri.encode(mAddress));
        Cursor c = contentResolver.query(uri, null, null, null, null);
        if(c != null && c.moveToFirst()) {
            mSender = new Contact(c, mAddress);
            c.close();
        } else {
            mSender = new Contact(mAddress);
        }
    }

    protected boolean isMms() {
        return mIsMms;
    }

    @Override
    public String getId() {
        return Long.toString(mId);
    }

    @Override
    public String getBody() {
        return mBody;
    }

    @Override
    public long getTimestamp() {
        return mDate;
    }

    @Override
    public String getThreadId() {
        return Long.toString(mThreadId);
    }

    @Override
    public ArrayList<Attachment> getAttachments() {
        return mAttachments;
    }

    @Override
    public Contact getSender() {
        return mSender;
    }

    @Override
    public Contact getRecipient() {
        //TODO: getRecipient()
        return null;
    }

    @Override
    public Status getStatus() {
        //TODO: getStatus()
        return null;
    }

    private Text(Parcel in) {
        mId = in.readLong();
        mThreadId = in.readLong();
        mDate = in.readLong();
        mMmsId = in.readLong();
        mAddress = in.readString();
        mBody = in.readString();
        mIncoming = in.readByte() != 0;
        mIsMms = in.readByte() != 0;
        mAttachment = Uri.parse(in.readString());
        int size = in.readInt();
        for(int i=0; i<size; i++){
            //Type.values()[in.readInt()];
            mAttachments.add((Attachment)in.readParcelable(Attachment.class.getClassLoader()));
        }
        //in.readTypedList(mAttachments, Attachment.CREATOR);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mId);
        out.writeLong(mThreadId);
        out.writeLong(mDate);
        out.writeLong(mMmsId);
        out.writeString(mAddress);
        out.writeString(mBody);
        out.writeByte((byte) (mIncoming ? 1 : 0));
        out.writeByte((byte) (mIsMms ? 1 : 0));
        out.writeString(mAttachment.toString());

        // Test
        out.writeInt(mAttachments.size());
        for(int i=0; i<mAttachments.size(); i++){
            //out.writeValue(mAttachments.get(0).getType().ordinal());
            out.writeParcelable(mAttachments.get(0), flags);
        }
        //out.writeTypedList(mAttachments);
    }

    public static final Parcelable.Creator<Text> CREATOR = new Parcelable.Creator<Text>() {
        public Text createFromParcel(Parcel in) {
            return new Text(in);
        }

        public Text[] newArray(int size) {
            return new Text[size];
        }
    };

    public static class Builder {
        private String mMessage;
        private String mRecipient;
        private ArrayList<Attachment> mAttachments = new ArrayList<>();

        public Builder() {
        }

        public Builder recipient(String recipient) {
            mRecipient = recipient;
            return this;
        }

        public Builder attach(Attachment attachment) {
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