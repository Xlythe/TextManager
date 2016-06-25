package com.xlythe.textmanager.text;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.xlythe.textmanager.User;
import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;
import com.xlythe.textmanager.text.util.Utils;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a phone number.
 */
public final class Contact implements User, Parcelable {
    private static final String TAG = Contact.class.getSimpleName();
    private static final boolean DEBUG = false;

    static final Contact UNKNOWN = new Contact(null, "???");

    private final long mId;
    private String mNumber;
    private final String mDisplayName;
    private final String mPhotoUri;
    private final String mPhotoThumbUri;

    protected Contact(Cursor c) {
        if (DEBUG) {
            Log.d(TAG, "--------Printing Columns-------");
            for (int i = 0; i < c.getColumnCount(); i++) {
                Log.d(TAG, "Column: " + c.getColumnName(i));
            }
        }

        mId = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
        // Number may not exist, so check first
        int column = c.getColumnIndex(ContactsContract.PhoneLookup.NUMBER);
        if (column != -1) {
            setNumber(mNumber);
        }
        mDisplayName = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
        mPhotoUri = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
        mPhotoThumbUri = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
    }

    protected Contact(String phoneNumber, Cursor c) {
        mId = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
        // Number may not exist, so check first
        setNumber(phoneNumber);
        mDisplayName = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
        mPhotoUri = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
        mPhotoThumbUri = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
    }

    protected Contact(String number) {
        this(number, (String) null);
    }

    protected Contact(String number, String displayName) {
        mId = -1;
        setNumber(number);
        mDisplayName = displayName;
        mPhotoUri = null;
        mPhotoThumbUri = null;
    }

    private Contact(Parcel in) {
        mId = in.readLong();
        mNumber = in.readString();
        mPhotoUri = in.readString();
        mDisplayName = in.readString();
        mPhotoThumbUri = in.readString();
    }

    public String getId() {
        return Long.toString(mId);
    }

    public long getIdAsLong() {
        return mId;
    }

    public synchronized Future<String> getMobileNumber(final Context context) {
        if (mNumber != null) {
            return new Present<>(mNumber);
        } else {
            return new FutureImpl<String>() {
                @Override
                public String get() {
                    List<String> numbers = new LinkedList<>();

                    Uri uri = Phone.CONTENT_URI;
                    String[] projection = new String[] {
                            Phone.NUMBER,
                            Phone.IS_PRIMARY,
                            Phone.TYPE
                    };
                    String clause = Phone.CONTACT_ID + " = ? AND " + Phone.TYPE + " = ?";
                    String[] args = new String[] { getId(), Integer.toString(Phone.TYPE_MOBILE) };
                    Cursor cursor = context.getContentResolver().query(uri, projection, clause, args, null);
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                do {
                                    boolean isPrimary = cursor.getInt(cursor.getColumnIndex(Phone.IS_PRIMARY)) != 0;
                                    String number = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));

                                    if (isPrimary) {
                                        numbers.add(0, number);
                                    } else {
                                        numbers.add(number);
                                    }
                                } while (cursor.moveToNext());
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                    if (numbers.isEmpty()) {
                        return null;
                    } else {
                        return setNumber(numbers.get(0));
                    }
                }
            };
        }
    }

    public String getNumber() {
        return mNumber;
    }

    private synchronized String setNumber(String number) {
        mNumber = number;
        return mNumber;
    }

    public List<String> getEmails(Context context) {
        List<String> emailAddresses = new LinkedList<>();

        Uri uri = Email.CONTENT_URI;
        String[] projection = new String[] {
                Email.DATA,
                Email.IS_PRIMARY
        };
        String clause = Email.CONTACT_ID + " = ?";
        String[] args = new String[] { getId() };
        Cursor cursor = context.getContentResolver().query(uri, projection, clause, args, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        boolean isPrimary = cursor.getInt(cursor.getColumnIndex(Email.IS_PRIMARY)) != 0;
                        String address = cursor.getString(cursor.getColumnIndex(Email.DATA));

                        if (isPrimary) {
                            emailAddresses.add(0, address);
                        } else {
                            emailAddresses.add(address);
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        return emailAddresses;
    }

    public List<String> getNumbers(Context context) {
        List<String> phoneNumbers = new LinkedList<>();

        Uri uri = Phone.CONTENT_URI;
        String[] projection = new String[] {
                Phone.NUMBER,
                Phone.IS_PRIMARY
        };
        String clause = Phone.CONTACT_ID + " = ?";
        String[] args = new String[] { getId() };
        Cursor cursor = context.getContentResolver().query(uri, projection, clause, args, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        boolean isPrimary = cursor.getInt(cursor.getColumnIndex(Phone.IS_PRIMARY)) != 0;
                        String number = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));

                        if (isPrimary) {
                            phoneNumbers.add(0, number);
                        } else {
                            phoneNumbers.add(number);
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        return phoneNumbers;
    }

    @Override
    public String getDisplayName() {
        if (hasName()) {
            return mDisplayName;
        } else {
            // Guaranteed to have a number if the name is null.
            return PhoneNumberUtils.formatNumber(getNumber());
        }
    }

    public boolean hasName() {
        return mDisplayName != null;
    }

    @Override
    public Uri getPhotoUri() {
        return mPhotoUri != null ? Uri.parse(mPhotoUri) : null;
    }

    public Uri getThumbnailUri() {
        return mPhotoThumbUri != null ? Uri.parse(mPhotoThumbUri) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Contact) {
            Contact a = (Contact) o;
            return Utils.equals(mNumber, a.mNumber)
                    && Utils.equals(mPhotoUri, a.mPhotoUri)
                    && Utils.equals(mDisplayName, a.mDisplayName)
                    && Utils.equals(mPhotoThumbUri, a.mPhotoThumbUri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(mNumber)
                + Utils.hashCode(mPhotoUri)
                + Utils.hashCode(mDisplayName)
                + Utils.hashCode(mPhotoThumbUri);
    }

    @Override
    public String toString() {
        return String.format("Contact{number=%s, photo_uri=%s, display_name=%s, photo_thumb_uri=%s}",
                mNumber, mPhotoUri, mDisplayName, mPhotoThumbUri);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mId);
        out.writeString(mNumber);
        out.writeString(mPhotoUri);
        out.writeString(mDisplayName);
        out.writeString(mPhotoThumbUri);
    }

    public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    public static class ContactCursor extends CursorWrapper {
        public ContactCursor(Cursor cursor) {
            super(cursor);
        }

        public Contact getContact() {
            return new Contact(this);
        }
    }

    public enum Sort {
        Alphabetical(ContactsContract.Contacts.DISPLAY_NAME + " ASC"),
        FrequentlyContacted(ContactsContract.Contacts.TIMES_CONTACTED + " ASC");
        private final String key;

        Sort(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
