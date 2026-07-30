package com.xlythe.textmanager.text.mms;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Non-exported provider used to share raw MMS PDUs with Android's MMS service. */
public final class MmsPduProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        if (getContext() == null) {
            throw new FileNotFoundException("Provider is not attached");
        }
        final File file;
        try {
            file = MmsPduStore.resolve(getContext(), uri);
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            throw new FileNotFoundException(e.getMessage());
        }
        int flags;
        if ("r".equals(mode)) {
            flags = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            flags = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new FileNotFoundException("Unsupported mode: " + mode);
        }
        return ParcelFileDescriptor.open(file, flags);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "application/vnd.wap.mms-message";
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("MMS PDU files are created internally");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        return getContext() != null && MmsPduStore.delete(getContext(), uri) ? 1 : 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
            @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("MMS PDU files cannot be updated");
    }
}
