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

    // This reads in the Bitmap, I make sure to set the type
    private ImageAttachment(Parcel in) {
        super(Type.IMAGE);
        mBitmap = in.readParcelable(Bitmap.class.getClassLoader());
        // mType = in.read...., mUri = in.read.... (read comments in creator)
    }

    public int describeContents() {
        return 0;
    }

    // This parcels the bitmap
    public void writeToParcel(Parcel out, int flags) {
        out.writeValue(mBitmap);
    }

    // The CREATOR needs to be Attachment so that I can parcel all attachments
    // ----------------------------------------v--------------------------------------------v
    public static final Parcelable.Creator<Attachment> CREATOR = new Parcelable.Creator<Attachment>() {
        // but this needs to be ImageAttachment so I can parcel the Bitmap
        // This doesnt parcel the type and uri though, which I could do(see above) but is that right?
        // either that or do I add this stuff in the attachment class?
        // The main question I geuss is do I need to make it certain that mType and Uri HAS to be parceled?
        // and if I do how do I do that?
        public ImageAttachment createFromParcel(Parcel in) {
            return new ImageAttachment(in);
        }

        public ImageAttachment[] newArray(int size) {
            return new ImageAttachment[size];
        }
    };
}
