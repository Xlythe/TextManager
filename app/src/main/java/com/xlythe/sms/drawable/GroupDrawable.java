package com.xlythe.sms.drawable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;

import com.xlythe.sms.R;

import java.io.IOException;

/**
 * Created by Niko on 3/1/16.
 */
public class GroupDrawable extends Drawable {

    Context mContext;
    float px;
    Bitmap bmp1;
    Bitmap bmp2;

    public GroupDrawable(Context context, Drawable... drawables) {
        double size;
        bmp1 = drawableToBitmap(drawables[0]);
        bmp2 = drawableToBitmap(drawables[1]);

        //2
//        size = Math.sqrt(2) * bmp1.getWidth() / (Math.sqrt(2) + 1);

        //3 and 4
        size = bmp1.getWidth() / 2;

        bmp1 = Bitmap.createScaledBitmap(bmp1, (int) size, (int) size, false);
        bmp2 = Bitmap.createScaledBitmap(bmp2, (int) size, (int) size, false);

        mContext = context;
        px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, mContext.getResources().getDisplayMetrics());
    }

    @Override
    public int getIntrinsicHeight() {
        return (int)px;
    }
    @Override
    public int getIntrinsicWidth() {
        return (int)px;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
    }

    @Override
    public void draw(Canvas canvas) {
//        canvas.drawBitmap(bmp1, 0, 0, null);
//        canvas.drawBitmap(bmp2, px - bmp1.getWidth(), px - bmp1.getHeight(), null);

        double size = Math.sqrt(3) * bmp1.getHeight() / 2;
        canvas.drawBitmap(bmp2, bmp1.getWidth() / 2, 0, null);
        canvas.drawBitmap(bmp1, 0, (int) size, null);
        canvas.drawBitmap(bmp2, bmp1.getWidth(), (int) size, null);

//        canvas.drawBitmap(bmp1, 0, 0, null);
//        canvas.drawBitmap(bmp2, bmp1.getWidth(), 0, null);
//        canvas.drawBitmap(bmp1, 0, bmp1.getWidth(), null);
//        canvas.drawBitmap(bmp2, bmp1.getWidth(), bmp1.getWidth(), null);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
