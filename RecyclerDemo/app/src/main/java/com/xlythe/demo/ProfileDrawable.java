package com.xlythe.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Created by Niko on 12/22/15.
 */
public class ProfileDrawable extends Drawable {
    Paint mPaint;
    RectF mRect;
    Path mPath;
    Context mContext;

    public ProfileDrawable(Context context) {
        mContext = context;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);

        mPath = new Path();
        mPath.setFillType(Path.FillType.EVEN_ODD);

        mRect = new RectF();
    }

    @Override
    public int getIntrinsicHeight() {
        return 160;
    }
    @Override
    public int getIntrinsicWidth() {
        return 160;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mPath.reset();
        mPath.addRect(bounds.left, bounds.top, bounds.right, bounds.bottom, Path.Direction.CW);
    }

    @Override
    public void draw(Canvas canvas) {
        mPaint.setColor(mContext.getResources().getColor(R.color.icon));
        canvas.drawPath(mPath, mPaint);
        mPaint.setColor(mContext.getResources().getColor(R.color.text));
        mPaint.setTextSize(96);
        mPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("W", 80, 115, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
