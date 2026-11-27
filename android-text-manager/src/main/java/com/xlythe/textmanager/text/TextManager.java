package com.xlythe.textmanager.text;

import java.util.Iterator;
import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.provider.BlockedNumberContract;
import android.provider.ContactsContract;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;
import com.xlythe.textmanager.text.pdu.PduHeaders;
import com.xlythe.textmanager.text.pdu.PduComposer;
import com.xlythe.textmanager.text.pdu.SendReq;
import com.xlythe.textmanager.text.mms.MmsPduStore;
import com.xlythe.textmanager.text.util.EncodedStringValue;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.util.PreKitKatUtils;
import com.xlythe.textmanager.text.Mock.Telephony;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.WorkerThread;

/**
 * Manages sms and mms messages
 */
@SuppressLint("Range")
public class TextManager implements MessageManager<Text, Thread, Contact> {
    static final String TAG = TextManager.class.getSimpleName();
    static final boolean DEBUG = false;
    private static final int COLUMN_CONTENT_LOCATION = 0;
    private static final String UNKNOWN = "Unknown";
    private static final String TEXT_EXTRA = "text";
    private static final String[] PROJECTION = messageProjection();

    private static String[] messageProjection() {
        List<String> columns = new ArrayList<>(Arrays.asList(
            // Determine if message is SMS or MMS
            Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN,
            // Base item ID
            BaseColumns._ID,
            // Conversation (thread) ID
            Telephony.Sms.Conversations.THREAD_ID,
            // Date values
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            // For SMS only
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            Telephony.Sms.SUBSCRIPTION_ID,
            // For MMS only
            Telephony.Mms.SUBJECT,
            Telephony.Mms.MESSAGE_BOX,
            Telephony.Mms.STATUS
        ));
        if (Build.VERSION.SDK_INT < 22) columns.remove(Telephony.Sms.SUBSCRIPTION_ID);
        if (Build.VERSION.SDK_INT < 16) columns.remove(Telephony.Sms.DATE_SENT);
        return columns.toArray(new String[0]);
    }

    @SuppressLint("StaticFieldLeak")
    private static TextManager sTextManager;

    public static synchronized TextManager getInstance(Context context) {
        Context app = context.getApplicationContext();
        if (sTextManager == null || sTextManager.mContext != app) {
            sTextManager = new TextManager(app);
        }
        return sTextManager;
    }

    private final Context mContext;
    private final MessageObserverRegistry mObservers = new MessageObserverRegistry();

    private TextManager(Context context) {
        mContext = context;
    }

    public static final class MmsDetails {
        public final String subject;
        public final String messageId;
        public final boolean readReportRequested;

        private MmsDetails(String subject, String messageId, boolean readReportRequested) {
            this.subject = subject;
            this.messageId = messageId;
            this.readReportRequested = readReportRequested;
        }
    }

    /** Carrier headers for a stored message, including the ID delivered in MMS report callbacks. */
    @Nullable
    public MmsDetails getMmsDetails(Text message) {
        if (!message.isMms() || message.getIdAsLong() < 0
                || !TelephonyPermissions.has(mContext, Manifest.permission.READ_SMS)) return null;
        Uri uri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, message.getIdAsLong());
        String[] columns = {Telephony.Mms.SUBJECT, Telephony.Mms.SUBJECT_CHARSET, Telephony.Mms.MESSAGE_ID, Telephony.Mms.READ_REPORT};
        try (Cursor c = mContext.getContentResolver().query(uri, columns, null, null, null)) {
            if (c == null || !c.moveToFirst()) return null;
            String subject = c.getString(0);
            if (subject != null) subject = new EncodedStringValue(c.getInt(1), PduPersister.getBytes(subject)).getString();
            return new MmsDetails(subject, c.getString(2), c.getInt(3) == PduHeaders.VALUE_YES);
        } catch (SecurityException denied) {
            return null;
        }
    }

    /** Call once, with the user's consent, when an incoming MMS is read or deleted unread. */
    @WorkerThread
    public void sendMmsReadReport(Text message, boolean deletedUnread) {
        MmsReceiveService.scheduleReadReport(mContext, message, deletedUnread);
    }

    public void downloadAttachment(Text text) {
        if (text != null && text.isMms()) {
            MmsReceiveService.schedule(mContext, text);
        }
    }

    private boolean observingConversations;

    public synchronized void registerObserver(MessageObserver observer) {
        if (!observingConversations && TelephonyPermissions.has(mContext, Manifest.permission.READ_SMS)) {
            mContext.getContentResolver().registerContentObserver(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI,
                    true, new TextObserver(new Handler(Looper.getMainLooper())));
            observingConversations = true;
        }
        mObservers.register(observer);
    }

    public void unregisterObserver(MessageObserver observer) {
        mObservers.unregister(observer);
    }

    private class TextObserver extends ContentObserver {
        TextObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mObservers.notifyDataChanged();
        }
    }

    public void send(Text text) {
        SendService.schedule(mContext, text);
    }

    public class Builder {
        private final Context mContext;
        @Nullable private final String mMessage;
        @Nullable private final Attachment mAttachment;
        private final List<Attachment> additionalAttachments = new ArrayList<>();
        private String mSubject;
        private boolean mMmsDeliveryReport;
        private boolean mMmsReadReport;
        private boolean mSeparateParts;
        private boolean mDeliveryReports = true;
        private boolean explicitSubscription;
        private int mSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        private Builder(Context context, @NonNull String message) {
            mContext = context;
            mMessage = message;
            mAttachment = null;
        }

        private Builder(Context context, @NonNull Attachment attachment) {
            mContext = context;
            mMessage = null;
            mAttachment = attachment;
        }

        private Builder(Context context, @NonNull String message, @NonNull Attachment attachment) {
            mContext = context;
            mMessage = message;
            mAttachment = attachment;
        }

        private void send(Text.Builder builder) {
            send(builder, mSubscriptionId);
        }

        private void send(Text.Builder builder, int subscription) {
            if (mMessage != null) {
                builder.message(mMessage);
            }
            if (mAttachment != null) {
                builder.attach(mAttachment);
            }
            for (Attachment attachment : additionalAttachments) builder.addAttachment(attachment);
            builder.subscriptionId(subscription);

            SendService.schedule(mContext, builder.build(), mSeparateParts, mDeliveryReports, mSubject, mMmsDeliveryReport, mMmsReadReport);
        }

        public Builder addAttachment(Attachment attachment) {
            if (attachment == null) throw new IllegalArgumentException("Missing attachment");
            additionalAttachments.add(attachment);
            return this;
        }

        public Builder subject(String subject) {
            mSubject = subject;
            return this;
        }

        public Builder requestMmsReports(boolean delivery, boolean read) {
            mMmsDeliveryReport = delivery;
            mMmsReadReport = read;
            return this;
        }

        /** Send long SMS as independent messages instead of concatenated multipart SMS. */
        public Builder separateSmsParts(boolean separate) {
            mSeparateParts = separate;
            return this;
        }

        /** Request delivery reports when the caller owns the SMS provider row. */
        public Builder requestSmsDeliveryReports(boolean enabled) {
            mDeliveryReports = enabled;
            return this;
        }

        /** Routes this outgoing SMS through a specific active SIM subscription. */
        public Builder usingSubscription(int subscriptionId) {
            if (subscriptionId < 0
                    && subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                throw new IllegalArgumentException("subscriptionId must be valid or INVALID_SUBSCRIPTION_ID");
            }
            explicitSubscription = true;
            mSubscriptionId = subscriptionId;
            return this;
        }

        public void to(String... addresses) {
            Text.Builder builder = new Text.Builder();
            for (String address : addresses) builder.addRecipient(mSubscriptionId < 0 ? lookupContact(address) : lookupContact(address, mSubscriptionId));
            send(builder);
        }

        public void to(Collection<Contact> addresses) {
            send(new Text.Builder().addRecipients(addresses));
        }

        public void to(Contact... addresses) {
            send(new Text.Builder().addRecipients(addresses));
        }
        public void to(Text text) {
            getMembersExceptMe(text).get(contacts -> send(new Text.Builder().addRecipients(contacts),
                    explicitSubscription ? mSubscriptionId : text.getSubscriptionId()));
        }
        public void to(Thread thread) {
            to(thread.getLatestMessage());
        }
    }

    /** Queue a complete MM1 Send.req, including Cc, Bcc, priority, expiry and MIME body. */
    @WorkerThread
    public void sendMms(SendReq request, int subscriptionId) {
        if (subscriptionId < -1) throw new IllegalArgumentException("Invalid subscription");
        byte[] bytes = new PduComposer(mContext, request).make();
        if (bytes == null || bytes.length > MmsPduStore.MAX_PDU_BYTES) {
            throw new IllegalArgumentException("Invalid or oversized MMS send request");
        }
        SendService.scheduleMms(mContext, bytes, subscriptionId);
    }

    /** Send a single binary SMS to an application port. The platform supplies the callback results. */
    @RequiresPermission(Manifest.permission.SEND_SMS)
    public void sendDataSms(String destination, int port, byte[] payload, int subscriptionId,
            PendingIntent sent, PendingIntent delivered) {
        if (TextUtils.isEmpty(destination) || port < 0 || port > 65535 || payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Missing destination, invalid port or empty data SMS");
        }
        SendService.resolveSmsManager(subscriptionId).sendDataMessage(destination, null, (short) port, payload, sent, delivered);
    }

    public Builder send(String message) {
        return new Builder(mContext, message);
    }

    public Builder send(String message, Attachment attachment) {
        return new Builder(mContext, message, attachment);
    }

    public Builder send(Attachment attachment) {
        return new Builder(mContext, attachment);
    }

    public Future<List<Text>> search(String text) {
        throw new RuntimeException("Unsupported");
    }

    @Override
    public Future<List<Text>> getMessages(final Thread thread) {
        return new FutureImpl<List<Text>>() {
            @Override
            public List<Text> get() {
                List<Text> messages = new ArrayList<>();
                Text.TextCursor c = getMessageCursor(thread);
                if (c.moveToFirst()) {
                    do {
                        messages.add(c.getText());
                    } while (c.moveToNext());
                }
                c.close();
                return messages;
            }
        };
    }

    @Override
    public Text.TextCursor getMessageCursor(Thread thread) {
        return getMessageCursor(thread.getId());
    }

    public Text.TextCursor getMessageCursor(String threadId) {
        ContentResolver contentResolver = mContext.getContentResolver();
        Uri uri = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, threadId);
        String order = "normalized_date ASC";

        final String[] mmsProjection = new String[]{
                BaseColumns._ID,
                Telephony.Mms.Part.CONTENT_TYPE,
                Telephony.Mms.Part.TEXT,
                Telephony.Mms.Part._DATA,
                Telephony.Mms.Part.MSG_ID
        };
        Uri mmsUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part");

        return new Text.TextCursor(
                contentResolver.query(uri, PROJECTION, null, null, order),
                contentResolver.query(mmsUri, mmsProjection, null, null, null));
    }

    @Override
    public Future<Text> getMessage(final String messageId) {
        return new FutureImpl<Text>() {
            @Override
            public Text get() {
                String clause = String.format("%s = %s",
                        Telephony.MmsSms._ID, messageId);
                ContentResolver contentResolver = mContext.getContentResolver();
                Uri uri = Telephony.MmsSms.CONTENT_URI;

                final String[] mmsProjection = new String[]{
                        BaseColumns._ID,
                        Telephony.Mms.Part.CONTENT_TYPE,
                        Telephony.Mms.Part.TEXT,
                        Telephony.Mms.Part._DATA,
                        Telephony.Mms.Part.MSG_ID
                };
                Uri mmsUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part");

                try (Text.TextCursor cursor = new Text.TextCursor(
                        contentResolver.query(uri, PROJECTION, clause, null, null),
                        contentResolver.query(mmsUri, mmsProjection, null, null, null))) {
                    if (cursor.moveToFirst()) {
                        return cursor.getText();
                    } else {
                        return null;
                    }
                }
            }
        };
    }

    @Override
    public Future<List<Thread>> getThreads() {
        return new FutureImpl<List<Thread>>() {
            @Override
            public List<Thread> get() {
                List<Thread> threads = new ArrayList<>();
                Cursor c = getThreadCursor();
                if (c.moveToFirst()) {
                    do {
                        threads.add(new Thread(c));
                    } while (c.moveToNext());
                }
                c.close();
                return threads;
            }
        };
    }

    @Override
    public Thread.ThreadCursor getThreadCursor() {
        ContentResolver contentResolver = mContext.getContentResolver();
        final Uri uri;
        final String order;
        if (PreKitKatUtils.requiresKitKatApis()) {
            uri = Uri.parse("content://mms-sms/conversations/?simple=true");
            order = "date DESC";
        } else {
            uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
            order = "normalized_date DESC";
        }
        Cursor threads = contentResolver.query(uri, null, null, null, order);

        List<String> ids = new ArrayList<>();
        List<Text> recentTexts = new ArrayList<>();

        while (threads.moveToNext()) {
            boolean isMms;
            int typeIndex = threads.getColumnIndex(Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN);
            if (typeIndex < 0) {
                // Type column not in projection, use another discriminator
                String cType = null;
                int cTypeIndex = threads.getColumnIndex(Telephony.Mms.CONTENT_TYPE);
                if (cTypeIndex >= 0) {
                    cType = threads.getString(threads.getColumnIndex(Telephony.Mms.CONTENT_TYPE));
                }
                // If content type is present, this is an MMS message
                isMms = cType != null;
            } else {
                isMms = threads.getString(typeIndex).equals("mms");
            }

            boolean incoming = Text.isIncomingMessage(threads, true);
            long id = -1;
            if (!PreKitKatUtils.requiresKitKatApis()) {
                id = threads.getLong(threads.getColumnIndexOrThrow(BaseColumns._ID));
                if (isMms) {
                    ids.add(Long.toString(id));
                }
            }
            long threadId = threads.getLong(threads.getColumnIndexOrThrow(Thread.THREAD_ID));
            long date = threads.getLong(threads.getColumnIndexOrThrow(Telephony.Sms.Conversations.DATE));
            Set<String> memberAddresses = new HashSet<>();
            String senderAddress = null;
            String body = null;
            long mmsId = -1;
            int status = -1;

            if (isMms) {
                if (!PreKitKatUtils.requiresKitKatApis()) {
                    date = date * 1000;
                    mmsId = threads.getLong(threads.getColumnIndex(Telephony.Mms._ID));
                    status = threads.getInt(threads.getColumnIndexOrThrow(Telephony.Mms.STATUS));
                }
            } else {
                if (PreKitKatUtils.requiresKitKatApis()) {
                    try (Cursor smsSamsung = contentResolver.query(
                            Uri.withAppendedPath(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, Long.toString(threadId)),
                            PROJECTION,
                            null,
                            null,
                            Mock.ORDER_NORMALIZED_DATE_ASC)) {
                        if (smsSamsung.moveToLast()) {
                            id = smsSamsung.getLong(smsSamsung.getColumnIndexOrThrow(BaseColumns._ID));
                            int ti = smsSamsung.getColumnIndex(Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN);
                            if (ti < 0) {
                                // Type column not in projection, use another discriminator
                                String cType = null;
                                int cTypeIndex = smsSamsung.getColumnIndex(Telephony.Mms.CONTENT_TYPE);
                                if (cTypeIndex >= 0) {
                                    cType = smsSamsung.getString(smsSamsung.getColumnIndex(Telephony.Mms.CONTENT_TYPE));
                                }
                                // If content type is present, this is an MMS message
                                isMms = cType != null;
                            } else {
                                isMms = smsSamsung.getString(ti).equals("mms");
                            }

                            if (isMms) {
                                date = date * 1000;
                                ids.add(Long.toString(id));
                                mmsId = smsSamsung.getLong(smsSamsung.getColumnIndex(Telephony.Mms._ID));
                                status = smsSamsung.getInt(smsSamsung.getColumnIndexOrThrow(Telephony.Mms.STATUS));
                            } else {
                                senderAddress = smsSamsung.getString(smsSamsung.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                                // If the sender is null that means its a failed mms soo populate data with a different message
                                if (senderAddress == null) {
                                    int position = smsSamsung.getPosition();
                                    while (senderAddress == null && smsSamsung.moveToNext()) {
                                        senderAddress = smsSamsung.getString(smsSamsung.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                                    }
                                    smsSamsung.moveToPosition(position);
                                }
                                memberAddresses.add(senderAddress);

                                body = smsSamsung.getString(smsSamsung.getColumnIndexOrThrow(Telephony.Sms.BODY));
                                status = smsSamsung.getInt(smsSamsung.getColumnIndexOrThrow(Telephony.Sms.STATUS));
                            }
                        }
                    }
                } else {
                    senderAddress = threads.getString(threads.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));

                    // If the sender is null that means its a failed mms, so populate data with a different message
                    if (senderAddress == null || senderAddress.equals(UNKNOWN)) {
                        try (Cursor smsFailed = contentResolver.query(
                                Uri.withAppendedPath(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, Long.toString(threadId)),
                                PROJECTION,
                                null,
                                null,
                                Mock.ORDER_NORMALIZED_DATE_ASC)) {
                            while ((senderAddress == null || senderAddress.equals(UNKNOWN)) && smsFailed.moveToNext()) {
                                senderAddress = smsFailed.getString(smsFailed.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                            }
                        }
                    }
                    memberAddresses.add(senderAddress);

                    body = threads.getString(threads.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    status = threads.getInt(threads.getColumnIndexOrThrow(Telephony.Sms.STATUS));
                }
            }

            recentTexts.add(new Text(
                    id,
                    threadId,
                    date,
                    mmsId,
                    status,
                    body,
                    incoming,
                    isMms,
                    senderAddress,
                    memberAddresses,
                    null /* attachment */
            ));
        }

        if (!ids.isEmpty()) {
            String[] mmsArgs = ids.toArray(new String[0]);
            final String[] mmsProjection = new String[]{
                    BaseColumns._ID,
                    Telephony.Mms.Part.CONTENT_TYPE,
                    Telephony.Mms.Part.TEXT,
                    Telephony.Mms.Part._DATA,
                    Telephony.Mms.Part.MSG_ID
            };
            Uri mmsUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part");

            try (Cursor mms = contentResolver.query(
                    mmsUri, mmsProjection, buildMmsPartSelection(ids.size()), mmsArgs, null)) {
                if (mms != null) {
                    for (Text text : recentTexts) {
                        if (!mms.moveToFirst()) {
                            continue;
                        }
                        do {
                            if (text.isMms() && mms.getLong(mms.getColumnIndexOrThrow(Telephony.Mms.Part.MSG_ID)) == text.getIdAsLong()) {
                                text.readPart(mms);
                            }
                        } while (mms.moveToNext());
                    }
                }
            }
        }

        String unreadMessagesClause = String.format("%s=%s", Telephony.Sms.READ, 0);
        Uri inboxUri = Telephony.Sms.Inbox.CONTENT_URI;
        return new Thread.ThreadCursor(threads,
                contentResolver.query(inboxUri, null, unreadMessagesClause, null, null),
                recentTexts);
    }

    static String buildMmsPartSelection(int messageCount) {
        if (messageCount <= 0) {
            return null;
        }
        StringBuilder selection = new StringBuilder(Telephony.Mms.Part.MSG_ID + " = ?");
        for (int i = 1; i < messageCount; i++) {
            selection.append(" OR ").append(Telephony.Mms.Part.MSG_ID).append(" = ?");
        }
        return selection.toString();
    }

    public List<Attachment> getAttachments(Thread thread) {
        List<Attachment> attachments = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        ContentResolver resolver = mContext.getContentResolver();
        String[] messageProjection = {BaseColumns._ID};

        try (Cursor messages = resolver.query(
                Telephony.Mms.CONTENT_URI,
                messageProjection,
                Telephony.Mms.THREAD_ID + " = ?",
                new String[]{thread.getId()},
                null)) {
            if (messages == null) {
                return attachments;
            }
            while (messages.moveToNext()) {
                ids.add(messages.getString(messages.getColumnIndexOrThrow(BaseColumns._ID)));
            }
        }

        if (ids.isEmpty()) {
            return attachments;
        }

        String[] partProjection = new String[]{
                BaseColumns._ID,
                Telephony.Mms.Part.CONTENT_TYPE,
                Telephony.Mms.Part.MSG_ID
        };
        Uri partsUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part");
        try (Cursor parts = resolver.query(
                partsUri,
                partProjection,
                buildMmsPartSelection(ids.size()),
                ids.toArray(new String[0]),
                null)) {
            if (parts == null) {
                return attachments;
            }
            while (parts.moveToNext()) {
                String contentType = parts.getString(
                        parts.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE));
                if (contentType == null) {
                    continue;
                }

                long partId = parts.getLong(parts.getColumnIndexOrThrow(BaseColumns._ID));
                Uri partUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part/" + partId);
                Attachment attachment = Attachment.fromPart(contentType, partUri);
                if (attachment != null) attachments.add(attachment);
            }
        }
        return attachments;
    }

    public Future<Thread> getThread(long threadId) {
        return getThread(Long.toString(threadId));
    }

    @Override
    public Future<Thread> getThread(final String threadId) {
        return new FutureImpl<Thread>() {
            @Override
            public Thread get() {
                String clause = String.format("%s = %s", Thread.THREAD_ID, threadId);
                ContentResolver contentResolver = mContext.getContentResolver();
                final Uri uri;
                final String order;
                if (PreKitKatUtils.requiresKitKatApis()) {
                    uri = Uri.parse("content://mms-sms/conversations/?simple=true");
                    order = "date DESC";
                } else {
                    uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
                    order = "normalized_date DESC";
                }

                String unreadMessagesClause = String.format("%s=%s", Telephony.Sms.READ, 0);
                Uri inboxUri = Telephony.Sms.Inbox.CONTENT_URI;

                List<Text> messages = new ArrayList<>();
                Text.TextCursor c = getMessageCursor(threadId);
                if (c.moveToFirst()) {
                    do {
                        messages.add(c.getText());
                    } while (c.moveToNext());
                }
                c.close();

                try (Thread.ThreadCursor cursor = new Thread.ThreadCursor(
                        contentResolver.query(uri, null, clause, null, order),
                        contentResolver.query(inboxUri, null, unreadMessagesClause, null, null),
                        messages)) {
                    if (cursor.moveToFirst()) {
                        return cursor.getThread();
                    } else {
                        return null;
                    }
                }
            }
        };
    }

    public void delete(Collection<Text> messages) {
        delete(messages.toArray(new Text[0]));
    }

    @Override
    public void delete(Text... messages) {
        for (Text message : messages) {
            Uri uri = message.isMms()
                    ? ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, message.getMmsId())
                    : ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, Long.parseLong(message.getId()));
            mContext.getContentResolver().delete(uri, null, null);
        }
    }

    @Override
    public void delete(Thread... threads) {
        for (Thread thread : threads) {
            Uri uri = ContentUris.withAppendedId(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, thread.getIdAsLong());
            mContext.getContentResolver().delete(uri, null, null);
        }
    }

    @Override
    public void markAsRead(Text message) {
        if (message.isMms()) {
            markMmsAsRead(message.getMmsId());
        } else {
            markSmsAsRead(Long.parseLong(message.getId()));
        }
    }

    public void markSmsAsRead(long messageId) {
        markRead(Telephony.Sms.Inbox.CONTENT_URI, BaseColumns._ID, messageId);
    }

    public void markMmsAsRead(long messageId) {
        markRead(Telephony.Mms.Inbox.CONTENT_URI, BaseColumns._ID, messageId);
    }

    @Override
    public void markAsRead(Thread thread) {
        markThreadAsRead(thread.getIdAsLong());
    }

    public void markThreadAsRead(long threadId) {
        markRead(Telephony.Sms.Inbox.CONTENT_URI, Telephony.Sms.THREAD_ID, threadId);
        markRead(Telephony.Mms.Inbox.CONTENT_URI, Telephony.Mms.THREAD_ID, threadId);
    }

    private void markRead(Uri inbox, String column, long id) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.READ, 1);
        mContext.getContentResolver().update(inbox, values, column + " = ? AND read = 0",
                new String[] {Long.toString(id)});
    }

    public Contact.ContactCursor getContactCursor() {
        return getContactCursor(Contact.Sort.Alphabetical);
    }

    public Contact.ContactCursor getContactCursor(Contact.Sort sortOrder) {
        return queryContacts(ContactsContract.Contacts.CONTENT_URI, null, sortOrder);
    }

    public Contact.ContactCursor getContactCursor(String partialName) {
        return getContactCursor(partialName, Contact.Sort.Alphabetical);
    }

    public Contact.ContactCursor getContactCursor(String partialName, Contact.Sort sortOrder) {
        Uri uri = ContactsContract.Contacts.CONTENT_FILTER_URI.buildUpon().appendPath(partialName).build();
        return queryContacts(uri, ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", sortOrder);
    }

    private Contact.ContactCursor queryContacts(Uri uri, String selection, Contact.Sort order) {
        if (TelephonyPermissions.has(mContext, Manifest.permission.READ_CONTACTS)) {
            try {
                Cursor c = mContext.getContentResolver().query(uri, null, selection, null, order.getKey());
                if (c != null) return new Contact.ContactCursor(c);
            } catch (SecurityException revoked) {
                // Return an empty cursor if the grant changed during the query.
            }
        }
        return new Contact.ContactCursor(new MatrixCursor(new String[]{BaseColumns._ID}));
    }

    /** Personal dialing records on one active SIM. Android exposes this provider from API 31. */
    public List<Contact> getSimContacts(int subscriptionId) {
        if (Build.VERSION.SDK_INT < 31 || subscriptionId < 0
                || !TelephonyPermissions.has(mContext, Manifest.permission.READ_CONTACTS)) return Collections.emptyList();
        return SimContacts.read(mContext.getContentResolver(), subscriptionId);
    }

    public List<Contact> getSimContacts() {
        List<Contact> contacts = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 31 && TelephonyPermissions.has(mContext, Manifest.permission.READ_CONTACTS)) {
            for (int sub : SimContacts.subscriptions(mContext.getContentResolver())) contacts.addAll(getSimContacts(sub));
        }
        return contacts;
    }

    public Contact lookupContact(String address) {
        int subscription = Build.VERSION.SDK_INT >= 24 ? SubscriptionManager.getDefaultSmsSubscriptionId() : -1;
        return lookupContact(address, subscription);
    }

    String dialingRegion(int subscription) {
        try {
            TelephonyManager phone = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (phone != null) {
                if (Build.VERSION.SDK_INT >= 24 && subscription >= 0) phone = phone.createForSubscriptionId(subscription);
                if (phone != null) {
                    String iso = phone.getSimCountryIso();
                    if (!TextUtils.isEmpty(iso)) return iso;
                    iso = phone.getNetworkCountryIso();
                    if (!TextUtils.isEmpty(iso)) return iso;
                }
            }
        } catch (SecurityException | UnsupportedOperationException unavailable) {
            // Devices without a usable telephony subscription can still resolve local contacts.
        }
        return Locale.getDefault().getCountry();
    }

    public Contact lookupContact(String address, int subscriptionId) {
        if (address != null && address.matches("[\\p{L}][\\p{L} .'-]*")
                && TelephonyPermissions.has(mContext, Manifest.permission.READ_CONTACTS)) {
            Contact selected = null;
            try (Contact.ContactCursor rows = getContactCursor(address)) {
                while (rows.moveToNext()) {
                    Contact candidate = rows.getContact();
                    if (address.equalsIgnoreCase(candidate.getDisplayName())) {
                        if (selected != null) return Contact.UNKNOWN;
                        selected = candidate;
                    }
                }
            }
            if (selected != null) return selected;
        }
        return lookupAddress(address, subscriptionId);
    }

    private Contact lookupAddress(String address, int subscriptionId) {
        if (TextUtils.isEmpty(address)) return Contact.UNKNOWN;
        Contact fallback = new Contact(address);
        if (!TelephonyPermissions.has(mContext, Manifest.permission.READ_CONTACTS)) return fallback;
        String region = dialingRegion(subscriptionId);
        String key = PhoneNumbers.normalize(address, region);
        boolean email = address.contains("@");
        Uri base = email ? ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI
                : ContactsContract.PhoneLookup.CONTENT_FILTER_URI;
        try (Cursor rows = mContext.getContentResolver().query(base.buildUpon().appendPath(key).build(),
                null, null, null, null)) {
            while (rows != null && rows.moveToNext()) {
                String stored = email ? rows.getString(rows.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS))
                        : rows.getString(rows.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER));
                if (email ? address.equalsIgnoreCase(stored) : key.equals(PhoneNumbers.normalize(stored, region))) {
                    return new Contact(address, rows);
                }
            }
        } catch (SecurityException revoked) {
            return fallback;
        }
        if (!email) {
            List<Contact> matches = subscriptionId >= 0 ? getSimContacts(subscriptionId) : getSimContacts();
            Contact match = null;
            for (Contact sim : matches) {
                if (key.equals(PhoneNumbers.normalize(sim.getNumber(), region))) {
                    if (match != null) return fallback;
                    match = new Contact(address, sim.getDisplayName(), sim.getSimRecordUri());
                }
            }
            if (match != null) return match;
        }
        return fallback;
    }

    @Nullable
    @SuppressLint({"HardwareIds", "InlinedApi", "MissingPermission"})
    public String getPhoneNumber() {
        return getPhoneNumber(-1);
    }

    @SuppressLint({"HardwareIds", "MissingPermission"})
    String getPhoneNumber(int subscription) {
        if (!TelephonyPermissions.canReadPhoneNumber(mContext)) {
            return null;
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                SubscriptionManager subscriptionManager =
                        (SubscriptionManager) mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (subscriptionManager != null) {
                    String phoneNumber = subscriptionManager.getPhoneNumber(
                            subscription < 0 ? SubscriptionManager.getDefaultSmsSubscriptionId() : subscription);
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        return phoneNumber;
                    }
                }
            }

            TelephonyManager telephonyManager =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= 24 && subscription >= 0 && telephonyManager != null) {
                telephonyManager = telephonyManager.createForSubscriptionId(subscription);
            }
            return telephonyManager == null ? null : telephonyManager.getLine1Number();
        } catch (SecurityException | UnsupportedOperationException e) {
            Log.w(TAG, "Phone number is unavailable", e);
            return null;
        }
    }

    public Contact getSelf() {
        String phoneNumber = getPhoneNumber();
        return phoneNumber != null ? lookupContact(phoneNumber) : Contact.UNKNOWN;
    }

    private void buildSender(Text text) {
        if (!TelephonyPermissions.has(mContext, Manifest.permission.READ_SMS)) {
            return;
        }
        Uri addressUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, text.getId() + "/addr");

        try (Cursor addresses = mContext.getContentResolver().query(
                addressUri, null, null, null, null)) {
            if (addresses == null) {
                return;
            }
            while (addresses.moveToNext()) {
                if (addresses.getLong(addresses.getColumnIndex(Telephony.Mms.Addr.MSG_ID))
                        == text.getMmsId()) {
                    String address = addresses.getString(
                            addresses.getColumnIndex(Telephony.Mms.Addr.ADDRESS));
                    int type = addresses.getInt(addresses.getColumnIndexOrThrow(Telephony.Mms.Addr.TYPE));
                    if (type == PduHeaders.BCC) continue; // A reply must not disclose blind recipients.
                    if (type == Text.TYPE_SENDER) {
                        text.setSenderAddress(address);
                    }
                    if (!TextUtils.isEmpty(address) && !"insert-address-token".equals(address)) text.setMemberAddress(address);
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "MMS addresses are unavailable", e);
        }
    }

    @Override
    public synchronized Future<Contact> getSender(final Text text) {
        final Contact sender = text.getSender();
        if (sender != null) {
            return new Present<>(sender);
        } else {
            return new FutureImpl<Contact>() {
                @Override
                public Contact get() {
                    if (text.getSenderAddress() == null && text.isMms()) {
                        buildSender(text);
                    }
                    Contact sender = text.isIncoming() ? lookupAddress(text.getSenderAddress(), text.getSubscriptionId()) : getSelf();
                    text.setSender(sender);
                    return sender;
                }
            };
        }
    }

    @Override
    public synchronized Future<Set<Contact>> getMembers(final Text text) {
        final Set<Contact> members = text.getMembers();
        if (members != null && !members.isEmpty()) {
            return new Present<>(text.getMembers());
        } else {
            return new FutureImpl<Set<Contact>>() {
                @Override
                public Set<Contact> get() {
                    if (text.getMemberAddresses().isEmpty() && text.isMms()) {
                        buildSender(text);
                    }
                    for (String address : text.getMemberAddresses()) {
                        text.addMember(lookupAddress(address, text.getSubscriptionId()));
                    }
                    return text.getMembers();
                }
            };
        }
    }
    public synchronized Future<Set<Contact>> getMembersExceptMe(final Text text) {
        return new FutureImpl<Set<Contact>>() {
            @Override
            public Set<Contact> get() {
                Set<Contact> members = new HashSet<>(getMembers(text).get());
                if (members.size() == 1) {
                    // It's possible to text yourself. To account for that, don't remove yourself if there's only one member.
                    return members;
                }
                String self = getPhoneNumber(text.getSubscriptionId());
                if (!TextUtils.isEmpty(self)) {
                    String region = dialingRegion(text.getSubscriptionId());
                    String key = PhoneNumbers.normalize(self, region);
                    Iterator<Contact> iterator = members.iterator();
                    while (iterator.hasNext()) {
                        if (key.equals(PhoneNumbers.normalize(iterator.next().getNumber(), region))) iterator.remove();
                    }
                }
                return members;
            }
        };
    }

    @Override
    public int getUnreadCount(Thread thread) {
        return thread.getUnreadCount();
    }

    @Override
    public int getCount(Thread thread) {
        String proj = String.format("%s=%s", Thread.THREAD_ID, thread.getId());
        Uri uri = Telephony.Sms.Inbox.CONTENT_URI;
        Cursor c = mContext.getContentResolver().query(uri, null, proj, null, null);
        int count = c.getCount();
        c.close();
        return count;
    }

    /**
     * The default sms app. Before api 19, this returns null.
     */
    @Nullable
    public String getDefaultSmsPackage() {
        return Telephony.Sms.getDefaultSmsPackage(mContext);
    }

    /**
     * Returns true if your package is the default SMS app. Before API 19, that means everyone.
     */
    public boolean isDefaultSmsPackage() {
        if (Build.VERSION.SDK_INT >= 29 && supportsSms()) {
            RoleManager roleManager = (RoleManager) mContext.getSystemService(Context.ROLE_SERVICE);
            return supportsSms() && roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && roleManager.isRoleHeld(RoleManager.ROLE_SMS);
        } else if (Build.VERSION.SDK_INT >= 19 && supportsSms()) {
            return mContext.getPackageName().equals(getDefaultSmsPackage());
        } else {
            return supportsSms();
        }
    }

    public boolean supportsSms() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    @TargetApi(24)
    public void block(Contact contact) {
        if (BlockedNumberContract.canCurrentUserBlockNumbers(mContext)) {
            ContentValues values = new ContentValues();
            values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, contact.getMobileNumber(mContext).get());
            mContext.getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
        }
    }

    @TargetApi(24)
    public void unblock(Contact contact) {
        if (BlockedNumberContract.canCurrentUserBlockNumbers(mContext)) {
            BlockedNumberContract.unblock(mContext, contact.getMobileNumber(mContext).get());
        }
    }

    @TargetApi(24)
    public boolean isBlocked(Contact contact) {
        if (BlockedNumberContract.canCurrentUserBlockNumbers(mContext)) {
            return BlockedNumberContract.isBlocked(mContext, contact.getMobileNumber(mContext).get());
        } else {
            return false;
        }
    }
}
