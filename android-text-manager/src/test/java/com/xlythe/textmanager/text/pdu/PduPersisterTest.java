package com.xlythe.textmanager.text.pdu;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class PduPersisterTest {
    private final PartsProvider provider = new PartsProvider();
    private PduPersister persister;

    @Before public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        ProviderInfo info = new ProviderInfo();
        info.authority = "mms";
        provider.attachInfo(context, info);
        ShadowContentResolver.registerProviderInternal("mms", provider);
        persister = PduPersister.getPduPersister(context);
    }

    @Test public void overlappingSavesAttachOnlyTheirOwnParts() throws Exception {
        provider.beforeMessageInsert = new CyclicBarrier(2);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Uri> first = workers.submit(() -> save("first"));
            Future<Uri> second = workers.submit(() -> save("second"));
            assertEquals("first", provider.messages.get(ContentUris.parseId(first.get(5, TimeUnit.SECONDS))));
            assertEquals("second", provider.messages.get(ContentUris.parseId(second.get(5, TimeUnit.SECONDS))));
            assertTrue(provider.staged.isEmpty());
        } finally {
            workers.shutdownNow();
        }
    }

    @Test public void failedMessageInsertRemovesStagedParts() {
        provider.rejectInsert = true;
        assertThrows(IllegalStateException.class, () -> save("temporary"));
        assertTrue(provider.staged.isEmpty());
    }

    private Uri save(String text) throws Exception {
        SendReq request = new SendReq();
        PduPart part = new PduPart();
        part.setContentType("text/plain".getBytes(StandardCharsets.US_ASCII));
        part.setData(text.getBytes(StandardCharsets.UTF_8));
        PduBody body = new PduBody();
        body.addPart(part);
        request.setBody(body);
        return persister.persist(request, Telephony.Mms.Outbox.CONTENT_URI, false, false, null);
    }

    private static final class PartsProvider extends ContentProvider {
        final Map<Long, String> staged = new HashMap<>();
        final Map<Long, String> messages = new HashMap<>();
        final Map<Long, Long> parents = new HashMap<>();
        long nextId;
        CyclicBarrier beforeMessageInsert;
        boolean rejectInsert;

        @Override public boolean onCreate() { return true; }

        @Override public Uri insert(Uri uri, ContentValues values) {
            if (uri.equals(Telephony.Mms.Outbox.CONTENT_URI)) {
                if (rejectInsert) throw new IllegalStateException("simulated storage failure");
                try {
                    beforeMessageInsert.await(5, TimeUnit.SECONDS);
                } catch (Exception failed) {
                    throw new AssertionError(failed);
                }
            }
            synchronized (this) {
                long id = ++nextId;
                if ("part".equals(uri.getLastPathSegment())) {
                    parents.put(id, Long.parseLong(uri.getPathSegments().get(0)));
                    return ContentUris.withAppendedId(Uri.parse("content://mms/part"), id);
                }
                return ContentUris.withAppendedId(uri, id);
            }
        }

        @Override public synchronized int update(Uri uri, ContentValues values, String where, String[] args) {
            if ("part".equals(uri.getPathSegments().get(0))) {
                staged.put(parents.get(ContentUris.parseId(uri)), values.getAsString("text"));
            } else {
                messages.put(values.getAsLong("mid"), staged.remove(Long.parseLong(uri.getPathSegments().get(0))));
            }
            return 1;
        }

        @Override public synchronized int delete(Uri uri, String where, String[] args) {
            return staged.remove(Long.parseLong(uri.getPathSegments().get(0))) == null ? 0 : 1;
        }
        @Override public String getType(Uri uri) { return null; }
        @Override public Cursor query(Uri uri, String[] columns, String where, String[] args, String order) {
            throw new UnsupportedOperationException();
        }
    }
}
