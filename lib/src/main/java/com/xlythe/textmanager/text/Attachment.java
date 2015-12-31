package com.xlythe.textmanager.text;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Niko on 12/29/15.
 */
public abstract class Attachment implements com.xlythe.textmanager.Attachment, Parcelable{
    enum Type {
        IMAGE, VIDEO, VOICE
    }

    Type mType;

    Type getType(){
        return mType;
    }

    Uri getUri(){
        return null;
    }

    public Attachment(Type type) {
        mType = type;
    }

    public Attachment(Parcel in, Type type) {
        // mType = in.read...., mUri = in.read....
    }

    public void writeToParcel(Parcel out, int flags) {
        // out.writeValue(mType); out.writeValue(mUri);
    }
}