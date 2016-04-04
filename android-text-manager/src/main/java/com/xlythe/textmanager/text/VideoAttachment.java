package com.xlythe.textmanager.text;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
                    byte[] videoBytes = null;
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        FileInputStream fis = new FileInputStream(new File(getRealPath()));

                        byte[] buf = new byte[1024];
                        int n;
                        while (-1 != (n = fis.read(buf)))
                            baos.write(buf, 0, n);

                        videoBytes = baos.toByteArray();
                        setBytes(videoBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return videoBytes;
                }

                private String getRealPath() {
                    Cursor cursor = null;
                    try {
                        String[] proj = { MediaStore.Images.Media.DATA };
                        cursor = context.getContentResolver().query(getUri(),  proj, null, null, null);
                        if (cursor == null) {
                            return getUri().getPath();
                        }
                        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        cursor.moveToFirst();
                        return cursor.getString(column_index);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            };
        }
    }

    private synchronized void setBytes(byte[] bytes) {
        mBytes = bytes;
    }
}
