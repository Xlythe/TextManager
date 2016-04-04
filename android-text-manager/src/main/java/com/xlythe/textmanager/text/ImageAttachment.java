package com.xlythe.textmanager.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.concurrency.FutureImpl;
import com.xlythe.textmanager.text.concurrency.Present;
import com.xlythe.textmanager.text.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.xlythe.textmanager.text.TextManager.TAG;

public final class ImageAttachment extends Attachment {
    private transient Bitmap mBitmap;

    private static Bitmap toBitmap(Drawable drawable) {
        Log.d(TAG, "Drawing drawable to bitmap with width=" + drawable.getIntrinsicWidth() + ", height=" + drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        return bitmap;
    }

    private static Uri persist(Context context, String name, Bitmap image) {
        Log.d(TAG, "Persisting bitmap \"" + name + ".png\" to cache");
        File file = new File(context.getCacheDir(), name + ".png");

        try {
            FileOutputStream out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Uri.fromFile(file);
    }

    public ImageAttachment(Uri uri){
        super(Type.IMAGE, uri);
    }

    public ImageAttachment(Context context, String name, Bitmap image) {
        super(Type.IMAGE, persist(context, name, image));
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
                    try {
                        setBitmap(MediaStore.Images.Media.getBitmap(context.getContentResolver(), getUri()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return mBitmap;
                }
            };
        }
    }

    private synchronized void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof ImageAttachment) {
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
