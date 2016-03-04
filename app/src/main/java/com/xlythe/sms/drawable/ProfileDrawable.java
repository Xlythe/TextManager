package com.xlythe.sms.drawable;

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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;

import com.xlythe.sms.R;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.sms.util.DateFormatter;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Receive;
import com.xlythe.textmanager.text.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ProfileDrawable extends Drawable {
    Paint mPaint;
    Context mContext;
    float px;
    float sp;
    ArrayList<Bitmap> mBitmaps = new ArrayList<>();

    public ProfileDrawable(Context context, Set<Contact> contacts) {
        mContext = context;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);

        px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, mContext.getResources().getDisplayMetrics());
        sp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, mContext.getResources().getDisplayMetrics());

        for (Contact contact : contacts) {
            mBitmaps.add(drawableToBitmap(contact));
        }
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
        double size;
        Bitmap bmp1;
        Bitmap bmp2;
        Bitmap bmp3;
        Bitmap bmp4;
        switch (mBitmaps.size()) {
            case 1:
                size = mBitmaps.get(0).getWidth();
                bmp1 = Bitmap.createScaledBitmap(mBitmaps.get(0), (int) size, (int) size, false);
                canvas.drawBitmap(bmp1, 0, 0, null);
                break;
            case 2:
                size = Math.sqrt(2) * mBitmaps.get(0).getWidth() / (Math.sqrt(2) + 1);
                bmp1 = Bitmap.createScaledBitmap(mBitmaps.get(0), (int) size, (int) size, false);
                bmp2 = Bitmap.createScaledBitmap(mBitmaps.get(1), (int) size, (int) size, false);
                canvas.drawBitmap(bmp1, 0, 0, null);
                canvas.drawBitmap(bmp2, px - bmp1.getWidth(), px - bmp1.getHeight(), null);
                break;
            case 3:
                size = mBitmaps.get(0).getWidth() / 2;
                bmp1 = Bitmap.createScaledBitmap(mBitmaps.get(0), (int) size, (int) size, false);
                bmp2 = Bitmap.createScaledBitmap(mBitmaps.get(1), (int) size, (int) size, false);
                bmp3 = Bitmap.createScaledBitmap(mBitmaps.get(2), (int) size, (int) size, false);
                canvas.drawBitmap(bmp1, bmp1.getWidth() / 2, 0, null);
                canvas.drawBitmap(bmp2, 0, (int) size, null);
                canvas.drawBitmap(bmp3, bmp1.getWidth(), (int) size, null);
                break;
            default:
                size = mBitmaps.get(0).getWidth() / 2;
                bmp1 = Bitmap.createScaledBitmap(mBitmaps.get(0), (int) size, (int) size, false);
                bmp2 = Bitmap.createScaledBitmap(mBitmaps.get(1), (int) size, (int) size, false);
                bmp3 = Bitmap.createScaledBitmap(mBitmaps.get(2), (int) size, (int) size, false);
                bmp4 = Bitmap.createScaledBitmap(mBitmaps.get(3), (int) size, (int) size, false);
                canvas.drawBitmap(bmp1, 0, 0, null);
                canvas.drawBitmap(bmp2, bmp1.getWidth(), 0, null);
                canvas.drawBitmap(bmp3, 0, bmp1.getWidth(), null);
                canvas.drawBitmap(bmp4, bmp1.getWidth(), bmp1.getWidth(), null);
                break;
        }

    }

    public Bitmap drawableToBitmap(Contact contact) {
        Bitmap profileBitmap = Bitmap.createBitmap((int) px, (int) px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(profileBitmap);

        char initial = contact.getDisplayName().charAt(0);
        Uri uri = contact.getPhotoUri();
        int color = ColorUtils.getColor(contact.getIdAsLong());
        if (contact.getIdAsLong() < 0) {
            color = ColorUtils.getColor(Receive.getOrCreateThreadId(mContext, contact.getNumber()));
        }

        mPaint.setColor(color);
        canvas.drawCircle(px / 2, px / 2, px / 2, mPaint);
        mPaint.setColor(mContext.getResources().getColor(R.color.text));
        if (uri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
                bitmap = Bitmap.createScaledBitmap(bitmap, (int) px, (int) px, false);
                mPaint.setColor(Color.WHITE);
                canvas.drawBitmap(clip(bitmap), 0, 0, mPaint);
            } catch (IOException ioe){
                Log.d("Profile image","io");
            }
        } else if (Character.isLetter(initial)) {
            mPaint.setTextSize(sp);
            mPaint.setTextAlign(Paint.Align.CENTER);

            String text = initial + "";
            Rect r = new Rect();
            mPaint.getTextBounds(text, 0, text.length(), r);
            int y = (int) px/2 + (Math.abs(r.height()))/2;

            canvas.drawText(text, px/2, y, mPaint);
        } else {
            Bitmap bmp1 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_profile);
            canvas.drawBitmap(bmp1, 0, 0, mPaint);
        }

        return profileBitmap;
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

    public Bitmap clip(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(bitmap.getWidth() / 2,
                bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
}
