package com.xlythe.textmanager.text;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class VoiceAttachment extends Attachment {
    public VoiceAttachment(Uri uri){
        super(Type.VOICE, uri);
    }

    private VoiceAttachment(Parcel in) {
        super(in);
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
