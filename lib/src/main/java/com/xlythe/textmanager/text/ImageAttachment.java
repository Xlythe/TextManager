package com.xlythe.textmanager.text;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Niko on 12/30/15.
 */
public class ImageAttachment extends Attachment implements Parcelable {
    Bitmap mBitmap;

    public ImageAttachment(Bitmap bitmap){
        mBitmap = bitmap;
    }

    public Bitmap getBitmap(){
        return mBitmap;
    }

    public Drawable getDrawable(){
        return null;
    }

    private ImageAttachment(Parcel in) {
        mBitmap = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
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
