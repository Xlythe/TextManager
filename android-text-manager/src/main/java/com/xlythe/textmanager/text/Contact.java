package com.xlythe.textmanager.text;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Person;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.IconCompat;

import com.xlythe.textmanager.User;
import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;
import com.xlythe.textmanager.text.util.Utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.List;

/**
 * Represents a phone number.
 */
public final class Contact implements User, Parcelable {

    static final Contact UNKNOWN = new Contact(null, "???");

    private final long mId;
    private final String mLookupKey;
    private final String mNumber;
    private final String mDisplayName;
    private final String mPhotoUri;
    private final String mPhotoThumbUri;

    Contact(Cursor c) {
        this(null, c);
    }

    Contact(String address, Cursor c) {
        int contactId = c.getColumnIndex(Phone.CONTACT_ID);
        mId = c.getLong(contactId >= 0 ? contactId : c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
        mLookupKey = column(c, ContactsContract.Contacts.LOOKUP_KEY);
        String rowNumber = column(c, ContactsContract.PhoneLookup.NUMBER);
        mNumber = address != null ? address : rowNumber != null ? rowNumber : column(c, Phone.NUMBER);
        mDisplayName = column(c, ContactsContract.Contacts.DISPLAY_NAME);
        mPhotoUri = column(c, ContactsContract.Contacts.PHOTO_URI);
        mPhotoThumbUri = column(c, ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
    }

    private static String column(Cursor cursor, String name) {
        int index = cursor.getColumnIndex(name);
        return index < 0 ? null : cursor.getString(index);
    }

    Contact(String number) {
        this(number, (String) null);
    }

    Contact(String number, String displayName) {
        this(number, displayName, null);
    }

    Contact(String number, String displayName, Uri simRecord) {
        mId = -1;
        mLookupKey = simRecord == null ? "" : simRecord.toString();
        mNumber = number;
        mDisplayName = displayName;
        mPhotoUri = null;
        mPhotoThumbUri = null;
    }

    private Contact(Parcel in) {
        mId = in.readLong();
        mLookupKey = in.readString();
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

    public String getLookupKey() {
        return mLookupKey;
    }

    @SuppressLint("Range")
    public synchronized Future<String> getMobileNumber(final Context context) {
        if (mNumber != null) return new Present<>(mNumber);
        return new FutureImpl<String>() {
            @Override public String get() {
                List<String> mobile = values(context, Phone.CONTENT_URI, Phone.NUMBER, true);
                if (mobile.isEmpty()) mobile = getNumbers(context);
                return mobile.isEmpty() ? null : mobile.get(0);
            }
        };
    }

    public String getNumber() { return mNumber; }

    public List<String> getEmails(Context context) {
        return values(context, Email.CONTENT_URI, Email.ADDRESS, false);
    }

    public List<String> getNumbers(Context context) {
        if (mId < 0 && mNumber != null) return Collections.singletonList(mNumber);
        return values(context, Phone.CONTENT_URI, Phone.NUMBER, false);
    }

    private List<String> values(Context context, Uri uri, String value, boolean mobileOnly) {
        if (mId < 0 || !TelephonyPermissions.has(context, Manifest.permission.READ_CONTACTS)) {
            return Collections.emptyList();
        }
        String selection = Phone.CONTACT_ID + "=?";
        if (mobileOnly) selection += " AND " + Phone.TYPE + "=" + Phone.TYPE_MOBILE;
        String order = Phone.IS_SUPER_PRIMARY + " DESC, " + Phone.IS_PRIMARY + " DESC, " + Phone._ID + " ASC";
        LinkedHashSet<String> result = new LinkedHashSet<>();
        try (Cursor rows = context.getContentResolver().query(uri, new String[]{value}, selection,
                new String[]{getId()}, order)) {
            while (rows != null && rows.moveToNext()) {
                String entry = rows.getString(0);
                if (entry != null && !entry.trim().isEmpty()) result.add(entry);
            }
        } catch (SecurityException revoked) {
            return Collections.emptyList();
        }
        return new ArrayList<>(result);
    }

    public Uri getSimRecordUri() {
        return mId < 0 && mLookupKey != null && mLookupKey.startsWith("content://")
                ? Uri.parse(mLookupKey) : null;
    }

    private String personUri() {
        if (getSimRecordUri() != null) return mLookupKey;
        Uri uri = mId < 0 ? null : ContactsContract.Contacts.getLookupUri(mId, mLookupKey);
        if (uri == null && mNumber != null) uri = Uri.fromParts(mNumber.contains("@") ? "mailto" : "tel", mNumber, null);
        return uri == null ? null : uri.toString();
    }

    @Override
    public String getDisplayName() {
        if (hasName()) {
            return mDisplayName;
        } else {
            // Guaranteed to have a number if the name is null.
            if (mNumber == null) return UNKNOWN.mDisplayName;
            if (!PhoneNumberUtils.isGlobalPhoneNumber(mNumber)) return mNumber;
            String formatted = PhoneNumberUtils.formatNumber(mNumber);
            return formatted == null || formatted.isEmpty() ? mNumber : formatted;
        }
    }

    public boolean hasName() {
        return mDisplayName != null && !mDisplayName.isEmpty();
    }

    @Override
    public Uri getPhotoUri() {
        return mPhotoUri != null ? Uri.parse(mPhotoUri) : null;
    }

    public Uri getThumbnailUri() {
        return mPhotoThumbUri != null ? Uri.parse(mPhotoThumbUri) : null;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public Person asPerson() {
        return new Person.Builder()
                .setKey(mId < 0 ? personUri() : getId())
                .setUri(personUri())
                .setName(getDisplayName())
                .setIcon(getPhotoUri() != null ? Icon.createWithContentUri(getPhotoUri()) : null)
                .build();
    }

    public androidx.core.app.Person asPersonCompat() {
        return new androidx.core.app.Person.Builder()
                .setKey(mId < 0 ? personUri() : getId())
                .setUri(personUri())
                .setName(getDisplayName())
                .setIcon(getPhotoUri() != null ? IconCompat.createWithContentUri(getPhotoUri()) : null)
                .build();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof Contact) {
            Contact a = (Contact) o;
            return mId == a.mId && Utils.equals(mLookupKey, a.mLookupKey) && Utils.equals(mNumber, a.mNumber)
                    && Utils.equals(mPhotoUri, a.mPhotoUri)
                    && Utils.equals(mDisplayName, a.mDisplayName)
                    && Utils.equals(mPhotoThumbUri, a.mPhotoThumbUri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(mId).hashCode() + Utils.hashCode(mLookupKey) + Utils.hashCode(mNumber)
                + Utils.hashCode(mPhotoUri)
                + Utils.hashCode(mDisplayName)
                + Utils.hashCode(mPhotoThumbUri);
    }

    @NonNull
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
        out.writeString(mLookupKey);
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
        FrequentlyContacted(ContactsContract.Contacts.TIMES_CONTACTED + " DESC");
        private final String key;

        Sort(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
