package com.xlythe.textmanager.text;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.xlythe.textmanager.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a phone number.
 */
public class TextUser implements User {

    private String mAddress;
    private String mName;

    protected TextUser(Text text, Context context) {
        mAddress = text.getAddress();
        mName = getName(mAddress, context);
    }

    protected TextUser(TextThread textThread, Context context) {
        mAddress = textThread.getAddress();
        mName = getName(mAddress, context);
    }

    public String getName() {
        return hasName() ? mName : mAddress;
    }

    public boolean hasName() {
        return !mName.equals("");
    }

    public Uri getImageThumbnailUri(){
        return null;
    }

    public Uri getImageUri(){
        return null;
    }

    public Drawable getImageThumbnailDrawable(){
        return null;
    }

    public Drawable getImageDrawable(){
        return null;
    }

    private String getName(String number, Context context) {
        Uri uri;
        String[] projection;

        if (android.os.Build.VERSION.SDK_INT >= 5) {
            uri = Uri.parse("content://com.android.contacts/phone_lookup");
            projection = new String[] { "display_name" };
        }
        else {
            uri = Uri.parse("content://contacts/phones/filter");
            projection = new String[] { "name" };
        }

        uri = Uri.withAppendedPath(uri, Uri.encode(number));
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        String contactName = "";

        if (cursor.moveToFirst()) {
            contactName = cursor.getString(0);
        }
        cursor.close();

        return contactName;
    }

}
