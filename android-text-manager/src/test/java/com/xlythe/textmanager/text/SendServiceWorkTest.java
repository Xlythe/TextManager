package com.xlythe.textmanager.text;

import android.text.TextUtils;
import java.io.File;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import android.Manifest;
import android.content.Context;
import android.telephony.SmsManager;

import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowSmsManager;

import com.xlythe.textmanager.text.pdu.SendReq;
import com.xlythe.textmanager.text.pdu.PduBody;
import com.xlythe.textmanager.text.pdu.PduPart;
import com.xlythe.textmanager.text.pdu.PduHeaders;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.mms.MmsPduStore;
import com.xlythe.textmanager.text.util.EncodedStringValue;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class SendServiceWorkTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        ((ShadowApplication) Shadow.extract(RuntimeEnvironment.getApplication())).grantPermissions(Manifest.permission.SEND_SMS);
        Configuration configuration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration);
    }

    @Test
    public void structuredMmsPreservesBlindAndCarbonCopyRecipientsAndOptionalHeaders() throws Exception {
        TextManager manager = TextManager.getInstance(context);
        SendReq request = new SendReq();
        assertThrows(IllegalArgumentException.class, () -> manager.sendMms(request, 7));
        request.addCc(new EncodedStringValue("+12025550100"));
        request.addBcc(new EncodedStringValue("private@example.com"));
        request.setPriority(PduHeaders.PRIORITY_HIGH);
        request.setExpiry(3600);
        PduBody body = new PduBody();
        PduPart part = new PduPart();
        part.setName(new byte[]{'a'});
        part.setContentType("text/plain".getBytes(StandardCharsets.US_ASCII));
        part.setData("hello".getBytes(StandardCharsets.UTF_8));
        body.addPart(part);
        request.setBody(body);
        manager.sendMms(request, 7);
        assertEquals(WorkInfo.State.SUCCEEDED, WorkManager.getInstance(context)
                .getWorkInfosByTag(SendService.WORK_TAG).get(5, TimeUnit.SECONDS).get(0).getState());
        ShadowSmsManager gateway = Shadow.extract(SendService.resolveSmsManager(7));
        byte[] wire = MmsPduStore.read(context, gateway.getLastSentMultimediaMessageParams().getContentUri());
        SendReq actual = (SendReq) new PduParser(wire,true).parse();
        assertNull(actual.getTo());
        assertEquals("+12025550100", actual.getCc()[0].getString());
        assertEquals("private@example.com", actual.getBcc()[0].getString());
        assertEquals(PduHeaders.PRIORITY_HIGH, actual.getPriority());
        assertTrue(actual.getExpiry() >= System.currentTimeMillis()/1000 + 3598);
    }

    @Test
    public void longUnicodeSmsSurvivesWorkManagerLimitAndDeletesItsFile() throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 6000; i++) body.append('雪');
        TextManager.getInstance(context).send(body.toString()).usingSubscription(7).to("+16505550100");
        List<WorkInfo> work = WorkManager.getInstance(context).getWorkInfosByTag(SendService.WORK_TAG)
                .get(5, TimeUnit.SECONDS);
        assertEquals(WorkInfo.State.SUCCEEDED, work.get(0).getState());
        ShadowSmsManager gateway = Shadow.extract(SendService.resolveSmsManager(7));
        assertEquals(body.toString(), TextUtils.join("", gateway.getLastSentMultipartTextMessageParams().getParts()));
        assertFalse(new File(context.getNoBackupFilesDir(), "textmanager-queued/" + work.get(0).getId()).exists());
    }

    @Test
    public void legacySendRequiresSuccessfulConfirmationForItsOwnTransaction() throws Exception {
        SendReq request = new SendReq();
        request.setTransactionId(new byte[]{'a'});
        byte[] confirmation = {(byte) 0x8c, (byte) 0x81, (byte) 0x98, 'a', 0,
                (byte) 0x8d, (byte) 0x92, (byte) 0x92, (byte) 0x80, (byte) 0x8b, '1', 0};
        SendService.verifyConfirmation(request.getTransactionId(), confirmation);
        confirmation[8] = (byte) 0x81;
        assertThrows(IOException.class, () -> SendService.verifyConfirmation(request.getTransactionId(), confirmation));
        confirmation[8] = (byte) 0x80;
        confirmation[3] = 'b';
        assertThrows(IOException.class, () -> SendService.verifyConfirmation(request.getTransactionId(), confirmation));
        assertThrows(IOException.class, () -> SendService.verifyConfirmation(request.getTransactionId(), new byte[0]));
    }

    @Test
    public void scheduleExecutesPersistentSendWork() throws Exception {
        Text text = new Text.Builder()
                .subscriptionId(7)
                .addRecipient(new Contact("+15551234567"))
                .message("hello")
                .build();

        SendService.schedule(context, text);

        List<WorkInfo> work = WorkManager.getInstance(context)
                .getWorkInfosByTag(SendService.WORK_TAG)
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, work.size());
        assertEquals(WorkInfo.State.SUCCEEDED, work.get(0).getState());
        ShadowSmsManager gateway = Shadow.extract(SendService.resolveSmsManager(7));
        assertEquals("hello", gateway.getLastSentTextMessageParams().getText());
        assertEquals("+15551234567", gateway.getLastSentTextMessageParams().getDestinationAddress());
        assertTrue(work.get(0).getTags().contains(SendService.class.getName()));
    }

    @Test
    public void missingRecipientFailsTheWorker() throws Exception {
        SendService.schedule(context, new Text.Builder().message("no recipient").build());
        List<WorkInfo> work = WorkManager.getInstance(context).getWorkInfosByTag(SendService.WORK_TAG)
                .get(5, TimeUnit.SECONDS);
        assertEquals(WorkInfo.State.FAILED, work.get(0).getState());
    }

    @Test
    public void explicitSubscriptionSelectsMatchingSmsManager() {
        SmsManager smsManager = SendService.resolveSmsManager(7);

        assertEquals(7, smsManager.getSubscriptionId());
    }
}