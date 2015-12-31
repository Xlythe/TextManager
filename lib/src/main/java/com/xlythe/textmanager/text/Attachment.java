package com.xlythe.textmanager.text;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Niko on 12/29/15.
 */
public class Attachment implements Parcelable{
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

    public Attachment() {
    }

    private Attachment(Parcel in) {
        mType = Type.values()[in.readInt()];
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mType.ordinal());
    }

    public static final Parcelable.Creator<Attachment> CREATOR = new Parcelable.Creator<Attachment>() {
        public Attachment createFromParcel(Parcel in) {
            return new Attachment(in);
        }

        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };
}