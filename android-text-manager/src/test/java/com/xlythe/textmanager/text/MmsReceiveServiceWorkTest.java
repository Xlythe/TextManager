package com.xlythe.textmanager.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.xlythe.textmanager.text.mms.MmsPduStore;

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
import org.robolectric.shadows.ShadowContentResolver;

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

    @Test
    public void failedCarrierCallbackUpdatesOnlyItsInboxRowAndReleasesItsPdu() throws Exception {
        Uri failed = MmsPduStore.create(context);
        Uri pending = MmsPduStore.write(context, new byte[]{7});
        Uri row = Uri.parse("content://mms/inbox/42");

        assertEquals(ListenableWorker.Result.failure(), complete(failed, row, Activity.RESULT_CANCELED));
        ShadowContentResolver provider = Shadow.extract(context.getContentResolver());
        assertEquals(1, provider.getUpdateStatements().size());
        ShadowContentResolver.UpdateStatement update = provider.getUpdateStatements().get(0);
        assertEquals(row, update.getUri());
        assertEquals(Integer.valueOf(Mock.Telephony.Sms.Sent.STATUS_FAILED), update.getContentValues().getAsInteger(Mock.Telephony.Mms.STATUS));
        assertTrue(update.getContentValues().getAsLong(Mock.Telephony.Mms.DATE) > 0);
        assertFalse(MmsPduStore.delete(context, failed));
        assertEquals(7, MmsPduStore.read(context, pending)[0]);
    }

    @Test
    public void successfulCarrierResultWithMalformedPduFailsAndCleansUp() throws Exception {
        Uri invalid = MmsPduStore.write(context, new byte[]{1, 2, 3});
        assertEquals(ListenableWorker.Result.failure(), complete(invalid,
                Uri.parse("content://mms/inbox/43"), Activity.RESULT_OK));
        assertFalse(MmsPduStore.delete(context, invalid));
    }

    private ListenableWorker.Result complete(Uri pdu, Uri row, int result) {
        Data input = new Data.Builder().putString("raw_pdu_uri", pdu.toString())
                .putString("message_uri", row.toString()).putInt("result_code", result).build();
        return TestListenableWorkerBuilder.from(context, MmsReceiveService.class)
                .setInputData(input).build().doWork();
    }
}
