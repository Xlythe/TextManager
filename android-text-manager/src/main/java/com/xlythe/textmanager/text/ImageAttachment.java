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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.annotation.Nullable;

import static com.xlythe.textmanager.text.TextManager.TAG;

public final class ImageAttachment extends Attachment {
    private static final String SCHEME_FILE = "file";

    private transient Bitmap mBitmap;

    private static Bitmap toBitmap(Drawable drawable) {
        Log.d(TAG, "Drawing drawable to bitmap with width=" + drawable.getIntrinsicWidth() + ", height=" + drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(0, 0, 0, 0);
        drawable.draw(canvas);

        return bitmap;
    }

    private static Uri persist(Context context, String name, Bitmap image, Type type) {
        Log.d(TAG, "Persisting bitmap \"" + name + ".jpg\" to cache");
        File file = new File(context.getCacheDir(), name + ".jpg");
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (type == Type.HIGH_RES_IMAGE) {
                image.compress(Bitmap.CompressFormat.PNG, 100, out);
            } else {
                image.compress(Bitmap.CompressFormat.JPEG, 100, out);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(file);
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

    @Nullable
    public byte[] getBytes(final Context context) {
        if (SCHEME_FILE.equals(getUri().getScheme())) {
            try {
                return toBytes(new File(getUri().getPath()));
            } catch (IOException e) {
                Log.e(TAG, "Failed to read file", e);
            }
        }

        if (getBitmap(context).get() == null) {
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (getType() == Type.HIGH_RES_IMAGE) {
            getBitmap(context).get().compress(Bitmap.CompressFormat.PNG, 100, stream);
        } else {
            getBitmap(context).get().compress(Bitmap.CompressFormat.JPEG, 100, stream);
        }
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
