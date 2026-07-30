package com.xlythe.textmanager.text;

import static org.junit.Assert.assertEquals;

import android.Manifest;
import android.content.Context;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class MmsReceiveServiceWorkTest {
    private final Context context = RuntimeEnvironment.getApplication();

    @Before
    public void grantReceivePermission() {
        ((ShadowApplication) Shadow.extract(RuntimeEnvironment.getApplication())).grantPermissions(Manifest.permission.RECEIVE_MMS);
    }

    @Test
    public void malformedNotificationFailsWithoutCrashing() {
        Data input = new Data.Builder()
                .putByteArray("notification_pdu", new byte[] {1, 2, 3})
                .build();
        MmsReceiveService worker = TestListenableWorkerBuilder
                .from(context, MmsReceiveService.class)
                .setInputData(input)
                .build();

        assertEquals(ListenableWorker.Result.failure(), worker.doWork());
    }

    @Test
    public void missingDownloadSourceFailsWithoutCrashing() {
        MmsReceiveService worker = TestListenableWorkerBuilder
                .from(context, MmsReceiveService.class)
                .build();

        assertEquals(ListenableWorker.Result.failure(), worker.doWork());
    }
}
