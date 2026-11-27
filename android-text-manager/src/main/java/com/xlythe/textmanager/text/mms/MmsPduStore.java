package com.xlythe.textmanager.text.mms;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Stores short-lived raw MMS PDUs for the platform MMS service. */
public final class MmsPduStore {
    static final int MAX_PDU_BYTES = 25 * 1024 * 1024;
    static final String DIRECTORY = "textmanager-mms";

    private MmsPduStore() {}

    public static Uri write(Context context, byte[] pdu) throws IOException {
        if (pdu == null || pdu.length == 0) {
            throw new IOException("Cannot store an empty MMS PDU");
        }
        if (pdu.length > MAX_PDU_BYTES) throw new IOException("Oversized MMS PDU");
        Uri uri = create(context);
        try (FileOutputStream output = new FileOutputStream(resolve(context, uri))) {
            output.write(pdu);
        } catch (IOException e) {
            delete(context, uri);
            throw e;
        }
        return uri;
    }

    public static Uri create(Context context) throws IOException {
        File directory = directory(context);
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
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
        if (length <= 0 || length > MAX_PDU_BYTES) {
            throw new IOException("Invalid MMS PDU size: " + length);
        }
        try (FileInputStream input = new FileInputStream(file)) {
            return readBounded(input, MAX_PDU_BYTES);
        }
    }

    static byte[] readBounded(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (count > limit - output.size()) throw new IOException("Oversized MMS response");
            output.write(buffer, 0, count);
        }
        if (output.size() == 0) throw new IOException("Empty MMS response");
        return output.toByteArray();
    }

    public static boolean delete(Context context, Uri uri) {
        try {
            return resolve(context, uri).delete();
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            return false;
        }
    }

    static File resolve(Context context, Uri uri) throws IOException {
        if (uri == null || !"content".equals(uri.getScheme()) || !authority(context).equals(uri.getAuthority())
                || uri.getPathSegments().size() != 1) {
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
