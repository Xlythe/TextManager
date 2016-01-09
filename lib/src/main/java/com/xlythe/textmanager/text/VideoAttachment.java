package com.xlythe.textmanager.text;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class VideoAttachment extends Attachment {
    public VideoAttachment(Uri uri){
        super(Type.VIDEO, uri);
    }

    private VideoAttachment(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<VideoAttachment> CREATOR = new Parcelable.Creator<VideoAttachment>() {
        public VideoAttachment createFromParcel(Parcel in) {
            return new VideoAttachment(in);
        }

        public VideoAttachment[] newArray(int size) {
            return new VideoAttachment[size];
        }
    };
}
