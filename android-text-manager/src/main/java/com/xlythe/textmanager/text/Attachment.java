package com.xlythe.textmanager.text;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xlythe.textmanager.text.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.xlythe.textmanager.text.TextManager.DEBUG;
import static com.xlythe.textmanager.text.TextManager.TAG;

public abstract class Attachment implements com.xlythe.textmanager.Attachment, Parcelable{
    public enum Type {
        IMAGE, VIDEO, VOICE, HIGH_RES_IMAGE
    }

    private final Type mType;
    private final Uri mUri;

    public Type getType(){
        return mType;
    }

    public Uri getUri(){
        return mUri;
    }

    public Attachment(Type type) {
        mUri = Uri.EMPTY;
        mType = type;
    }

    public Attachment(Type type, Uri uri) {
        mType = type;
        mUri = uri;
    }

    protected Attachment(Parcel in) {
        mType = Type.valueOf(in.readString());

        String uri = in.readString();
        if (uri == null) {
            mUri = Uri.EMPTY;
        } else {
            mUri = Uri.parse(uri);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mType.name());
        out.writeString(mUri.toString());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof Attachment) {
            Attachment a = (Attachment) o;
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
        return String.format("Attachment{type=%s, uri=%s}", getType(), getUri());
    }

    public InputStream asStream(Context context) throws IOException {
        Uri uri = getUri();

        if (DEBUG) {
            Log.d(TAG, "getInputStream(): " + uri);
        }

        if (uri.getScheme() == null) return new FileInputStream(uri.getPath());
        InputStream stream = context.getContentResolver().openInputStream(uri);
        if (stream == null) throw new IOException("Attachment provider is unavailable");
        return stream;
    }

    protected static byte[] toBytes(File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            return toBytes(stream);
        }
    }

    protected static byte[] toBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        do {
            bos.write(buffer, 0, bytesRead);
            bytesRead = is.read(buffer);
        } while (bytesRead != -1);
        return bos.toByteArray();
    }
}