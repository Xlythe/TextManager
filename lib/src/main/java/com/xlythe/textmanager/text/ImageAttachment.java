package com.xlythe.textmanager.text;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * Created by Niko on 12/30/15.
 */
public class ImageAttachment extends Attachment {
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
}
