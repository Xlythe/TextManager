package com.xlythe.textmanager.text;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.text.pdu.PduBody;
import com.xlythe.textmanager.text.mms.MmsPduStore;
import com.xlythe.textmanager.text.pdu.PduHeaders;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
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
    private final Set<Contact> mMembers = new HashSet<>();
    private final List<Attachment> mAttachments = new ArrayList<>();
    private byte[] mBytesToSend;
    private int mSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

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
        mMemberAddresses = new HashSet<>(memberAddresses);
        if (attachment != null) mAttachments.add(attachment);
    }

    void readPart(Cursor part) {
        String type = part.getString(part.getColumnIndexOrThrow(Mock.Telephony.Mms.Part.CONTENT_TYPE));
        if ("text/plain".equalsIgnoreCase(type) || "text/html".equalsIgnoreCase(type)) {
            String value = part.getString(part.getColumnIndexOrThrow(Mock.Telephony.Mms.Part.TEXT));
            if (value != null) mBody = mBody == null ? value : mBody + "\n" + value;
            return;
        }
        long id = part.getLong(part.getColumnIndexOrThrow(BaseColumns._ID));
        Attachment attachment = Attachment.fromPart(type, Uri.parse("content://mms/part/" + id));
        if (attachment != null) mAttachments.add(attachment);
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

        Attachment first = in.readParcelable(Attachment.class.getClassLoader());
        if (first != null) mAttachments.add(first);

        int expectedByteCount = in.readInt();
        if (expectedByteCount == -1) {
            int additional = in.readInt();
            if (additional < 1 || additional > in.dataAvail() / 4) throw new BadParcelableException("Invalid attachment count");
            for (int i = 0; i < additional; i++) mAttachments.add(in.readParcelable(Attachment.class.getClassLoader()));
            expectedByteCount = in.readInt();
        }
        mBytesToSend = in.createByteArray();
        if (mBytesToSend == null || mBytesToSend.length != expectedByteCount) {
            throw new BadParcelableException("Invalid prepared message payload");
        }
        if (in.dataAvail() >= Integer.BYTES) {
            mSubscriptionId = in.readInt();
        }
    }

    @SuppressLint("Range")
    private String getMessageType(Cursor cursor) {
        int typeIndex = cursor.getColumnIndex(Mock.Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN);
        if (typeIndex < 0) {
            // Type column not in projection, use another discriminator
            String contentType = null;
            int contentTypeIndex = cursor.getColumnIndex(Mock.Telephony.Mms.CONTENT_TYPE);
            if (contentTypeIndex >= 0) {
                contentType = cursor.getString(cursor.getColumnIndex(Mock.Telephony.Mms.CONTENT_TYPE));
            }
            // If content type is present, this is an MMS message
            if (contentType != null) {
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
        parseSubscriptionId(data);
    }

    private void parseSubscriptionId(Cursor data) {
        int subscriptionIndex = data.getColumnIndex(Mock.Telephony.Sms.SUBSCRIPTION_ID);
        if (subscriptionIndex >= 0 && !data.isNull(subscriptionIndex)) {
            mSubscriptionId = data.getInt(subscriptionIndex);
        }
    }

    @SuppressLint("Range")
    private void parseMmsMessage(Cursor data, Cursor mmsCursor) {
        mIncoming = isIncomingMessage(data, false);
        mId = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID));
        mThreadId = data.getLong(data.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.THREAD_ID));
        mDate = data.getLong(data.getColumnIndexOrThrow(Mock.Telephony.Sms.Conversations.DATE)) * SEC_TO_MILLI;
        mMmsId = data.getLong(data.getColumnIndex(Mock.Telephony.Mms._ID));
        mStatus = data.getInt(data.getColumnIndexOrThrow(Mock.Telephony.Mms.STATUS));
        parseSubscriptionId(data);

        if (mmsCursor != null && mmsCursor.moveToFirst()) {
            do {
                if (mmsCursor.getLong(mmsCursor.getColumnIndex(Mock.Telephony.Mms.Part.MSG_ID)) == mMmsId) {
                    readPart(mmsCursor);
                }
            } while (mmsCursor.moveToNext());
        }
    }

    synchronized Set<String> getMemberAddresses() {
        return new HashSet<>(mMemberAddresses);
    }

    String getSenderAddress() {
        return mSenderAddress;
    }


    synchronized Set<Contact> getMembers() {
        return new HashSet<>(mMembers);
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

    /** Returns the explicit SIM subscription for this SMS, or INVALID_SUBSCRIPTION_ID. */
    public int getSubscriptionId() {
        return mSubscriptionId;
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
        return mAttachments.isEmpty() ? null : mAttachments.get(mAttachments.size() - 1);
    }

    /** Every media or file part, in provider or composition order. */
    public List<Attachment> getAttachments() {
        return Collections.unmodifiableList(mAttachments);
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
        return getSendRequest(context, null, false, false);
    }

    SendReq getSendRequest(Context context, String subject, boolean deliveryReport, boolean readReport) {
        final PduBody pduBody = new PduBody();

        long messageSize = 0;
        Charset utf8 = Charset.forName("UTF-8");
        for (Attachment attachment : mAttachments) {
            PduPart part = new PduPart();
            byte[] data;
            if (attachment instanceof ImageAttachment) {
                data = ((ImageAttachment) attachment).getBytes(context);
            } else {
                try (InputStream input = attachment.asStream(context)) {
                    data = MmsPduStore.readBounded(input, (int) (MmsPduStore.MAX_PDU_BYTES - messageSize));
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to read MMS attachment", e);
                }
            }
            if (data == null || data.length == 0) throw new IllegalArgumentException("Empty MMS attachment");
            byte[] mime = attachment.getMimeType(context).getBytes(utf8);
            byte[] name = ("part" + pduBody.getPartsNum()).getBytes(utf8);
            part.setName(name);
            part.setContentLocation(name);
            part.setContentType(mime);
            part.setData(data);
            pduBody.addPart(part);
            messageSize += (long) name.length + mime.length + data.length;
            if (messageSize > MmsPduStore.MAX_PDU_BYTES) throw new IllegalArgumentException("Oversized MMS attachments");
        }

        if (!TextUtils.isEmpty(mBody)) {
            PduPart part = new PduPart();
            byte[] data = mBody.getBytes(utf8);
            part.setName("text".getBytes(utf8));
            part.setContentType(ContentType.TEXT_PLAIN.getBytes(utf8));
            part.setData(data);
            part.setCharset(CharacterSets.UTF_8);
            pduBody.addPart(part);
            messageSize += part.getName().length + part.getContentType().length + data.length;
        }

        // Create the actual send request that can later be turned into byte for sending
        final SendReq sendRequest = new SendReq();

        // Add recipients to the send request
        TextManager manager = TextManager.getInstance(context);
        String region = manager.dialingRegion(mSubscriptionId);
        Set<String> addresses = new HashSet<>();
        for (Contact recipient : manager.getMembersExceptMe(this).get()) {
            String address = recipient.getMobileNumber(context).get();
            if (TextUtils.isEmpty(address)) throw new IllegalArgumentException("MMS recipient has no address");
            if (addresses.add(PhoneNumbers.normalize(address, region))) sendRequest.addTo(new EncodedStringValue(address));
        }
        if (addresses.isEmpty()) throw new IllegalArgumentException("MMS has no recipients");

        if (!TextUtils.isEmpty(subject)) sendRequest.setSubject(new EncodedStringValue(subject));
        if (deliveryReport) sendRequest.setDeliveryReport(PduHeaders.VALUE_YES);
        if (readReport) sendRequest.setReadReport(PduHeaders.VALUE_YES);

        // Set date
        sendRequest.setDate(Calendar.getInstance().getTimeInMillis() / 1000L);

        // Set sender
        String phoneNumber = manager.getPhoneNumber(mSubscriptionId);
        // SendReq defaults to the carrier's insert-address token on numberless SIMs.
        if (!TextUtils.isEmpty(phoneNumber)) {
            sendRequest.setFrom(new EncodedStringValue(phoneNumber));
        }

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
    byte[] getByteData() {
        return mBytesToSend;
    }

    public byte[] toBytes() {
        Parcel parcel = Parcel.obtain();
        writeToParcel(parcel, 0);
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
    public boolean equals(@Nullable Object o) {
        if (o instanceof Text) {
            Text a = (Text) o;
            return Utils.equals(mId, a.mId)
                    && Utils.equals(mThreadId, a.mThreadId)
                    && Utils.equals(mDate, a.mDate)
                    && Utils.equals(mMmsId, a.mMmsId)
                    && Utils.equals(mSubscriptionId, a.mSubscriptionId)
                    && Utils.equals(mSenderAddress, a.mSenderAddress)
                    && Utils.equals(mBody, a.mBody)
                    && Utils.equals(mIncoming, a.mIncoming)
                    && Utils.equals(mIsMms, a.mIsMms)
                    && Utils.equals(mSender, a.mSender)
                    && Utils.equals(mMembers, a.mMembers)
                    && mAttachments.equals(a.mAttachments);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(mId)
                + Utils.hashCode(mThreadId)
                + Utils.hashCode(mDate)
                + Utils.hashCode(mMmsId)
                + Utils.hashCode(mSubscriptionId)
                + Utils.hashCode(mSenderAddress)
                + Utils.hashCode(mBody)
                + Utils.hashCode(mIncoming)
                + Utils.hashCode(mIsMms)
                + Utils.hashCode(mSender)
                + Utils.hashCode(mMembers)
                + mAttachments.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Text{id=%s, thread_id=%s, date=%s, mms_id=%s, address=%s, body=%s, " +
                        "incoming=%s, is_mms=%s, subscription_id=%s, sender=%s, recipient=%s, attachment=%s}",
                mId, mThreadId, mDate, mMmsId, mSenderAddress, mBody, mIncoming, mIsMms,
                mSubscriptionId, mSender, mMembers, mAttachments);
    }

    @Override
    public int compareTo(Text text) {
        return Long.compare(getTimestamp(), text.getTimestamp());
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

        out.writeParcelable(mAttachments.isEmpty() ? null : mAttachments.get(0), flags);
        if (mAttachments.size() > 1) {
            out.writeInt(-1);
            out.writeInt(mAttachments.size() - 1);
            for (int i = 1; i < mAttachments.size(); i++) out.writeParcelable(mAttachments.get(i), flags);
        }

        byte[] bytesToSend = mBytesToSend != null ? mBytesToSend : new byte[0];
        out.writeInt(bytesToSend.length);
        out.writeByteArray(bytesToSend);
        out.writeInt(mSubscriptionId);
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
            try {
                super.close();
            } finally {
                if (mMMSCursor != null) {
                    mMMSCursor.close();
                }
            }
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
        private final Set<Contact> mRecipients = new HashSet<>();
        private final List<Attachment> mAttachments = new ArrayList<>();
        private int mSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

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
            mAttachments.clear();
            if (attachment != null) addAttachment(attachment);
            return this;
        }

        public Builder addAttachment(Attachment attachment) {
            if (attachment == null) throw new IllegalArgumentException("Missing attachment");
            mAttachments.add(attachment);
            return this;
        }

        public Builder message(String message) {
            mMessage = message;
            return this;
        }

        /** Routes an outgoing SMS through a specific active SIM subscription. */
        public Builder subscriptionId(int subscriptionId) {
            if (subscriptionId < 0
                    && subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                throw new IllegalArgumentException("subscriptionId must be valid or INVALID_SUBSCRIPTION_ID");
            }
            mSubscriptionId = subscriptionId;
            return this;
        }

        public Text build() {
            Text text = new Text();
            text.mBody = mMessage;
            text.mSubscriptionId = mSubscriptionId;
            text.mMembers.addAll(mRecipients);
            if (mRecipients.size() > 1) {
                text.mIsMms = true;
            }
            for (Contact recipient : mRecipients) {
                if (recipient.getNumber() != null && recipient.getNumber().contains("@")) text.mIsMms = true;
            }
            text.mAttachments.addAll(mAttachments);
            if (!mAttachments.isEmpty()) {
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

        @Override
        public DebugBuilder subscriptionId(int subscriptionId) {
            super.subscriptionId(subscriptionId);
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
