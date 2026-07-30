package com.xlythe.textmanager.text.mms;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/** Stores short-lived raw MMS PDUs for the platform MMS service. */
public final class MmsPduStore {
    static final String DIRECTORY = "textmanager-mms";

    private MmsPduStore() {}

    public static Uri write(Context context, byte[] pdu) throws IOException {
        if (pdu == null || pdu.length == 0) {
            throw new IOException("Cannot store an empty MMS PDU");
        }
        File directory = directory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create MMS cache directory");
        }
        String name = UUID.randomUUID() + ".pdu";
        File file = new File(directory, name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(pdu);
        }
        return uriFor(context, name);
    }

    public static Uri create(Context context) throws IOException {
        File directory = directory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create MMS cache directory");
        }
        String name = UUID.randomUUID() + ".pdu";
        File file = new File(directory, name);
        if (!file.createNewFile()) {
            throw new IOException("Unable to create MMS cache file");
        }
        return uriFor(context, name);
    }

    public static byte[] read(Context context, Uri uri) throws IOException {
        File file = resolve(context, uri);
        long length = file.length();
        if (length <= 0 || length > 25L * 1024L * 1024L) {
            throw new IOException("Invalid MMS PDU size: " + length);
        }
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream((int) length)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
    public static boolean delete(Context context, Uri uri) {
        try {
            return resolve(context, uri).delete();
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            return false;
        }
    }

    static File resolve(Context context, Uri uri) throws IOException {
        if (uri == null || !authority(context).equals(uri.getAuthority())) {
            throw new IllegalArgumentException("URI is not owned by the MMS PDU provider");
        }
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("/") || name.contains("\\") || !name.endsWith(".pdu")) {
            throw new IllegalArgumentException("Invalid MMS PDU path");
        }
        File directory = directory(context).getCanonicalFile();
        File file = new File(directory, name).getCanonicalFile();
        if (!directory.equals(file.getParentFile())) {
            throw new SecurityException("MMS PDU path escapes its cache directory");
        }
        return file;
    }

    static String authority(Context context) {
        return context.getPackageName() + ".textmanager.mms";
    }

    private static Uri uriFor(Context context, String name) {
        return new Uri.Builder()
                .scheme("content")
                .authority(authority(context))
                .appendPath(name)
                .build();
    }

    private static File directory(Context context) {
        return new File(context.getCacheDir(), DIRECTORY);
    }
}
