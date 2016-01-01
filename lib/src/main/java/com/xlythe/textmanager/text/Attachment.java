package com.xlythe.textmanager.text;

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

    private Type mType;

    public Type getType(){
        return mType;
    }

    public Uri getUri(){
        return null;
    }

    public Attachment(Type type) {
        mType = type;
    }

    protected Attachment(Parcel in) {
        // mType = in.read...., mUri = in.read....
    }

    public void writeToParcel(Parcel out, int flags) {
        // out.writeValue(mType); out.writeValue(mUri);
    }
}