package com.xlythe.textmanager.text.mms;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class MmsPduStoreTest {
    private final Context context = RuntimeEnvironment.getApplication();

    @Test
    public void writeCreatesPrivatePduWithExpectedBytes() throws Exception {
        byte[] pdu = new byte[] {1, 2, 3, 4};

        Uri uri = MmsPduStore.write(context, pdu);

        assertTrue(uri.toString().startsWith("content://" + context.getPackageName()));
        assertArrayEquals(pdu, Files.readAllBytes(MmsPduStore.resolve(context, uri).toPath()));
        assertArrayEquals(pdu, MmsPduStore.read(context, uri));
    }

    @Test
    public void createProducesWritableEmptyPdu() throws Exception {
        Uri uri = MmsPduStore.create(context);

        assertTrue(MmsPduStore.resolve(context, uri).isFile());
        assertArrayEquals(new byte[0], Files.readAllBytes(MmsPduStore.resolve(context, uri).toPath()));
    }

    @Test
    public void deleteOnlyDeletesOwnedPdus() throws Exception {
        Uri uri = MmsPduStore.write(context, new byte[] {7});

        assertTrue(MmsPduStore.delete(context, uri));
        assertFalse(MmsPduStore.resolve(context, uri).exists());
        assertFalse(MmsPduStore.delete(context, Uri.parse("content://other/not-ours.pdu")));
    }

    @Test
    public void simultaneousDownloadsHaveIndependentFilesAndCleanup() throws Exception {
        ExecutorService io = Executors.newFixedThreadPool(2);
        try {
            Future<Uri> first = io.submit(() -> MmsPduStore.write(context, new byte[]{1}));
            Future<Uri> second = io.submit(() -> MmsPduStore.write(context, new byte[]{2}));
            Uri firstUri = first.get(5, TimeUnit.SECONDS);
            Uri secondUri = second.get(5, TimeUnit.SECONDS);
            assertFalse(firstUri.equals(secondUri));
            assertArrayEquals(new byte[]{1}, MmsPduStore.read(context, firstUri));
            assertTrue(MmsPduStore.delete(context, firstUri));
            assertArrayEquals(new byte[]{2}, MmsPduStore.read(context, secondUri));
        } finally {
            io.shutdownNow();
        }
    }

    @Test
    public void rejectsEmptyPayload() throws Exception {
        try {
            MmsPduStore.write(context, new byte[0]);
            fail("Expected empty PDU to be rejected");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("empty"));
        }
    }
}
