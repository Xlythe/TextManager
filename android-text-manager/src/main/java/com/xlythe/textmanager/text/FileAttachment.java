package com.xlythe.textmanager.text;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;

/** A MIME part sent without transcoding, for example a vCard or calendar invitation. */
public final class FileAttachment extends Attachment {
    private final String mimeType;

    public FileAttachment(Uri uri, String mimeType) {
        super(Type.FILE, uri);
        if (!isMediaType(mimeType)) {
            throw new IllegalArgumentException("Expected a MIME media type");
        }
        this.mimeType = mimeType;
    }

    static boolean isMediaType(String value) {
        return value != null && value.matches("[A-Za-z0-9!#$%&^_.+~-]+/[A-Za-z0-9!#$%&^_.+~-]+");
    }

    private FileAttachment(Parcel in) {
        super(in);
        mimeType = in.readString();
    }

    @Override
    public String getMimeType(Context context) {
        return mimeType;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FileAttachment
                && super.equals(other)
                && mimeType.equals(((FileAttachment) other).mimeType);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + mimeType.hashCode();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mimeType);
    }

    public static final Creator<FileAttachment> CREATOR =
            new Creator<FileAttachment>() {
                public FileAttachment createFromParcel(Parcel in) {
                    return new FileAttachment(in);
                }

                public FileAttachment[] newArray(int size) {
                    return new FileAttachment[size];
                }
            };
}
