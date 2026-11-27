package com.xlythe.textmanager.text.mms;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.FileNotFoundException;
import java.io.OutputStream;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class MmsPduProviderTest {
    private final Context context = RuntimeEnvironment.getApplication();
    private final MmsPduProvider provider = new MmsPduProvider();

    @Before public void attachProvider() {
        ProviderInfo info = new ProviderInfo();
        info.authority = MmsPduStore.authority(context);
        provider.attachInfo(context, info);
    }

    @Test public void carrierCanWriteReservedFileButLateWriteCannotResurrectIt() throws Exception {
        Uri transfer = MmsPduStore.create(context);
        try (OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(provider.openFile(transfer, "wt"))) {
            output.write(new byte[]{1, 2, 3});
        }
        assertArrayEquals(new byte[]{1, 2, 3}, MmsPduStore.read(context, transfer));
        assertEquals(1, provider.delete(transfer, null, null));
        assertThrows(FileNotFoundException.class, () -> provider.openFile(transfer, "w"));
        assertEquals(0, provider.delete(transfer, null, null));
    }

    @Test public void rejectsAmbiguousPathsAndUnneededModes() throws Exception {
        Uri transfer = MmsPduStore.write(context, new byte[]{7});
        Uri alias = transfer.buildUpon().path("other/" + transfer.getLastPathSegment()).build();
        assertThrows(FileNotFoundException.class, () -> provider.openFile(alias, "r"));
        assertThrows(FileNotFoundException.class, () -> provider.openFile(transfer, "rw"));
        assertArrayEquals(new byte[]{7}, MmsPduStore.read(context, transfer));
    }
}
