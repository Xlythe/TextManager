package com.xlythe.textmanager.text;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Niko on 12/30/15.
 */
public class ImageAttachment extends Attachment {
    Bitmap mBitmap;

    // no default constructor in Attachment so I'm forced to set the type
    public ImageAttachment(Bitmap bitmap){
        super(Type.IMAGE);
        mBitmap = bitmap;
    }

    public Bitmap getBitmap(){
        return mBitmap;
    }

    public Drawable getDrawable(){
        return null;
    }

    private ImageAttachment(Parcel in) {
        super(in, Type.IMAGE);
        mBitmap = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeValue(mBitmap);
    }

    public static final Parcelable.Creator<Attachment> CREATOR = new Parcelable.Creator<Attachment>() {
        public ImageAttachment createFromParcel(Parcel in) {
            return new ImageAttachment(in);
        }

        public ImageAttachment[] newArray(int size) {
            return new ImageAttachment[size];
        }
    };
}
