package com.xlythe.textmanager.text;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Niko on 12/30/15.
 */
public class VoiceAttachment extends Attachment {
    private VoiceAttachment(Parcel in) {
        super(in);
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
    }

    public static final Parcelable.Creator<Attachment> CREATOR = new Parcelable.Creator<Attachment>() {
        public VoiceAttachment createFromParcel(Parcel in) {
            return new VoiceAttachment(in);
        }

        public VideoAttachment[] newArray(int size) {
            return new VideoAttachment[size];
        }
    };
}
