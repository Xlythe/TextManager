package com.xlythe.textmanager.text;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Niko on 12/29/15.
 */
//TODO: parcelable
public abstract class Attachment implements com.xlythe.textmanager.Attachment, Serializable{
    enum Type {
        IMAGE, VIDEO, VOICE
    }

    private Type mType;
    private String mUri;

    public Type getType(){
        return mType;
    }

    public String getUri(){
        return mUri;
    }

    public Attachment(Type type) {
        mType = type;
    }

    public Attachment(Type type, String uri) {
        mUri = uri;
        mType = type;
    }

    protected Attachment(Parcel in) {
        // mType = in.read...., mUri = in.read....
    }

    public void writeToParcel(Parcel out, int flags) {
        // out.writeValue(mType); out.writeValue(mUri);
    }
}