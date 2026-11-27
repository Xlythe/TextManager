package com.xlythe.textmanager.text;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.net.Uri;
import android.telephony.SmsMessage;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import com.xlythe.textmanager.text.pdu.*;
import com.xlythe.textmanager.text.sms.SmsStatusTracker;
import com.xlythe.textmanager.text.util.Utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowSmsManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class CarrierMessagingTest {
    private final Context context = RuntimeEnvironment.getApplication();

    @Test
    public void groupMmsRoundTripsAudioVcardCaptionSubjectAndRequests() throws Exception {
        File audio = new File(context.getCacheDir(), "clip.amr");
        File card = new File(context.getCacheDir(), "contact.vcf");
        byte[] recording = "#!AMR\n".getBytes(StandardCharsets.US_ASCII);
        byte[] vcard =
                "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Ada\r\nEND:VCARD\r\n"
                        .getBytes(StandardCharsets.UTF_8);
        Files.write(audio.toPath(), recording);
        Files.write(card.toPath(), vcard);
        Text original =
                new Text.Builder()
                        .addRecipients(new Contact("+16505550100"), new Contact("+16505550101"))
                        .message("Καλημέρα")
                        .attach(new VoiceAttachment(Uri.fromFile(audio)))
                        .addAttachment(new FileAttachment(Uri.fromFile(card), "text/vcard"))
                        .subscriptionId(2)
                        .build();
        Text queued = Text.fromBytes(original.toBytes());
        assertEquals(original, queued);
        assertEquals(2, queued.getAttachments().size());
        queued.getSendRequest(context, "旅行", true, true);
        SendReq decoded = (SendReq) new PduParser(queued.getByteData(), true).parse();
        assertEquals(2, decoded.getTo().length);
        assertEquals("旅行", decoded.getSubject().getString());
        assertEquals(PduHeaders.VALUE_YES, decoded.getReadReport());
        assertEquals(PduHeaders.VALUE_YES, decoded.getDeliveryReport());
        PduBody body = decoded.getBody();
        assertEquals(4, body.getPartsNum());
        assertTrue(
                new String(body.getPart(0).getData(), StandardCharsets.UTF_8).contains("<audio"));
        assertArrayEquals(recording, body.getPart(1).getData());
        assertEquals(
                "text/vcard",
                new String(body.getPart(2).getContentType(), StandardCharsets.US_ASCII));
        assertArrayEquals(vcard, body.getPart(2).getData());
        assertEquals("Καλημέρα", new String(body.getPart(3).getData(), StandardCharsets.UTF_8));
    }

    @Test
    public void inboxAndThreadPreviewsRetainAllMediaAndTextParts() {
        MatrixCursor parts = new MatrixCursor(new String[] {"_id", "ct", "text"});
        parts.addRow(new Object[] {1, "application/smil", null});
        parts.addRow(new Object[] {2, "text/plain", "one"});
        parts.addRow(new Object[] {3, "audio/amr", null});
        parts.addRow(new Object[] {4, "image/png", null});
        parts.addRow(new Object[] {5, "text/vcard", null});
        parts.addRow(new Object[] {6, "text/plain", "two"});
        Text text = new Text.Builder().build();
        while (parts.moveToNext()) text.readPart(parts);
        assertEquals("one\ntwo", text.getBody());
        assertEquals(3, text.getAttachments().size());
        assertEquals(Uri.parse("content://mms/part/3"), text.getAttachments().get(0).getUri());
        assertEquals(Attachment.Type.FILE, text.getAttachment().getType());
        assertThrows(UnsupportedOperationException.class, () -> text.getAttachments().clear());
    }

    @Test
    public void dataSmsPreservesAllBytesAndUnsignedPortOnSelectedSim() {
        TextManager manager = TextManager.getInstance(context);
        byte[] bytes = {0, 127, (byte) 255};
        manager.sendDataSms("+14155550100", 62000, bytes, 3, null, null);
        ShadowSmsManager sms = Shadow.extract(SendService.resolveSmsManager(3));
        assertEquals(62000, sms.getLastSentDataMessageParams().getDestinationPort() & 0xffff);
        assertArrayEquals(bytes, sms.getLastSentDataMessageParams().getData());
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.sendDataSms("1", 65536, bytes, 3, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.sendDataSms("1", 0, new byte[0], 3, null, null));
    }

    @Test
    public void gsmAndCdmaStatusReportsDistinguishPendingAndFailure() {
        byte[] pdu = Utils.hexToBytes("0002010B915155050501F1000000000000000000000000000000");
        for (int status : new int[] {0, 0x20, 0x40, 0x60}) {
            pdu[pdu.length - 1] = (byte) status;
            SmsMessage message = SmsMessage.createFromPdu(pdu, "3gpp");
            assertTrue(message.isStatusReportMessage());
            assertEquals(status, message.getStatus());
            SmsStatusTracker.Status expected =
                    status == 0
                            ? SmsStatusTracker.Status.COMPLETE
                            : status == 0x20
                                    ? SmsStatusTracker.Status.PENDING
                                    : SmsStatusTracker.Status.FAILED;
            assertEquals(expected, SendService.deliveryStatus(message.getStatus(), false));
        }
        assertEquals(SmsStatusTracker.Status.COMPLETE, SendService.deliveryStatus(2 << 16, true));
        assertEquals(SmsStatusTracker.Status.PENDING, SendService.deliveryStatus(0x02010000, true));
        assertEquals(SmsStatusTracker.Status.FAILED, SendService.deliveryStatus(0x03050000, true));
    }

    @Test
    public void wapPushReportsAreDeliveredToTheAppWithoutDownloading() {
        ShadowApplication app = Shadow.extract(RuntimeEnvironment.getApplication());
        app.grantPermissions(Manifest.permission.RECEIVE_MMS);
        String[] reports = {
            "8C868D928B69640085010197313233009581",
            "8C888D928B6964008501019731323300890580343536009B80"
        };
        for (String report : reports) {
            Data input =
                    new Data.Builder()
                            .putByteArray("notification_pdu", Utils.hexToBytes(report))
                            .putInt("subscription_id", 3)
                            .build();
            assertEquals(
                    ListenableWorker.Result.success(),
                    TestListenableWorkerBuilder.from(context, MmsReceiveService.class)
                            .setInputData(input)
                            .build()
                            .doWork());
            List<Intent> intents = app.getBroadcastIntents();
            Intent intent = intents.get(intents.size() - 1);
            assertEquals(context.getPackageName(), intent.getPackage());
            assertEquals(TextReceiver.ACTION_MMS_REPORT, intent.getAction());
            assertEquals("id", intent.getStringExtra("message_id"));
            assertEquals(3, intent.getIntExtra("subscription_id", -1));
            boolean delivery = report.equals(reports[0]);
            assertArrayEquals(
                    new String[] {delivery ? "123" : "456"},
                    intent.getStringArrayExtra("addresses"));
            assertEquals(delivery ? 0x81 : 0x80, intent.getIntExtra("status", 0));
        }
    }
}
