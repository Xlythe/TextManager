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
import android.util.LruCache;
import android.util.TypedValue;

import com.xlythe.sms.R;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Receive;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ProfileDrawable extends Drawable {
    private static final String TAG = ProfileDrawable.class.getSimpleName();
    private static final int DRAWABLE_SIZE_DP = 40;
    private static final int FONT_SIZE_SP = 24;

    private static final LruCache<Contact, Integer> COLOR_CACHE = new LruCache<>(50);

    private final Paint mPaint;
    private final Context mContext;
    private final float mDrawableSizeInPx;
    private final float mFontSizeInSp;
    private final Bitmap[] mBitmaps;
    private final int mBitmapSize;

    private final Set<Contact> mContacts;

    public ProfileDrawable(Context context, Contact... contacts) {
        this(context, new HashSet<>(Arrays.asList(contacts)));
    }

    public ProfileDrawable(Context context, Set<Contact> contacts) {
        mContext = context;
        mContacts = contacts;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);

        mDrawableSizeInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DRAWABLE_SIZE_DP, mContext.getResources().getDisplayMetrics());
        mFontSizeInSp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_SP, mContext.getResources().getDisplayMetrics());

        // Create an array of bitmaps (min 1, max 4) that hold the profile picture of a contact
        mBitmaps = new Bitmap[Math.min(4, contacts.size())];
        Contact[] contactsArray = contacts.toArray(new Contact[contacts.size()]);
        for (int i = 0; i < mBitmaps.length; i++) {
            mBitmaps[i] = toBitmap(contactsArray[i]);
        }

        // Resize the bitmaps so they'll all fit within the confines of our drawable
        mBitmapSize = getBitmapSize((int) mDrawableSizeInPx, mBitmaps.length);
        for (int i = 0; i < mBitmaps.length; i++) {
            mBitmaps[i] = Bitmap.createScaledBitmap(mBitmaps[i], mBitmapSize, mBitmapSize, false);
        }
    }

    private int getBitmapSize(int width, int numberOfContacts) {
        double size;
        switch (numberOfContacts) {
            case 1:
                size = width;
                break;
            case 2:
                size = Math.sqrt(2) * width / (Math.sqrt(2) + 1);
                break;
            case 3:
                size = width / 2f;
                break;
            default:
                size = width / 2f;
                break;
        }
        return (int) size;
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) mDrawableSizeInPx;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) mDrawableSizeInPx;
    }

    @Override
    public void draw(Canvas canvas) {
        switch (mBitmaps.length) {
            case 0:
                // Something went wrong
                Log.w(TAG, "Attempted to draw but no contacts set");
                break;
            case 1:
                canvas.drawBitmap(mBitmaps[0], 0, 0, null);
                break;
            case 2:
                canvas.drawBitmap(mBitmaps[0], 0, 0, null);
                canvas.drawBitmap(mBitmaps[1], mDrawableSizeInPx - mBitmapSize, mDrawableSizeInPx - mBitmapSize, null);
                break;
            case 3:
                canvas.drawBitmap(mBitmaps[0], mBitmapSize / 2f, 0, null);
                canvas.drawBitmap(mBitmaps[1], 0, mBitmapSize, null);
                canvas.drawBitmap(mBitmaps[2], mBitmapSize, mBitmapSize, null);
                break;
            default:
                canvas.drawBitmap(mBitmaps[0], 0, 0, null);
                canvas.drawBitmap(mBitmaps[1], mBitmapSize, 0, null);
                canvas.drawBitmap(mBitmaps[2], 0, mBitmapSize, null);
                canvas.drawBitmap(mBitmaps[3], mBitmapSize, mBitmapSize, null);
                break;
        }

    }

    protected Bitmap toBitmap(Contact contact) {
        Bitmap profileBitmap = Bitmap.createBitmap((int) mDrawableSizeInPx, (int) mDrawableSizeInPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(profileBitmap);

        char initial = contact.getDisplayName().charAt(0);
        Uri uri = contact.getPhotoUri();
        if (uri != null) {
            drawUri(canvas, uri);
        } else if (Character.isLetter(initial)) {
            drawBackground(canvas, contact);
            drawName(canvas, initial);
        } else {
            drawBackground(canvas, contact);
            drawNumber(canvas);
        }

        return profileBitmap;
    }

    private void drawUri(Canvas canvas, Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) mDrawableSizeInPx, (int) mDrawableSizeInPx, false);
            mPaint.setColor(Color.WHITE);
            canvas.drawBitmap(clip(bitmap), 0, 0, mPaint);
        } catch (IOException e){
            Log.e(TAG, "Failed to load bitmap", e);
        }
    }

    private void drawName(Canvas canvas, char initial) {
        mPaint.setColor(mContext.getResources().getColor(R.color.text));
        mPaint.setTextSize(mFontSizeInSp);
        mPaint.setTextAlign(Paint.Align.CENTER);

        String text = Character.toString(initial);
        Rect r = new Rect();
        mPaint.getTextBounds(text, 0, 1, r);
        int y = (int) mDrawableSizeInPx / 2 + r.height() / 2;

        canvas.drawText(text, mDrawableSizeInPx / 2, y, mPaint);
    }

    private void drawNumber(Canvas canvas) {
        mPaint.setColor(mContext.getResources().getColor(R.color.text));
        Bitmap bmp1 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_profile);
        canvas.drawBitmap(bmp1, 0, 0, mPaint);
    }

    private void drawBackground(Canvas canvas, Contact contact) {
        Integer color = COLOR_CACHE.get(contact);
        if (color == null) {
            // Hasn't been queried yet
            color = ColorUtils.getColor(Receive.getOrCreateThreadId(mContext, contact.getNumber()));
            COLOR_CACHE.put(contact, color);
        }
        mPaint.setColor(color);
        canvas.drawCircle(mDrawableSizeInPx / 2, mDrawableSizeInPx / 2, mDrawableSizeInPx / 2, mPaint);
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

    protected Bitmap clip(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(bitmap.getWidth() / 2f,
                bitmap.getHeight() / 2f, bitmap.getWidth() / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public Set<Contact> getContacts() {
        return mContacts;
    }
}
