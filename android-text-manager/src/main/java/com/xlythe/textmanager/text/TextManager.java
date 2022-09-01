package com.xlythe.textmanager.text;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.role.RoleManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.BaseColumns;
import android.provider.BlockedNumberContract;
import android.provider.ContactsContract;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;
import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.RetrieveConf;
import com.xlythe.textmanager.text.util.PreKitKatUtils;
import com.xlythe.textmanager.text.Mock.Telephony;

import java.util.ArrayList;
import java.util.Collection;
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
    private static final int CACHE_SIZE = 50;
    private static final String UNKNOWN = "Unknown";
    private static final String TEXT_EXTRA = "text";
    private static final String[] PROJECTION = new String[] {
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
            // For MMS only
            Telephony.Mms.SUBJECT,
            Telephony.Mms.MESSAGE_BOX,
            Telephony.Mms.STATUS
    };

    @SuppressLint("StaticFieldLeak")
    private static TextManager sTextManager;

    public static synchronized TextManager getInstance(Context context) {
        if (sTextManager == null) {
            sTextManager = new TextManager(context.getApplicationContext());
        }
        return sTextManager;
    }

    private final Context mContext;
    private final Set<MessageObserver> mObservers = new HashSet<>();
    private final LruCache<String, Contact> mContactCache = new LruCache<>(CACHE_SIZE);

    private TextManager(Context context) {
        mContext = context;
        context.getContentResolver().registerContentObserver(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, true, new TextObserver(new Handler(Looper.getMainLooper())));
    }

    public void downloadAttachment(Text text){
        if (text.isMms()) {
            Intent intent = new Intent();
            intent.putExtra(TEXT_EXTRA, text);
            new ReceivePushTask(mContext).execute(intent);
        }
    }

    // TODO: Merge with MmsReceiveService
    private static class ReceivePushTask extends AsyncTask<Intent, Void, Void> {
        private static final String TAG_MMS = "sms:mms";

        @SuppressLint("StaticFieldLeak")
        private final Context mContext;

        ReceivePushTask(Context context) {
            mContext = context;
        }

        @WorkerThread
        @Override
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        protected Void doInBackground(Intent... intents) {
            Text text = intents[0].getParcelableExtra(TEXT_EXTRA);
            PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_MMS);
            try {
                wakelock.acquire(5000);

                Uri databaseUri = Uri.withAppendedPath(Telephony.Mms.Inbox.CONTENT_URI, text.getId());
                String url = getContentUrl(databaseUri);
                if (url == null) {
                    Log.e(TAG, "Failed to parse text " + text + ". Unable to receive text.");
                    return null;
                }

                if (!NetworkUtils.forceDataConnection(mContext)) {
                    Log.e(TAG, "Failed to connect to a mobile network. Unable to receive text " + text);
                    return null;
                }

                byte[] data = Receive.receive(mContext, url);
                RetrieveConf retrieveConf = (RetrieveConf) new PduParser(data, true).parse();

                PduPersister persister = PduPersister.getPduPersister(mContext);
                Uri msgUri;
                try {
                    msgUri = persister.persist(retrieveConf, databaseUri, true, true, null);

                    // Use local time instead of PDU time
                    ContentValues values = new ContentValues(1);
                    values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
                    values.put(Telephony.Mms.STATUS, Telephony.Sms.Sent.STATUS_COMPLETE);
                    mContext.getContentResolver().update(msgUri, values, null, null);
                } catch (MmsException e) {
                    Log.e(TAG, "Failed to persist text " + text);
                    return null;
                }
            } finally {
                wakelock.release();
            }

            return null;
        }

        @Nullable
        private String getContentUrl(Uri uri) {
            String[] projection = new String[] {
                    Telephony.Mms.CONTENT_LOCATION
            };
            Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null) {
                return null;
            }

            try {
                if (cursor.getCount() != 1 || !cursor.moveToFirst()) {
                    return null;
                }

                return cursor.getString(COLUMN_CONTENT_LOCATION);
            } finally {
                cursor.close();
            }
        }
    }

    public void registerObserver(MessageObserver observer) {
        mObservers.add(observer);
    }

    public void unregisterObserver(MessageObserver observer) {
        mObservers.remove(observer);
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
            for (MessageObserver observer : mObservers) {
                observer.notifyDataChanged();
            }
        }
    }

    public void send(Text text) {
        SendService.schedule(mContext, text);
    }

    public class Builder {
        private final Context mContext;
        @Nullable private final String mMessage;
        @Nullable private final Attachment mAttachment;

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
            if (mMessage != null) {
                builder.message(mMessage);
            }
            if (mAttachment != null) {
                builder.attach(mAttachment);
            }

            SendService.schedule(mContext, builder.build());
        }

        public void to(String... addresses) {
            send(new Text.Builder().addRecipients(mContext, addresses));
        }

        public void to(Collection<Contact> addresses) {
            send(new Text.Builder().addRecipients(addresses));
        }

        public void to(Contact... addresses) {
            send(new Text.Builder().addRecipients(addresses));
        }

        @SuppressLint("InlinedApi")
        @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
        public void to(Text text) {
            getMembersExceptMe(text).get(contacts -> send(new Text.Builder().addRecipients(contacts)));
        }

        @SuppressLint("InlinedApi")
        @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
        public void to(Thread thread) {
            getMembersExceptMe(thread.getLatestMessage()).get(contacts -> send(new Text.Builder().addRecipients(contacts)));
        }
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
        Uri mmsUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "/part");

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
                Uri mmsUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "/part");

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

        String[] mmsArgs = new String[ids.size()];
        mmsArgs = ids.toArray(mmsArgs);

        StringBuilder mmsClause = new StringBuilder(Telephony.Mms.Part.MSG_ID + " = ?");
        for (int i = 0; i < ids.size() - 1; i++) {
            mmsClause.append(" OR ").append(Telephony.Mms.Part.MSG_ID).append(" = ?");
        }

        String unreadMessagesClause = String.format("%s=%s",  Telephony.Sms.READ, 0);
        Uri inboxUri = Telephony.Sms.Inbox.CONTENT_URI;

        final String[] mmsProjection = new String[]{
                BaseColumns._ID,
                Telephony.Mms.Part.CONTENT_TYPE,
                Telephony.Mms.Part.TEXT,
                Telephony.Mms.Part._DATA,
                Telephony.Mms.Part.MSG_ID
        };
        Uri mmsUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "/part");

        try (Cursor mms = contentResolver.query(mmsUri, mmsProjection, mmsClause.toString(), mmsArgs, null)) {
            for (Text text : recentTexts) {
                mms.moveToFirst();
                while (mms.moveToNext()) {
                    if (mms.getLong(mms.getColumnIndex(Telephony.Mms.Part.MSG_ID)) == text.getIdAsLong()) {
                        String contentType = mms.getString(mms.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE));
                        if (contentType == null) {
                            continue;
                        }

                        if (contentType.matches("image/.*")) {
                            // Find any part that is an image attachment
                            long partId = mms.getLong(mms.getColumnIndex(BaseColumns._ID));
                            text.setAttachment(new ImageAttachment(Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part/" + partId)));
                        } else if (contentType.matches("text/.*")) {
                            // Find any part that is text data
                            text.setBody(mms.getString(mms.getColumnIndex(Telephony.Mms.Part.TEXT)));
                        } else if (contentType.matches("video/.*")) {
                            long partId = mms.getLong(mms.getColumnIndex(BaseColumns._ID));
                            text.setAttachment(new VideoAttachment(Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part/" + partId)));
                        }
                    }
                }
            }
        }

        return new Thread.ThreadCursor(threads,
                contentResolver.query(inboxUri, null, unreadMessagesClause, null, null),
                recentTexts);
    }

    public List<Attachment> getAttachments(Thread thread) {
        List<Attachment> attachments = new ArrayList<>();
        List<String> ids = new ArrayList<>();

        String[] MMS_PROJECTION = new String[]{
                BaseColumns._ID,
                Telephony.Mms.THREAD_ID,
                Telephony.Mms.TEXT_ONLY
        };

        Uri messageUri = Telephony.Mms.CONTENT_URI;
        Cursor inner = mContext.getContentResolver().query(
                messageUri,
                MMS_PROJECTION,
                Telephony.Mms.THREAD_ID + " = " + thread.getId(),
                null,
                null
        );

        while (inner.moveToNext()) {
            int contentType = inner.getInt(inner.getColumnIndex(Telephony.Mms.TEXT_ONLY));
            if (contentType == 1) {
                continue;
            }
            if (contentType == 0) {
                ids.add(inner.getString(inner.getColumnIndex(BaseColumns._ID)));
            }
        }
        inner.close();

        MMS_PROJECTION = new String[]{
                BaseColumns._ID,
                Telephony.Mms.Part.CONTENT_TYPE,
                Telephony.Mms.Part.MSG_ID
        };

        String[] args = new String[ids.size()];
        args = ids.toArray(args);

        StringBuilder clause = new StringBuilder(Telephony.Mms.Part.MSG_ID + " = ?");
        for (int i = 0; i < ids.size() - 1; i++) {
            clause.append(" OR ").append(Telephony.Mms.Part.MSG_ID).append(" = ?");
        }

        messageUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "/part");
        inner = mContext.getContentResolver().query(
                messageUri,
                MMS_PROJECTION,
                clause.toString(),
                args,
                null
        );

        while (inner.moveToNext()) {
            String contentType = inner.getString(inner.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE));
            if (contentType == null) {
                continue;
            }

            if (contentType.matches("image/.*")) {
                // Find any part that is an image attachment
                long partId = inner.getLong(inner.getColumnIndex(BaseColumns._ID));
                attachments.add(new ImageAttachment(Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part/" + partId)));
            } else if (contentType.matches("video/.*")) {
                long partId = inner.getLong(inner.getColumnIndex(BaseColumns._ID));
                attachments.add(new VideoAttachment(Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part/" + partId)));
            }
        }
        inner.close();

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
        StringBuilder ids = new StringBuilder();
        for (Text text : messages) {
            if (ids.length() > 0) {
                ids.append(",");
            }
            ids.append(text.getId());
        }
        String clause = String.format("%s in (%s)", Telephony.Sms._ID, ids);
        int rowsDeleted = mContext.getContentResolver().delete(
                Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, clause, null);
        if (DEBUG) {
            Log.i(TAG, String.format("Deleting %s. %s rows deleted", ids, rowsDeleted));
        }
    }

    @Override
    public void delete(Thread... threads) {
        StringBuilder ids = new StringBuilder();
        for (Thread thread : threads) {
            if (ids.length() > 0) {
                ids.append(",");
            }
            ids.append(thread.getId());
        }
        String clause = String.format("%s in (%s)", Telephony.Sms.THREAD_ID, ids);
        int rowsDeleted = mContext.getContentResolver().delete(
                Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, clause, null);
        if (DEBUG) {
            Log.i(TAG, String.format("Deleting %s. %s rows deleted", ids, rowsDeleted));
        }
    }

    @Override
    public void markAsRead(Text message) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.READ, true);
        String clause = String.format("%s = %s",
                Telephony.Sms._ID, message.getId());
        int rowsUpdated = mContext.getContentResolver().update(
                Telephony.Sms.Inbox.CONTENT_URI, values, clause, null);
        if (DEBUG) {
            Log.i(TAG, String.format("Marking %s as read. %s rows updated", message, rowsUpdated));
        }
    }

    @Override
    public void markAsRead(Thread thread) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.READ, true);
        String clause = String.format("%s=%s AND %s=%s",
                Telephony.Sms.THREAD_ID, thread.getId(),
                Telephony.Sms.READ, 0);
        int rowsUpdated = mContext.getContentResolver().update(
                Telephony.Sms.Inbox.CONTENT_URI, values, clause, null);
        if (DEBUG) {
            Log.i(TAG, String.format("Marking %s as read. %s rows updated", thread, rowsUpdated));
        }
    }

    public Contact.ContactCursor getContactCursor() {
        return getContactCursor(Contact.Sort.Alphabetical);
    }

    public Contact.ContactCursor getContactCursor(Contact.Sort sortOrder) {
        ContentResolver contentResolver = mContext.getContentResolver();
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        return new Contact.ContactCursor(contentResolver.query(uri, null, null, null, sortOrder.getKey()));
    }

    public Contact.ContactCursor getContactCursor(String partialName) {
        return getContactCursor(partialName, Contact.Sort.Alphabetical);
    }

    public Contact.ContactCursor getContactCursor(String partialName, Contact.Sort sortOrder) {
        String clause = String.format("%s = 1",
                ContactsContract.PhoneLookup.HAS_PHONE_NUMBER);

        ContentResolver contentResolver = mContext.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, partialName);
        return new Contact.ContactCursor(contentResolver.query(uri, null, clause, null, sortOrder.getKey()));
    }

    /**
     * Sanitizes a phone number, stripping out any extensions and country codes.
     *
     * WARNING: If you then try to send a text to this number (and it was international),
     * the text will not send. The country code was stripped out.
     */
    private String sanitizeNumber(String phoneNumber) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNumber, Locale.getDefault().getCountry());
            return Long.toString(numberProto.getNationalNumber());
        } catch (NumberParseException e) {
            e.printStackTrace();
        }
        return phoneNumber;
    }

    public Contact lookupContact(String phoneNumber) {
        String sanitizedNumber = sanitizeNumber(phoneNumber);
        if (sanitizedNumber == null) {
            return Contact.UNKNOWN;
        }
        Contact contact = mContactCache.get(sanitizedNumber);
        if (contact == null) {
            Cursor c;
            if (phoneNumber.matches(".*\\d+.*")) {
                // Found a phone number (probably). We'll look up using the sanitized number,
                // as that will match even if the international number is stored as a contact.
                ContentResolver contentResolver = mContext.getContentResolver();
                Uri uri = Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(sanitizedNumber));

                c = contentResolver.query(uri, null, null, null, null);
            } else {
                // This isn't a phone number! This is a name!
                c = getContactCursor(phoneNumber);
            }

            try {
                if (c != null && c.moveToFirst()) {
                    contact = new Contact(phoneNumber, c);
                } else {
                    // There was no contact with this number. Use the full (unsanitized) number
                    // so that we don't lose any information (such as country code).
                    contact = new Contact(phoneNumber);

                    if (TextUtils.isEmpty(contact.getNumber())) {
                        contact = Contact.UNKNOWN;
                    }
                }
            } finally {
                if (c != null) c.close();
            }
            // Save the contact in the cache with the sanitized number, in case they're looking up
            // the contact both with and without country codes.
            mContactCache.put(sanitizedNumber, contact);
        }
        return contact;
    }

    @Nullable
    @SuppressLint("InlinedApi")
    @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
    public String getPhoneNumber() {
        if (Build.VERSION.SDK_INT >= 33) {
            SubscriptionManager subscriptionManager = (SubscriptionManager) mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            String phoneNumber = subscriptionManager.getPhoneNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
            if (phoneNumber != null) {
                return phoneNumber;
            }
        }

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        @SuppressLint("HardwareIds") String phoneNumber = telephonyManager.getLine1Number();
        return phoneNumber;
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
    public Contact getSelf() {
        String phoneNumber = getPhoneNumber();
        return phoneNumber != null ? lookupContact(phoneNumber) : Contact.UNKNOWN;
    }

    private void buildSender(Text text) {
        Uri addressUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, text.getId() + "/addr");

        // Query the address information for this message
        Cursor addr = mContext.getContentResolver().query(addressUri, null, null, null, null);
        while (addr.moveToNext()) {
            if (addr.getLong(addr.getColumnIndex(Telephony.Mms.Addr.MSG_ID)) == text.getMmsId()) {
                if (addr.getLong(addr.getColumnIndex(Telephony.Mms.Addr.TYPE)) == Text.TYPE_SENDER) {
                    text.setSenderAddress(addr.getString(addr.getColumnIndex(Telephony.Mms.Addr.ADDRESS)));
                }
                text.setMemberAddress(addr.getString(addr.getColumnIndex(Telephony.Mms.Addr.ADDRESS)));
            }
        }
        addr.close();
    }

    @Override
    @RequiresPermission(Manifest.permission.READ_SMS)
    public synchronized Future<Contact> getSender(final Text text) {
        final Contact sender = text.getSender();
        if (sender != null) {
            return new Present<>(sender);
        } else {
            return new FutureImpl<Contact>() {
                @SuppressLint("InlinedApi")
                @Override
                @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
                public Contact get() {
                    if (text.getSenderAddress() == null && text.isMms()) {
                        buildSender(text);
                    }
                    Contact sender = text.isIncoming() ? lookupContact(text.getSenderAddress()) : getSelf();
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
                        text.addMember(lookupContact(address));
                    }
                    return members;
                }
            };
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
    public synchronized Future<Set<Contact>> getMembersExceptMe(final Text text) {
        return new FutureImpl<Set<Contact>>() {
            @Override
            @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
            public Set<Contact> get() {
                Set<Contact> members = new HashSet<>(getMembers(text).get());
                if (members.size() == 1) {
                    // It's possible to text yourself. To account for that, don't remove yourself if there's only one member.
                    return members;
                }
                members.remove(getSelf());
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