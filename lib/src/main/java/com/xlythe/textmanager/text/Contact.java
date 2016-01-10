package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

import com.xlythe.textmanager.User;
import com.xlythe.textmanager.text.util.Utils;

/**
 * Represents a phone number.
 */
public final class Contact implements User, Parcelable {

    private final String mNumber;
    private final String mDisplayName;
    private final String mPhotoUri;
    private final String mPhotoThumbUri;

    protected Contact(Cursor c) {
        // Number may not exist, so check first
        int column = c.getColumnIndex(ContactsContract.PhoneLookup.NUMBER);
        if (column != -1) {
            mNumber = c.getString(c.getColumnIndex(ContactsContract.PhoneLookup.NUMBER));
        } else {
            mNumber = "";
        }
        mDisplayName = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
        mPhotoUri = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
        mPhotoThumbUri = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
    }

    protected Contact(String address) {
        mNumber = address;
        mDisplayName = null;
        mPhotoUri = null;
        mPhotoThumbUri = null;
    }

    private Contact(Parcel in) {
        mNumber = in.readString();
        mPhotoUri = in.readString();
        mDisplayName = in.readString();
        mPhotoThumbUri = in.readString();
    }

    public String getNumber() {
        return mNumber;
    }

    public String getDisplayName() {
        return hasName() ? mDisplayName : getNumber();
    }

    public boolean hasName() {
        return mDisplayName != null;
    }

    public Bitmap getPhoto() {
        return null;
    }

    public Uri getPhotoThumbUri(){
        return mPhotoThumbUri!=null ? Uri.parse(mPhotoThumbUri) : null;
    }

    public Uri getPhotoUri(){
        return mPhotoUri!=null ? Uri.parse(mPhotoUri) : null;
    }

    public Drawable getPhotoThumbDrawable(){
        return null;
    }

    public Drawable getPhotoDrawable(){
        return null;
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
}
