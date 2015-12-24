package com.xlythe.demo;

import android.graphics.Bitmap;

/**
 * Created by Niko on 12/23/15.
 */
public class Thread {
    //TODO: Didnt feel like making getters, dont judge...
    String mSender;
    String mMessagesPeek;
    String mTimeStamp;
    Bitmap mAttachment;
    int mUnreadCount;
    int mColor;
    Bitmap mDrawable = null;

    Thread(String sender, String peek, String time, Bitmap attach, int unread, int color){
        mSender = sender;
        mMessagesPeek = peek;
        mTimeStamp = time;
        mAttachment = attach;
        mUnreadCount = unread;
        mColor = color;
    }
}
