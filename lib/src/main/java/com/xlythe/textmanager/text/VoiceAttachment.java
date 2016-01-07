package com.xlythe.textmanager.text;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Niko on 12/30/15.
 */
public class VoiceAttachment extends Attachment {
    protected VoiceAttachment(Parcel in) {
        super(in);
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
    }

    public static final Parcelable.Creator<VoiceAttachment> CREATOR = new Parcelable.Creator<VoiceAttachment>() {
        public VoiceAttachment createFromParcel(Parcel in) {
            return new VoiceAttachment(in);
        }

        public VoiceAttachment[] newArray(int size) {
            return new VoiceAttachment[size];
        }
    };
}
