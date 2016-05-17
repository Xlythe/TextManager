package com.xlythe.textmanager.text;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserManager;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.support.annotation.VisibleForTesting;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.User;
import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;
import com.xlythe.textmanager.text.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.xlythe.textmanager.text.TextManager.TAG;

/**
 * Either an sms or an mms
 */
public final class Text implements Message, Parcelable, Comparable<Text> {
    private static final String[] MMS_PROJECTION = new String[]{
            BaseColumns._ID,
            Mock.Telephony.Mms.Part.CONTENT_TYPE,
            Mock.Telephony.Mms.Part.TEXT,
            Mock.Telephony.Mms.Part._DATA
    };
    private static final String TYPE_SMS = "sms";
    private static final String TYPE_MMS = "mms";
    private static final long SEC_TO_MILLI = 1000;
    private static final long TYPE_SENDER = 137;

    static final Text EMPTY_TEXT = new Text();

    private long mId = -1;
    private long mThreadId;
    private long mDate;
    private long mMmsId;
    private int mStatus;
    private String mBody;
    private boolean mIncoming;
    private boolean mIsMms = false;
    private String mSenderAddress;
    private Contact mSender;
    private Set<String> mMemberAddresses = new HashSet<>();
    private Set<Contact> mMembers = new HashSet<>();
    private Attachment mAttachment;

    private Text() {}

    Text(long id,
            long threadId,
            long date,
            long mmsId,
            int status,
            String body,
            boolean incoming,
            boolean isMms,
            String senderAddress,
            Set<String> memberAddresses,
            Attachment attachment) {
        mId = id;
        mThreadId = threadId;
        mDate = date;
        mMmsId = mmsId;
        mStatus = status;
        mBody = body;
        mIncoming = incoming;
        mIsMms = isMms;
        mSenderAddress = senderAddress;
        mMemberAddresses = memberAddresses;
        mAttachment = attachment;
    }

    void setAttachment(Attachment attachment){
        mAttachment = attachment;
    }

    void setBody(String body){
        mBody = body;
    }

    Text(Context context, Cursor cursor) {
        String type = getMessageType(cursor);
        if (TYPE_SMS.equals(type)){
            mIsMms = false;
            parseSmsMessage(cursor);
        } else if (TYPE_MMS.equals(type)){
            mIsMms = true;
            parseMmsMessage(cursor, ((TextCursor) cursor).getMmsCursor(), context);
        } else {
            Log.w("TelephonyProvider", "Unknown Message Type");
        }
    }

    Text(Context context, Cursor cursor, Cursor cursor2) {
        String type = getMessageType(cursor);
        if (TYPE_SMS.equals(type)){
            mIsMms = false;
            parseSmsMessage(cursor);
        } else if (TYPE_MMS.equals(type)){
            mIsMms = true;
            parseMmsMessage(cursor, cursor2, context);
        } else {
            Log.w("TelephonyProvider", "Unknown Message Type");
        }
    }

    private Text(Parcel in) {
        mId = in.readLong();
        mThreadId = in.readLong();
        mDate = in.readLong();
        mMmsId = in.readLong();
        mStatus = in.readInt();
        mSenderAddress = in.readString();
        mBody = in.readString();
        mIncoming = in.readByte() != 0;
        mIsMms = in.readByte() != 0;
        mSender = in.readParcelable(Contact.class.getClassLoader());

        int membersSize = in.readInt();
        for (int i = 0; i < membersSize; i++) {
            mMemberAddresses.add(in.readString());
        }

        membersSize = in.readInt();
        for (int i = 0; i < membersSize; i++) {
            mMembers.add((Contact) in.readParcelable(Contact.class.getClassLoader()));
        }

        mAttachment = in.readParcelable(Attachment.class.getClassLoader());
    }

    private String getMessageType(Cursor cursor) {
        int typeIndex = cursor.getColumnIndex(Mock.Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN);
        if (typeIndex < 0) {
            // Type column not in projection, use another discriminator
            String cType = null;
            int cTypeIndex = cursor.getColumnIndex(Mock.Telephony.Mms.CONTENT_TYPE);
            if (cTypeIndex >= 0) {
                cType = cursor.getString(cursor.getColumnIndex(Mock.Telephony.Mms.CONTENT_TYPE));
            }
            // If content type is present, this is an MMS message
            if (cType != null) {
                return TYPE_MMS;
            } else {
                return TYPE_SMS;
            }
        } else {
            return cursor.getString(typeIndex);
        }
    }

    private void parseSmsMessage(Cursor data) {
        mIncoming = isIncomingMessage(data, true);
        mId = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID));
        mThreadId = data.getLong(data.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.THREAD_ID));
        mDate = data.getLong(data.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.DATE));
        mMemberAddresses.add(data.getString(data.getColumnIndexOrThrow(Mock.Telephony.Sms.ADDRESS)));
        mSenderAddress = data.getString(data.getColumnIndexOrThrow(Mock.Telephony.Sms.ADDRESS));
        mBody = data.getString(data.getColumnIndexOrThrow(Mock.Telephony.Sms.BODY));
        mStatus = data.getInt(data.getColumnIndexOrThrow(Mock.Telephony.Sms.STATUS));
    }

    private void parseMmsMessage(Cursor data, Cursor data2, Context context) {
        mIncoming = isIncomingMessage(data, false);
        mId = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID));
        mThreadId = data.getLong(data.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.THREAD_ID));
        mDate = data.getLong(data.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.DATE)) * SEC_TO_MILLI;
        mMmsId = data.getLong(data.getColumnIndex(Mock.Telephony.Mms._ID));
        mStatus = data.getInt(data.getColumnIndexOrThrow(Mock.Telephony.Mms.STATUS));

        if (data2 != null) {
            data2.moveToFirst();
            while (data2.moveToNext()) {
                if (data2.getLong(data2.getColumnIndex(Mock.Telephony.Mms.Part.MSG_ID)) == mMmsId) {
                    String contentType = data2.getString(data2.getColumnIndex(Mock.Telephony.Mms.Part.CONTENT_TYPE));
                    if (contentType == null) {
                        continue;
                    }

                    if (contentType.matches("image/.*")) {
                        // Find any part that is an image attachment
                        long partId = data2.getLong(data2.getColumnIndex(BaseColumns._ID));
                        mAttachment = new ImageAttachment(Uri.withAppendedPath(Mock.Telephony.Mms.CONTENT_URI, "part/" + partId));
                    } else if (contentType.matches("text/.*")) {
                        // Find any part that is text data
                        mBody = data2.getString(data2.getColumnIndex(Mock.Telephony.Mms.Part.TEXT));
                    } else if (contentType.matches("video/.*")) {
                        long partId = data2.getLong(data2.getColumnIndex(BaseColumns._ID));
                        mAttachment = new VideoAttachment(Uri.withAppendedPath(Mock.Telephony.Mms.CONTENT_URI, "part/" + partId));
                    }
                }
            }
        }
    }

    static boolean isIncomingMessage(Cursor cursor, boolean isSMS) {
        int boxId;
        if (isSMS) {
            boxId = cursor.getInt(cursor.getColumnIndexOrThrow(Mock.Telephony.Sms.TYPE));
            return (boxId == Mock.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX ||
                    boxId == Mock.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_ALL);
        }
        else {
            boxId = cursor.getInt(cursor.getColumnIndexOrThrow(Mock.Telephony.Mms.MESSAGE_BOX));
            return (boxId == Mock.Telephony.Mms.MESSAGE_BOX_INBOX ||
                    boxId == Mock.Telephony.Mms.MESSAGE_BOX_ALL);
        }
    }

    public boolean isMms() {
        return mIsMms;
    }

    public boolean isIncoming(){
        return mIncoming;
    }

    @Override
    public String getId() {
        return Long.toString(mId);
    }

    public long getIdAsLong() {
        return mId;
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

    public long getThreadIdAsLong() {
        return mThreadId;
    }

    @Override
    public Attachment getAttachment() {
        return mAttachment;
    }

    @Override
    public Contact getSender() {
        return mSender;
    }

    private void buildSender(Context context) {
        Uri addressUri = Uri.withAppendedPath(Mock.Telephony.Mms.CONTENT_URI, mId + "/addr");

        // Query the address information for this message
        Cursor addr = context.getContentResolver().query(addressUri, null, null, null, null);

        while (addr.moveToNext()) {
            if (addr.getLong(addr.getColumnIndex(Mock.Telephony.Mms.Addr.MSG_ID)) == mMmsId) {
                if (addr.getLong(addr.getColumnIndex(Mock.Telephony.Mms.Addr.TYPE)) == TYPE_SENDER) {
                    setSenderAddress(addr.getString(addr.getColumnIndex(Mock.Telephony.Mms.Addr.ADDRESS)));
                }
                setMemberAddress(addr.getString(addr.getColumnIndex(Mock.Telephony.Mms.Addr.ADDRESS)));
            }
        }
        addr.close();
    }

    private synchronized void setMemberAddress(String memberAddress) {
        mMemberAddresses.add(memberAddress);
    }

    private synchronized void setSenderAddress(String senderAddress) {
        mSenderAddress = senderAddress;
    }

    private synchronized void setSender(Contact sender) {
        mSender = sender;
    }

    public synchronized Future<Contact> getSender(final Context context) {
        if (mSender != null) {
            return new Present<>(mSender);
        } else {
            return new FutureImpl<Contact>() {
                @Override
                public Contact get() {
                    TextManager manager = TextManager.getInstance(context);
                    if (mSenderAddress == null && mIsMms) {
                        buildSender(context);
                    }
                    Contact sender = isIncoming() ? manager.lookupContact(mSenderAddress) : manager.getSelf();
                    setSender(sender);
                    return sender;
                }
            };
        }
    }

    @Override
    public Set<Contact> getMembers() {
        return mMembers;
    }

    private synchronized void addMember(Contact member) {
        mMembers.add(member);
    }

    public synchronized Future<Set<Contact>> getMembers(final Context context) {
        if (!mMembers.isEmpty()) {
            return new Present<>(mMembers);
        } else {
            return new FutureImpl<Set<Contact>>() {
                @Override
                public Set<Contact> get() {
                    TextManager manager = TextManager.getInstance(context);
                    if (mMemberAddresses.isEmpty() && mIsMms) {
                        buildSender(context);
                    }
                    for (String address : mMemberAddresses) {
                        addMember(manager.lookupContact(address));
                    }
                    return mMembers;
                }
            };
        }
    }

    public synchronized Future<Set<Contact>> getMembersExceptMe(final Context context) {
        return new FutureImpl<Set<Contact>>() {
            @Override
            public Set<Contact> get() {
                Set<Contact> members = new HashSet<>(getMembers(context).get());
                if (members.size() == 1) {
                    // It's possible to text yourself. To account for that, don't remove yourself if there's only one member.
                    return members;
                }
                members.remove(TextManager.getInstance(context).getSelf());
                return members;
            }
        };
    }

    @Override
    public Status getStatus() {
        if (mStatus == Mock.Telephony.Sms.STATUS_COMPLETE)
                return Status.COMPLETE;
        if (mStatus == Mock.Telephony.Sms.STATUS_PENDING)
                return Status.PENDING;
        if (mStatus == Mock.Telephony.Sms.STATUS_FAILED)
                return Status.FAILED;
        return Status.NONE;
    }

    public byte[] toBytes() {
        Parcel parcel = Parcel.obtain();
        writeToParcel(parcel, describeContents());
        try {
            return parcel.marshall();
        } finally {
            parcel.recycle();
        }
    }

    public static Text fromBytes(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        try {
            return CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Text) {
            Text a = (Text) o;
            return Utils.equals(mId, a.mId)
                    && Utils.equals(mThreadId, a.mThreadId)
                    && Utils.equals(mDate, a.mDate)
                    && Utils.equals(mMmsId, a.mMmsId)
                    && Utils.equals(mSenderAddress, a.mSenderAddress)
                    && Utils.equals(mBody, a.mBody)
                    && Utils.equals(mIncoming, a.mIncoming)
                    && Utils.equals(mIsMms, a.mIsMms)
                    && Utils.equals(mSender, a.mSender)
                    && Utils.equals(mMembers, a.mMembers)
                    && Utils.equals(mAttachment, a.mAttachment);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(mId)
                + Utils.hashCode(mThreadId)
                + Utils.hashCode(mDate)
                + Utils.hashCode(mMmsId)
                + Utils.hashCode(mSenderAddress)
                + Utils.hashCode(mBody)
                + Utils.hashCode(mIncoming)
                + Utils.hashCode(mIsMms)
                + Utils.hashCode(mSender)
                + Utils.hashCode(mMembers)
                + Utils.hashCode(mAttachment);
    }

    @Override
    public String toString() {
        return String.format("Text{id=%s, thread_id=%s, date=%s, mms_id=%s, address=%s, body=%s, " +
                        "incoming=%s, is_mms=%s, sender=%s, recipient=%s, attachment=%s}",
                mId, mThreadId, mDate, mMmsId, mSenderAddress, mBody, mIncoming, mIsMms,
                mSender, mMembers, mAttachment);
    }

    @Override
    public int compareTo(Text text) {
        if (text.getTimestamp() > getTimestamp()) {
            return -1;
        }
        if(text.getTimestamp() < getTimestamp()) {
            return 1;
        }
        return 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mId);
        out.writeLong(mThreadId);
        out.writeLong(mDate);
        out.writeLong(mMmsId);
        out.writeInt(mStatus);
        out.writeString(mSenderAddress);
        out.writeString(mBody);
        out.writeByte((byte) (mIncoming ? 1 : 0));
        out.writeByte((byte) (mIsMms ? 1 : 0));
        out.writeParcelable(mSender, flags);

        out.writeInt(mMemberAddresses.size());
        for (String member : mMemberAddresses){
            out.writeString(member);
        }

        out.writeInt(mMembers.size());
        for (Contact member : mMembers){
            out.writeParcelable(member, flags);
        }

        out.writeParcelable(mAttachment, flags);
    }

    public static final Parcelable.Creator<Text> CREATOR = new Parcelable.Creator<Text>() {
        public Text createFromParcel(Parcel in) {
            return new Text(in);
        }

        public Text[] newArray(int size) {
            return new Text[size];
        }
    };

    public static class TextCursor extends CursorWrapper {
        private final Context mContext;
        private final android.database.Cursor mCursor2;

        public TextCursor(Context context, android.database.Cursor cursor, android.database.Cursor cursor2) {
            super(cursor);
            mContext = context.getApplicationContext();
            mCursor2 = cursor2;
        }

        private Cursor getMmsCursor() {
            return mCursor2;
        }

        public Text getText() {
            return new Text(mContext, this);
        }
    }

    public static class Builder {
        private String mMessage;
        private HashSet<Contact> mRecipients = new HashSet<>();
        private Attachment mAttachment;

        public Builder addRecipient(Context context, String address) {
            mRecipients.add(TextManager.getInstance(context).lookupContact(address));
            return this;
        }

        public Builder addRecipient(Contact address) {
            mRecipients.add(address);
            return this;
        }

        public Builder addRecipients(Context context, String... addresses) {
            TextManager textManager = TextManager.getInstance(context);
            for (String address : addresses) {
                mRecipients.add(textManager.lookupContact(address));
            }
            return this;
        }

        public Builder addRecipients(Collection<Contact> addresses) {
            mRecipients.addAll(addresses);
            return this;
        }

        public Builder addRecipients(Contact... addresses) {
            Collections.addAll(mRecipients, addresses);
            return this;
        }

        public Builder attach(Attachment attachment) {
            mAttachment = attachment;
            return this;
        }

        public Builder message(String message) {
            mMessage = message;
            return this;
        }

        public Text build() {
            Text text = new Text();
            text.mBody = mMessage;
            text.mMembers.addAll(mRecipients);
            if (mRecipients.size() > 1) {
                text.mIsMms = true;
            }
            text.mAttachment = mAttachment;
            if (mAttachment != null) {
                text.mIsMms = true;
            }
            text.mDate = System.currentTimeMillis();
            return text;
        }
    }

    @VisibleForTesting
    public static class DebugBuilder extends Builder {
        private Contact mSender;
        private long mThreadId;

        public DebugBuilder setSender(Contact contact) {
            mSender = contact;
            addRecipient(contact);
            return this;
        }

        public DebugBuilder setSender(String contact) {
            mSender = new Contact(contact);
            addRecipient(mSender);
            return this;
        }

        public DebugBuilder addRecipient(String contact) {
            addRecipient(new Contact(contact));
            return this;
        }

        public DebugBuilder setThreadId(long threadId) {
            mThreadId = threadId;
            return this;
        }

        @Override
        public DebugBuilder addRecipient(Context context, String address) {
            super.addRecipient(context, address);
            return this;
        }

        @Override
        public DebugBuilder addRecipient(Contact address) {
            super.addRecipient(address);
            return this;
        }

        public DebugBuilder addRecipients(Context context, String... addresses) {
            super.addRecipients(context, addresses);
            return this;
        }

        public DebugBuilder addRecipients(Collection<Contact> addresses) {
            super.addRecipients(addresses);
            return this;
        }

        public DebugBuilder addRecipients(Contact... addresses) {
            super.addRecipients(addresses);
            return this;
        }

        public DebugBuilder attach(Attachment attachment) {
            super.attach(attachment);
            return this;
        }

        public DebugBuilder message(String message) {
            super.message(message);
            return this;
        }

        public Text build() {
            Text text = super.build();
            if (mSender != null) {
                text.mSender = mSender;
            }
            text.mThreadId = mThreadId;
            return text;
        }
    }
}