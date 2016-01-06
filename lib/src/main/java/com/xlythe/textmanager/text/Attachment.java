package com.xlythe.textmanager.text;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Niko on 12/29/15.
 */
public abstract class Attachment implements com.xlythe.textmanager.Attachment, Parcelable{
    enum Type {
        IMAGE, VIDEO, VOICE
    }

    private Type mType;
    private Uri mUri;

    public Type getType(){
        return mType;
    }

    public Uri getUri(){
        return mUri;
    }

    public Attachment(Type type) {
        mType = type;
    }

    public Attachment(Type type, Uri uri) {
        mUri = uri;
        mType = type;
    }

    protected Attachment(Parcel in) {
        mType = Type.values()[in.readInt()];
        mUri = Uri.parse(in.readString());
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeValue(mType.ordinal());
        out.writeString(mUri.toString());
    }
}