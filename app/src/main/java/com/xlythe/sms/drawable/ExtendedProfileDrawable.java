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
import android.graphics.RectF;
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
    private static final int DRAWABLE_MARGIN_SIZE_DP = 2;
    private static final int FONT_SIZE_SP = 14;

    private final Context mContext;
    private final Contact mContact;
    private final ProfileDrawable mProfileDrawable;

    private final Paint mPaint;

    private final Rect mDimens = new Rect();
    private final Rect mTextBounds = new Rect();
    private final Rect mMargins = new Rect();
    private final Rect mTextMargins = new Rect();

    public ExtendedProfileDrawable(Context context, Contact contact) {
        mContext = context;
        mContact = contact;
        mProfileDrawable = new ProfileDrawable(context, contact);

        int drawableMargins = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DRAWABLE_MARGIN_SIZE_DP, mContext.getResources().getDisplayMetrics());
        float fontSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_SP, mContext.getResources().getDisplayMetrics());

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(fontSize);
        mPaint.getTextBounds(mContact.getDisplayName(), 0, mContact.getDisplayName().length(), mTextBounds);

        // Margins for the entire drawable
        mMargins.left = mMargins.right = mMargins.top = mMargins.bottom = drawableMargins;

        // Margins for the text
        mTextMargins.left = (int) (mProfileDrawable.getIntrinsicWidth() * 0.3);
        mTextMargins.right = mProfileDrawable.getIntrinsicWidth() / 2;

        // Dimensions for the entire drawable
        mDimens.bottom = mMargins.top + mProfileDrawable.getIntrinsicHeight() + mMargins.bottom;
        mDimens.right = mMargins.left + mProfileDrawable.getIntrinsicWidth() + mTextMargins.left + mTextBounds.width() + mTextMargins.right + mMargins.right;
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) mDimens.height();
    }

    protected int getHeight() {
        return getIntrinsicHeight() - mMargins.top - mMargins.bottom;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) mDimens.width();
    }

    protected int getWidth() {
        return getIntrinsicWidth() - mMargins.left - mMargins.right;
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

    @Override
    public void draw(Canvas canvas) {
        canvas.translate(mMargins.left, mMargins.top);

        // Draw a grey oval in the background, by using 2 circles and a rectangle
        mPaint.setColor(Color.LTGRAY);
        int radius = mProfileDrawable.getIntrinsicWidth() / 2;
        canvas.drawCircle(getWidth() - radius, radius, radius, mPaint);
        canvas.drawRect(radius, 0, getWidth() - radius, getHeight(), mPaint);

        // Draw the user's profile pic in the left-hand side
        mProfileDrawable.draw(canvas);

        // Draw the user's name on the right
        String text = mContact.getDisplayName();
        mPaint.setColor(mContext.getResources().getColor(R.color.text));
        int x = mProfileDrawable.getIntrinsicWidth() + mTextMargins.left;
        int y = getHeight() / 2 + mTextBounds.height() / 2;
        canvas.drawText(text, x, y, mPaint);
    }

    public Contact getContact() {
        return mContact;
    }
}
