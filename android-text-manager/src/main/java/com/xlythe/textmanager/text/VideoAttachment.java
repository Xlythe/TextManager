package com.xlythe.textmanager.text;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;

import java.io.IOException;

import static com.xlythe.textmanager.text.TextManager.TAG;

public final class VideoAttachment extends Attachment {
    private transient byte[] mBytes;

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

    public synchronized Future<byte[]> getBytes(final Context context) {
        if (mBytes != null) {
            return new Present<>(mBytes);
        } else {
            return new FutureImpl<byte[]>() {
                @Override
                public byte[] get() {
                    try {
                        mBytes = toBytes(asStream(context));
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to decode " + getUri() + " as bytes");
                    }
                    return mBytes;
                }
            };
        }
    }

    private synchronized void setBytes(byte[] bytes) {
        mBytes = bytes;
    }
}
