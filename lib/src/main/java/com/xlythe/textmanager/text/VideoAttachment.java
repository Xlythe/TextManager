package com.xlythe.textmanager.text;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Niko on 12/30/15.
 */
public final class VideoAttachment extends Attachment {
    private VideoAttachment(Parcel in) {
        super(in);
    }

    public VideoAttachment(String uri){
        super(Type.VIDEO, uri);
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
    }

    public static final Parcelable.Creator<Attachment> CREATOR = new Parcelable.Creator<Attachment>() {
        public VideoAttachment createFromParcel(Parcel in) {
            return new VideoAttachment(in);
        }

        public VideoAttachment[] newArray(int size) {
            return new VideoAttachment[size];
        }
    };
}
