package com.xlythe.textmanager.text;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.annotation.VisibleForTesting;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.text.pdu.PduBody;
import com.xlythe.textmanager.text.pdu.PduComposer;
import com.xlythe.textmanager.text.pdu.PduPart;
import com.xlythe.textmanager.text.pdu.SendReq;
import com.xlythe.textmanager.text.smil.SmilHelper;
import com.xlythe.textmanager.text.smil.SmilXmlSerializer;
import com.xlythe.textmanager.text.util.CharacterSets;
import com.xlythe.textmanager.text.util.ContentType;
import com.xlythe.textmanager.text.util.EncodedStringValue;
import com.xlythe.textmanager.text.util.Utils;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import static com.xlythe.textmanager.text.TextManager.TAG;

/**
 * Either an sms or an mms
 */
public final class Text implements Message, Parcelable, Comparable<Text> {

    private static final String TYPE_SMS = "sms";
    private static final String TYPE_MMS = "mms";
    private static final long SEC_TO_MILLI = 1000;
    public static final long TYPE_SENDER = 137;

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
    private byte[] mBytesToSend;

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

    Text(TextCursor cursor) {
        String type = getMessageType(cursor);
        if (TYPE_SMS.equals(type)){
            mIsMms = false;
            parseSmsMessage(cursor);
        } else if (TYPE_MMS.equals(type)){
            mIsMms = true;
            parseMmsMessage(cursor, cursor.getMmsCursor());
        } else {
            Log.w("TelephonyProvider", "Unknown Message Type");
        }
    }

    Text(Cursor cursor, Cursor mmsCursor) {
        String type = getMessageType(cursor);
        if (TYPE_SMS.equals(type)){
            mIsMms = false;
            parseSmsMessage(cursor);
        } else if (TYPE_MMS.equals(type)){
            mIsMms = true;
            parseMmsMessage(cursor, mmsCursor);
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

        if (in.readInt() > 0) {
            in.readByteArray(mBytesToSend);
        }
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

    private void parseMmsMessage(Cursor data, Cursor mmsCursor) {
        mIncoming = isIncomingMessage(data, false);
        mId = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID));
        mThreadId = data.getLong(data.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.THREAD_ID));
        mDate = data.getLong(data.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.DATE)) * SEC_TO_MILLI;
        mMmsId = data.getLong(data.getColumnIndex(Mock.Telephony.Mms._ID));
        mStatus = data.getInt(data.getColumnIndexOrThrow(Mock.Telephony.Mms.STATUS));

        if (mmsCursor != null) {
            mmsCursor.moveToFirst();
            while (mmsCursor.moveToNext()) {
                if (mmsCursor.getLong(mmsCursor.getColumnIndex(Mock.Telephony.Mms.Part.MSG_ID)) == mMmsId) {
                    String contentType = mmsCursor.getString(mmsCursor.getColumnIndex(Mock.Telephony.Mms.Part.CONTENT_TYPE));
                    if (contentType == null) {
                        continue;
                    }

                    if (contentType.matches("image/.*")) {
                        // Find any part that is an image attachment
                        long partId = mmsCursor.getLong(mmsCursor.getColumnIndex(BaseColumns._ID));
                        mAttachment = new ImageAttachment(Uri.withAppendedPath(Mock.Telephony.Mms.CONTENT_URI, "part/" + partId));
                    } else if (contentType.matches("text/.*")) {
                        // Find any part that is text data
                        mBody = mmsCursor.getString(mmsCursor.getColumnIndex(Mock.Telephony.Mms.Part.TEXT));
                    } else if (contentType.matches("video/.*")) {
                        long partId = mmsCursor.getLong(mmsCursor.getColumnIndex(BaseColumns._ID));
                        mAttachment = new VideoAttachment(Uri.withAppendedPath(Mock.Telephony.Mms.CONTENT_URI, "part/" + partId));
                    }
                }
            }
        }
    }

    Set<String> getMemberAddresses() {
        return mMemberAddresses;
    }

    String getSenderAddress() {
        return mSenderAddress;
    }


    Set<Contact> getMembers() {
        return mMembers;
    }

    Contact getSender() {
        return mSender;
    }

    synchronized void setMemberAddress(String memberAddress) {
        mMemberAddresses.add(memberAddress);
    }

    synchronized void setSenderAddress(String senderAddress) {
        mSenderAddress = senderAddress;
    }

    synchronized void setSender(Contact sender) {
        mSender = sender;
    }

    synchronized void addMember(Contact member) {
        mMembers.add(member);
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

    public long getMmsId() {
        return mMmsId;
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
    public Status getStatus() {
        if (mStatus == Mock.Telephony.Sms.STATUS_COMPLETE)
                return Status.COMPLETE;
        if (mStatus == Mock.Telephony.Sms.STATUS_PENDING)
                return Status.PENDING;
        if (mStatus == Mock.Telephony.Sms.STATUS_FAILED)
                return Status.FAILED;
        return Status.NONE;
    }

    SendReq getSendRequest(Context context) {
        final PduBody pduBody = new PduBody();
        PduPart partPdu = new PduPart();
        long messageSize = 0;
        byte[] nameBytes = new byte[]{};
        byte[] typeBytes = new byte[]{};
        byte[] dataBytes = new byte[]{};

        // Create media parts, we only send one pic/video at a time
        if (mAttachment != null) {
            Attachment.Type type = mAttachment.getType();
            switch (type) {
                case IMAGE:
                    nameBytes = "image".getBytes();
                    typeBytes = ContentType.IMAGE_JPEG.getBytes();
                    dataBytes = ((ImageAttachment) mAttachment).getBytes(context);
                    if (dataBytes == null) {
                        Log.e(TAG, "Error getting bitmap from attachment");
                        break;
                    }
                    break;
                case HIGH_RES:
                    nameBytes = "image".getBytes();
                    typeBytes = ContentType.IMAGE_PNG.getBytes();
                    dataBytes = ((ImageAttachment) mAttachment).getBytes(context);
                    if (dataBytes == null) {
                        Log.e(TAG, "Error getting bitmap from attachment");
                        break;
                    }
                    break;
                case VIDEO:
                    nameBytes = "video".getBytes();
                    typeBytes = ContentType.VIDEO_MP4.getBytes();
                    dataBytes = ((VideoAttachment) mAttachment).getBytes(context).get();
                    if (dataBytes == null) {
                        Log.e(TAG, "Error getting bytes from attachment");
                        break;
                    }
                case VOICE:
                    //TODO: Voice support
                    break;
            }
            partPdu.setName(nameBytes);
            partPdu.setContentType(typeBytes);
            partPdu.setData(dataBytes);
            pduBody.addPart(partPdu);
            messageSize += (nameBytes.length + typeBytes.length + dataBytes.length);
        }

        // We can have an image with text, so we do this separate
        if (mBody != null && !mBody.isEmpty()) {
            // add text to the end of the part and send
            partPdu.setName("text".getBytes());
            partPdu.setContentType(ContentType.TEXT_PLAIN.getBytes());
            partPdu.setData(mBody.getBytes());
            partPdu.setCharset(CharacterSets.UTF_8);
            pduBody.addPart(partPdu);
            messageSize += ("text".getBytes().length + ContentType.TEXT_PLAIN.getBytes().length + mBody.getBytes().length);
        }

        // Create the actual send request that can later be turned into byte for sending
        final SendReq sendRequest = new SendReq();

        // Add recipients to the send request
        for (Contact recipient : TextManager.getInstance(context).getMembersExceptMe(this).get()) {
            final EncodedStringValue[] phoneNumbers = EncodedStringValue.extract(recipient.getNumber());
            if (phoneNumbers != null && phoneNumbers.length > 0) {
                sendRequest.addTo(phoneNumbers[0]);
            }
        }

        // This can't be empty, but we really should give the opportunity to set it
        String subject = " ";
        sendRequest.setSubject(new EncodedStringValue(subject));

        // Set date
        sendRequest.setDate(Calendar.getInstance().getTimeInMillis() / 1000L);

        // Set sender
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        sendRequest.setFrom(new EncodedStringValue(manager.getLine1Number()));

        // Turn all the parts into a smil document
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(SmilHelper.createSmilDocument(pduBody), out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        pduBody.addPart(0, smilPart);

        // Add content to the send request
        sendRequest.setBody(pduBody);
        sendRequest.setMessageSize(messageSize);

        // Populate byte for convenience
        final PduComposer composer = new PduComposer(context, sendRequest);
        mBytesToSend = composer.make();

        return sendRequest;
    }

    byte[] getByteData(Context context) {
        // create byte array which will actually be sent
        if (mBytesToSend != null) {
            return mBytesToSend;
        } else {
            final PduComposer composer = new PduComposer(context, getSendRequest(context));
            final byte[] bytesToSend;
            bytesToSend = composer.make();
            return bytesToSend;
        }
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

        if (mBytesToSend == null) {
            mBytesToSend = new byte[0];
        }
        out.writeInt(mBytesToSend.length);
        out.writeByteArray(mBytesToSend);
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
        private final Cursor mMMSCursor;

        public TextCursor(Cursor cursor, Cursor mmsCursor) {
            super(cursor);
            mMMSCursor = mmsCursor;
        }

        private Cursor getMmsCursor() {
            return mMMSCursor;
        }

        public Text getText() {
            return new Text(this);
        }

        @Override
        public void close() {
            super.close();
            mMMSCursor.close();
        }
    }

    public static class Converter {
        public Text toText(Context context, SmsMessage[] msgs){
            Text text = new Text();

            SmsMessage sms = msgs[0];

            text.mDate = checkDate(sms);
            text.mMemberAddresses.add(sms.getDisplayOriginatingAddress());
            text.mSenderAddress = sms.getDisplayOriginatingAddress();


            // Add the message body
            StringBuilder body = new StringBuilder();
            for (SmsMessage msg: msgs) {
                sms = msg;
                if(sms.getDisplayMessageBody() != null) {
                    body.append(sms.getDisplayMessageBody());
                }
            }
            text.mBody = replaceFormFeeds(body.toString());

            if(text.mSenderAddress != null) {
                text.mThreadId = Receive.getOrCreateThreadId(context, text.mSenderAddress);
            }

            return text;
        }

        private String replaceFormFeeds(String s) {
            return s == null ? "" : s.replace('\f', '\n');
        }

        private long checkDate(SmsMessage sms){
            Calendar buildDate = new GregorianCalendar(2011, 8, 18); // 18 Sep 2011
            Calendar nowDate = new GregorianCalendar();
            long now = System.currentTimeMillis();
            nowDate.setTimeInMillis(now);
            if(nowDate.before(buildDate)) {
                now = sms.getTimestampMillis();
            }
            return now;
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