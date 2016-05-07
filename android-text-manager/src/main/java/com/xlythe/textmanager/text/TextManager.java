package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.RetrieveConf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages sms and mms messages
 */
public class TextManager implements MessageManager<Text, Thread, Contact> {
    static final String TAG = TextManager.class.getSimpleName();
    static final boolean DEBUG = true;
    private static final int COLUMN_CONTENT_LOCATION = 0;
    private static final int CACHE_SIZE = 50;
    public static final String[] PROJECTION = new String[] {
            // Determine if message is SMS or MMS
            Mock.Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN,
            // Base item ID
            BaseColumns._ID,
            // Conversation (thread) ID
            Mock.Telephony.Sms.Conversations.THREAD_ID,
            // Date values
            Mock.Telephony.Sms.DATE,
            Mock.Telephony.Sms.DATE_SENT,
            // For SMS only
            Mock.Telephony.Sms.ADDRESS,
            Mock.Telephony.Sms.BODY,
            Mock.Telephony.Sms.TYPE,
            Mock.Telephony.Sms.STATUS,
            // For MMS only
            Mock.Telephony.Mms.SUBJECT,
            Mock.Telephony.Mms.MESSAGE_BOX,
            Mock.Telephony.Mms.STATUS
    };

    private static TextManager sTextManager;

    public static synchronized TextManager getInstance(Context context) {
        if (sTextManager == null) {
            sTextManager = new TextManager(context);
        }
        return sTextManager;
    }

    private Context mContext;
    private final Set<MessageObserver> mObservers = new HashSet<>();
    private final LruCache<String, Contact> mContactCache = new LruCache<>(CACHE_SIZE);

    private TextManager(Context context) {
        mContext = context;
        context.getContentResolver().registerContentObserver(Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, true, new TextObserver(new Handler()));
    }

    public void downloadAttachment(Text text){
        if (text.isMms()) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS StoreMedia");
            wl.acquire();
            final Uri uri = Uri.withAppendedPath(Mock.Telephony.Mms.Inbox.CONTENT_URI, text.getId());

            final String[] proj = new String[] {
                    Mock.Telephony.Mms.CONTENT_LOCATION
            };

            Cursor cursor = mContext.getContentResolver().query(uri, proj, null, null, null);
            String url = "";

            if (cursor != null) {
                try {
                    if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                        url = cursor.getString(COLUMN_CONTENT_LOCATION);
                    }
                } finally {
                    cursor.close();
                }
            }

            Receive.getPdu(url, mContext, new Receive.DataCallback() {
                @Override
                public void onSuccess(byte[] result) {
                    RetrieveConf retrieveConf = (RetrieveConf) new PduParser(result, true).parse();

                    PduPersister persister = PduPersister.getPduPersister(mContext);
                    Uri msgUri;
                    try {
                        msgUri = persister.persist(retrieveConf, uri, true, true, null);

                        // Use local time instead of PDU time
                        ContentValues values = new ContentValues(1);
                        values.put(Mock.Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
                        values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_COMPLETE);
                        mContext.getContentResolver().update(msgUri, values, null, null);
                    } catch (MmsException e) {
                        Log.e("MMS", "unable to persist message");
                        onFail();
                    }
                }

                @Override
                public void onFail() {
                    // this maybe useful
                }
            });
            wl.release();
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

    @Override
    public void send(final Text text) {
        Intent sendService = new Intent(mContext, SendService.class);
        sendService.putExtra(SendService.TEXT_EXTRA, text);
        mContext.startService(sendService);
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
        final String[] projection = PROJECTION;
        Uri uri = Uri.withAppendedPath(Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, threadId);
        String order = "normalized_date ASC";
        return new Text.TextCursor(mContext, contentResolver.query(uri, projection, null, null, order));
    }

    @Override
    public Future<Text> getMessage(final String messageId) {
        return new FutureImpl<Text>() {
            @Override
            public Text get() {
                String clause = String.format("%s = %s",
                        Mock.Telephony.MmsSms._ID, messageId);
                ContentResolver contentResolver = mContext.getContentResolver();
                final String[] projection = PROJECTION;
                Uri uri = Mock.Telephony.MmsSms.CONTENT_URI;
                Text.TextCursor cursor = new Text.TextCursor(mContext, contentResolver.query(uri, projection, clause, null, null));
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getText();
                    } else {
                        return null;
                    }
                } finally {
                    cursor.close();
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
        if (android.os.Build.MANUFACTURER.equals(Mock.MANUFACTURER_SAMSUNG) && android.os.Build.VERSION.SDK_INT < 19) {
            uri = Uri.parse("content://mms-sms/conversations/?simple=true");
            order = "date DESC";
        } else {
            uri = Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
            order = "normalized_date DESC";
        }
        return new Thread.ThreadCursor(contentResolver.query(uri, null, null, null, order));
    }

    public Future<Thread> getThread(long threadId) {
        return getThread(Long.toString(threadId));
    }

    @Override
    public Future<Thread> getThread(final String threadId) {
        return new FutureImpl<Thread>() {
            @Override
            public Thread get() {
                String clause = String.format("%s = %s",
                        Mock.Telephony.Sms.Conversations.THREAD_ID, threadId);
                ContentResolver contentResolver = mContext.getContentResolver();
                final Uri uri;
                final String order;
                if (android.os.Build.MANUFACTURER.equals(Mock.MANUFACTURER_SAMSUNG) && android.os.Build.VERSION.SDK_INT < 19) {
                    uri = Uri.parse("content://mms-sms/conversations/?simple=true");
                    order = "date DESC";
                } else {
                    uri = Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
                    order = "normalized_date DESC";
                }
                Thread.ThreadCursor cursor = new Thread.ThreadCursor(contentResolver.query(uri, null, clause, null, order));
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getThread();
                    } else {
                        return null;
                    }
                } finally {
                    cursor.close();
                }
            }
        };
    }

    public void delete(Collection<Text> messages) {
        delete(messages.toArray(new Text[messages.size()]));
    }

    @Override
    public void delete(Text... messages) {
        String ids = "";
        for (Text text : messages) {
            if (!ids.isEmpty()) {
                ids += ",";
            }
            ids += text.getId();
        }
        String clause = String.format("%s in (%s)",
                Mock.Telephony.Sms._ID, ids);
        int rowsDeleted = mContext.getContentResolver().delete(
                Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, clause, null);
        if (DEBUG) {
            Log.i(TAG, String.format("Deleting %s. %s rows deleted", ids, rowsDeleted));
        }
    }

    @Override
    public void delete(Thread... threads) {
        String ids = "";
        for (Thread thread : threads) {
            if (!ids.isEmpty()) {
                ids += ",";
            }
            ids += thread.getId();
        }
        String clause = String.format("%s in (%s)",
                Mock.Telephony.Sms.THREAD_ID, ids);
        int rowsDeleted = mContext.getContentResolver().delete(
                Mock.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, clause, null);
        if (DEBUG) {
            Log.i(TAG, String.format("Deleting %s. %s rows deleted", ids, rowsDeleted));
        }
    }

    @Override
    public void markAsRead(Text message) {
        ContentValues values = new ContentValues();
        values.put(Mock.Telephony.Sms.READ, true);
        String clause = String.format("%s = %s",
                Mock.Telephony.Sms._ID, message.getId());
        int rowsUpdated = mContext.getContentResolver().update(
                Mock.Telephony.Sms.Inbox.CONTENT_URI, values, clause, null);
        if (DEBUG) {
            Log.i(TAG, String.format("Marking %s as read. %s rows updated", message, rowsUpdated));
        }
    }

    @Override
    public void markAsRead(Thread thread) {
        ContentValues values = new ContentValues();
        values.put(Mock.Telephony.Sms.READ, true);
        String clause = String.format("%s=%s AND %s=%s",
                Mock.Telephony.Sms.THREAD_ID, thread.getId(),
                Mock.Telephony.Sms.READ, 0);
        int rowsUpdated = mContext.getContentResolver().update(
                Mock.Telephony.Sms.Inbox.CONTENT_URI, values, clause, null);
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

    public Contact lookupContact(String phoneNumber) {
        Contact contact = mContactCache.get(phoneNumber);
        if (contact == null) {
            Cursor c;
            if (phoneNumber.matches(".*\\d+.*")) {
                // Found a phone number (probably)
                ContentResolver contentResolver = mContext.getContentResolver();
                Uri uri = Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(phoneNumber));

                c = contentResolver.query(uri, null, null, null, null);
            } else {
                // This isn't a phone number! This is a name!
                c = getContactCursor(phoneNumber);
            }

            try {
                if (c != null && c.moveToFirst()) {
                    contact = new Contact(c);
                } else {
                    contact = new Contact(phoneNumber);

                    if (TextUtils.isEmpty(contact.getNumber(mContext).get())) {
                        contact = Contact.UNKNOWN;
                    }
                }
            } finally {
                if (c != null) c.close();
            }
            mContactCache.put(phoneNumber, contact);
        }
        return contact;
    }

    public Contact getSelf() {
        TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = manager.getLine1Number();
        if (phoneNumber == null) {
            return Contact.UNKNOWN;
        } else {
            if (phoneNumber.charAt(0) == '+' && phoneNumber.charAt(1) == '1') {
                return lookupContact(phoneNumber.substring(2));
            } else if (phoneNumber.charAt(0) == '1') {
                return lookupContact(phoneNumber.substring(1));
            } else {
                return lookupContact(phoneNumber);
            }

        }
    }

    /**
     * The default sms app. Before api 19, this returns null.
     */
    public String getDefaultSmsPackage() {
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            return Telephony.Sms.getDefaultSmsPackage(mContext);
        } else {
            return null;
        }
    }

    /**
     * Returns true if your package is the default SMS app. Before API 19, that means everyone.
     */
    public boolean isDefaultSmsPackage() {
        if (android.os.Build.VERSION.SDK_INT >= 19 && supportsSms()) {
            return Telephony.Sms.getDefaultSmsPackage(mContext).equals(mContext.getPackageName());
        } else {
            return supportsSms();
        }
    }

    public boolean supportsSms() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }
}