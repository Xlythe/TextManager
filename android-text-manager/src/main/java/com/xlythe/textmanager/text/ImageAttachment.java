package com.xlythe.textmanager.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;
import com.xlythe.textmanager.text.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.xlythe.textmanager.text.TextManager.TAG;

public final class ImageAttachment extends Attachment {

    private transient Bitmap mBitmap;

    private static Bitmap toBitmap(Drawable drawable) {
        int width = Math.max(1, drawable.getIntrinsicWidth());
        int height = Math.max(1, drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Rect bounds = new Rect(drawable.getBounds());
        try {
            drawable.setBounds(0, 0, width, height);
            drawable.draw(new Canvas(bitmap));
        } finally {
            drawable.setBounds(bounds);
        }
        return bitmap;
    }

    private static Uri persist(Context context, String name, Bitmap image, Type type) {
        boolean png = type == Type.HIGH_RES_IMAGE;
        try {
            File file = File.createTempFile("mms-image-", png ? ".png" : ".jpg", context.getCacheDir());
            try (FileOutputStream out = new FileOutputStream(file)) {
                if (!image.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 100, out)) {
                    throw new IOException("Bitmap encoder failed");
                }
            } catch (IOException failed) {
                file.delete();
                throw failed;
            }
            return Uri.fromFile(file);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store MMS image", e);
        }
    }

    public ImageAttachment(Uri uri, Type type){
        super(type, uri);
    }

    public ImageAttachment(Context context, String name, Bitmap image, Type type) {
        super(type, persist(context, name, image, type));
        mBitmap = image;
    }

    public ImageAttachment(Context context, String name, Drawable image, Type type) {
        this(context, name, toBitmap(image), type);
    }

    public ImageAttachment(Uri uri){
        super(Type.IMAGE, uri);
    }

    public ImageAttachment(Context context, String name, Bitmap image) {
        super(Type.IMAGE, persist(context, name, image, Type.IMAGE));
        mBitmap = image;
    }

    public ImageAttachment(Context context, String name, Drawable image) {
        this(context, name, toBitmap(image));
    }

    private ImageAttachment(Parcel in) {
        super(in);
    }

    public synchronized Future<Bitmap> getBitmap(final Context context) {
        Log.d(TAG, "Getting bitmap");
        if (mBitmap != null) {
            return new Present<>(mBitmap);
        } else {
            return new FutureImpl<Bitmap>() {
                @Override
                public Bitmap get() {
                    try (InputStream input = asStream(context)) {
                        setBitmap(BitmapFactory.decodeStream(input));
                    } catch (IOException e) {
                        Log.w(TAG, "Unable to decode MMS image", e);
                    }
                    return mBitmap;
                }
            };
        }
    }

    @Nullable
    public byte[] getBytes(final Context context) {
        Bitmap bitmap = getBitmap(context).get();
        if (bitmap == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Bitmap.CompressFormat format = getType() == Type.HIGH_RES_IMAGE ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
        if (!bitmap.compress(format, 100, stream)) return null;
        return stream.toByteArray();
    }

    private synchronized void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ImageAttachment) {
            ImageAttachment a = (ImageAttachment) o;
            return Utils.equals(getType(), a.getType())
                    && Utils.equals(getUri(), a.getUri());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(getType())
                + Utils.hashCode(getUri());
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("ImageAttachment{type=%s, uri=%s}",
                getType(), getUri());
    }

    public static final Parcelable.Creator<ImageAttachment> CREATOR = new Parcelable.Creator<ImageAttachment>() {
        public ImageAttachment createFromParcel(Parcel in) {
            return new ImageAttachment(in);
        }

        public ImageAttachment[] newArray(int size) {
            return new ImageAttachment[size];
        }
    };
}
