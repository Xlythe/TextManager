package com.xlythe.sms;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.util.TypedValue;

/**
 * Created by Niko on 12/22/15.
 */
public class ProfileDrawable extends Drawable {
    Paint mPaint;
    RectF mRect;
    Path mPath;
    Context mContext;
    char mInitial;
    int mColor;
    Bitmap mBitmap;
    float px;
    float sp;

    public ProfileDrawable(Context context, char initial, int color, Bitmap bitmap) {
        mContext = context;
        mInitial = initial;
        mColor = color;
        mBitmap = bitmap;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPath = new Path();
        mPath.setFillType(Path.FillType.EVEN_ODD);
        mRect = new RectF();

        px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, mContext.getResources().getDisplayMetrics());
        sp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, mContext.getResources().getDisplayMetrics());
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
        mPath.reset();
        mPath.addRect(bounds.left, bounds.top, bounds.right, bounds.bottom, Path.Direction.CW);
    }

    @Override
    public void draw(Canvas canvas) {
        mPaint.setColor(mColor);
        canvas.drawPath(mPath, mPaint);
        mPaint.setColor(mContext.getResources().getColor(R.color.text));
        if(mBitmap!=null){
            mPaint.setColor(Color.WHITE);
            canvas.drawBitmap(mBitmap,0,0,mPaint);
            return;
        }
        if (!(mInitial+"").equals("(")) {
            mPaint.setTextSize(sp);
            mPaint.setTextAlign(Paint.Align.CENTER);

            String text = mInitial + "";
            Rect r = new Rect();
            mPaint.getTextBounds(text, 0, text.length(), r);
            int y = (int) px/2 + (Math.abs(r.height()))/2;

            canvas.drawText(text, px/2, y, mPaint);
        } else {
            Bitmap bmp1 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_profile);
            canvas.drawBitmap(bmp1,0,0,mPaint);
        }
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
