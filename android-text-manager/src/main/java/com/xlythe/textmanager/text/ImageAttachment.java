package com.xlythe.textmanager.text;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.xlythe.textmanager.text.util.Utils;

import java.io.Serializable;

public final class ImageAttachment extends Attachment {
    Bitmap mBitmap;

    public ImageAttachment(Uri uri){
        super(Type.IMAGE, uri);
    }

    private ImageAttachment(Parcel in) {
        super(in);
        mBitmap = in.readParcelable(Bitmap.class.getClassLoader());
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof ImageAttachment) {
            ImageAttachment a = (ImageAttachment) o;
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
        return String.format("ImageAttachment{type=%s, uri=%s}",
                getType(), getUri());
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeValue(mBitmap);
    }

    public static final Parcelable.Creator<ImageAttachment> CREATOR = new Parcelable.Creator<ImageAttachment>() {
        public ImageAttachment createFromParcel(Parcel in) {
            return new ImageAttachment(in);
        }

        public ImageAttachment[] newArray(int size) {
            return new ImageAttachment[size];
        }
    };
}
