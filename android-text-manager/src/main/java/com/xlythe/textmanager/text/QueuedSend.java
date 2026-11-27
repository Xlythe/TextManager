package com.xlythe.textmanager.text;

import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Stores messages that exceed WorkManager's input limit under the corresponding work ID. */
final class QueuedSend {

    private static File directory(Context context) {
        return new File(ContextCompat.getNoBackupFilesDir(context), "textmanager-queued");
    }

    static void write(Context context, UUID id, byte[] text, String subject) throws IOException {
        File directory = directory(context);
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Unable to create message queue");
        }
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeByteArray(text);
            parcel.writeString(subject);
            try (FileOutputStream out = new FileOutputStream(new File(directory, id.toString()))) {
                out.write(parcel.marshall());
                out.getFD().sync();
            }
        } catch (IOException failure) {
            delete(context, id);
            throw failure;
        } finally {
            parcel.recycle();
        }
    }

    final byte[] text;
    final String subject;

    private QueuedSend(byte[] text, String subject) {
        this.text = text;
        this.subject = subject;
    }

    static QueuedSend read(Context context, UUID id) throws IOException {
        File file = new File(directory(context), id.toString());
        long length = file.length();
        if (length <= 0 || length > Integer.MAX_VALUE)
            throw new IOException("Missing or invalid queued message");
        byte[] bytes = new byte[(int) length];
        try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
            input.readFully(bytes);
        }
        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            return new QueuedSend(parcel.createByteArray(), parcel.readString());
        } finally {
            parcel.recycle();
        }
    }

    static void delete(Context context, UUID id) {
        File file = new File(directory(context), id.toString());
        if (!file.delete() && file.exists())
            Log.w("TextManager", "Could not clean up queued message");
    }

    static void prune(Context context, WorkManager manager) {
        File[] files = directory(context).listFiles();
        if (files == null) return;
        // Give a writer in another process time to finish enqueueing its request.
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        for (File file : files) {
            if (file.lastModified() >= cutoff) continue;
            UUID id = UUID.fromString(file.getName());
            ListenableFuture<WorkInfo> state = manager.getWorkInfoById(id);
            state.addListener(
                    () -> {
                        try {
                            WorkInfo info = state.get();
                            if (info == null || info.getState().isFinished()) delete(context, id);
                        } catch (ExecutionException e) {
                            Log.w("TextManager", "Unable to inspect queued work", e);
                        } catch (InterruptedException e) {
                            java.lang.Thread.currentThread().interrupt();
                        }
                    },
                    Runnable::run);
        }
    }
}
