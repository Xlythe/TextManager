package com.xlythe.sms.drawable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;

import com.xlythe.sms.R;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Receive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

public class ExtendedProfileDrawable extends Drawable {

    public ExtendedProfileDrawable(Context context, Set<Contact> contacts) {

    }

    @Override
    public int getIntrinsicHeight() {
        return 0;
    }
    @Override
    public int getIntrinsicWidth() {
        return 0;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public void draw(Canvas canvas) {

    }
}
