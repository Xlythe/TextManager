package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.xlythe.textmanager.User;

/**
 * Represents a phone number.
 */
public class Contact implements User {

    private String mAddress;
    private String mId;
    private String mType;
    private String mTimesContacted;
    private String mNumber;
    private String mPhotoUri;
    private String mSendToVoicemail;
    private String mLookup;
    private String mDisplayName;
    private String mLastTimeContacted;
    private String mHasPhoneNumber;
    private String mInVisibleGroup;
    private String mPhotoFileId;
    private String mLabel;
    private String mStarred;
    private String mNormalizedNumber;
    private String mPhotoThumbUri;
    private String mPhotoId;
    private String mInDefaultDirectory;
    private String mCustomRingtone;

    protected Contact(Cursor c, String address) {
        mAddress = address;
        if(c.moveToFirst()) {
            mId = c.getString(c.getColumnIndex("_id"));
            mType = c.getString(c.getColumnIndex("type"));
            mTimesContacted = c.getString(c.getColumnIndex("times_contacted"));
            mNumber = c.getString(c.getColumnIndex("number"));
            mPhotoUri = c.getString(c.getColumnIndex("photo_uri"));
            mSendToVoicemail = c.getString(c.getColumnIndex("send_to_voicemail"));
            mLookup = c.getString(c.getColumnIndex("lookup"));
            mDisplayName = c.getString(c.getColumnIndex("display_name"));
            mLastTimeContacted = c.getString(c.getColumnIndex("last_time_contacted"));
            mHasPhoneNumber = c.getString(c.getColumnIndex("has_phone_number"));
            mInVisibleGroup = c.getString(c.getColumnIndex("in_visible_group"));
            mPhotoFileId = c.getString(c.getColumnIndex("photo_file_id"));
            mLabel = c.getString(c.getColumnIndex("label"));
            mStarred = c.getString(c.getColumnIndex("starred"));
            mNormalizedNumber = c.getString(c.getColumnIndex("normalized_number"));
            mPhotoThumbUri = c.getString(c.getColumnIndex("photo_thumb_uri"));
            mPhotoId = c.getString(c.getColumnIndex("photo_id"));
            mInDefaultDirectory = c.getString(c.getColumnIndex("in_default_directory"));
            mCustomRingtone = c.getString(c.getColumnIndex("custom_ringtone"));
        }
        c.close();
    }

    //TODO: add more methods from the contacts app

    public String getNumber() {
        return mNumber!=null ? mNumber : mAddress;
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
}
