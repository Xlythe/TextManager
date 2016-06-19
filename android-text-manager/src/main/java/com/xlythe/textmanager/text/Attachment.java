package com.xlythe.textmanager.text;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.xlythe.textmanager.text.util.Utils;

public abstract class Attachment implements com.xlythe.textmanager.Attachment, Parcelable{
    public enum Type {
        IMAGE, VIDEO, VOICE, HIGH_RES;
    }

    private final Type mType;
    private final Uri mUri;

    public Type getType(){
        return mType;
    }

    public Uri getUri(){
        return mUri;
    }

    public Attachment(Type type) {
        mUri = Uri.EMPTY;
        mType = type;
    }

    public Attachment(Type type, Uri uri) {
        mType = type;
        mUri = uri;
    }

    protected Attachment(Parcel in) {
        mType = Type.valueOf(in.readString());

        String uri = in.readString();
        if (uri == null) {
            mUri = Uri.EMPTY;
        } else {
            mUri = Uri.parse(uri);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mType.name());
        out.writeString(mUri.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Attachment) {
            Attachment a = (Attachment) o;
            return Utils.equals(getType(), a.getType())
                    && Utils.equals(getUri(), a.getUri());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(getType())
                + Utils.hashCode(getUri());
    }

    @Override
    public String toString() {
        return String.format("Attachment{type=%s, uri=%s}", getType(), getUri());
    }
}