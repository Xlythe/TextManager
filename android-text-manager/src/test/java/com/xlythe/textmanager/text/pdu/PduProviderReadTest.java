package com.xlythe.textmanager.text.pdu;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.telephony.SmsManager;

import androidx.work.Configuration;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.mms.MmsPduStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowSmsManager;

import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class PduProviderReadTest {
    private final Rows rows = new Rows();
    private PduPersister storage;
    private final Uri inbox = Uri.parse("content://mms/inbox/15");

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        ProviderInfo info = new ProviderInfo();
        info.authority = "mms";
        rows.attachInfo(context, info);
        ShadowContentResolver.registerProviderInternal("mms", rows);
        PduCache.getInstance().purgeAll();
        storage = PduPersister.getPduPersister(context);
    }

    @Test
    public void readReportWorkerHonorsTheOriginalRequestAndEncodesDeletedUnread() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication app = Shadow.extract(context.getApplicationContext());
        app.grantPermissions(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS);
        Configuration config =
                new Configuration.Builder().setExecutor(new SynchronousExecutor()).build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);
        MatrixCursor message =
                new MatrixCursor(
                        new String[] {
                            "_id", "msg_box", "thread_id", "ct_t", "date", "st", "sub_id"
                        });
        message.addRow(new Object[] {15, 1, 42, "application/vnd.wap.multipart.related", 42, 0, 3});
        message.moveToFirst();
        Text incoming = new Text.TextCursor(message, null).getText();
        ShadowSmsManager gateway = Shadow.extract(SmsManager.getSmsManagerForSubscriptionId(3));
        for (int request : new int[] {PduHeaders.VALUE_NO, PduHeaders.VALUE_YES}) {
            rows.report = request;
            TextManager.getInstance(context).sendMmsReadReport(incoming, true);
            if (request == PduHeaders.VALUE_NO) {
                assertNull(gateway.getLastSentMultimediaMessageParams());
            } else {
                Uri file = gateway.getLastSentMultimediaMessageParams().getContentUri();
                byte[] bytes = MmsPduStore.read(context, file);
                ReadRecInd read = (ReadRecInd) new PduParser(bytes, true).parse();
                assertEquals(
                        PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ, read.getReadStatus());
                assertArrayEquals(new byte[] {'i', 'd'}, read.getMessageId());
                assertEquals("sender", read.getTo()[0].getString());
            }
        }
    }

    @Test
    public void reloadsHeaderAddressesInlineCharsetAndStreamBackedParts() throws Exception {
        RetrieveConf mms = (RetrieveConf) storage.load(inbox);
        assertEquals("subject", mms.getSubject().getString());
        assertEquals("sender", mms.getFrom().getString());
        assertEquals("first", mms.getTo()[0].getString());
        assertEquals(42, mms.getDate());
        PduBody parts = mms.getBody();
        assertEquals(2, parts.getPartsNum());
        assertArrayEquals("café".getBytes(StandardCharsets.ISO_8859_1), parts.getPart(0).getData());
        assertArrayEquals(
                "caption.txt".getBytes(StandardCharsets.US_ASCII), parts.getPart(0).getFilename());
        assertEquals(Uri.parse("content://mms/part/63"), parts.getPart(1).getDataUri());
        assertNull(parts.getPart(1).getData());
        Uri canonical = Uri.parse("content://mms/15");
        assertSame(mms, PduCache.getInstance().get(canonical).getPdu());
        assertSame(mms, PduCache.getInstance().get(inbox).getPdu());
        rows.type = PduHeaders.MESSAGE_TYPE_SEND_REQ;
        assertTrue(storage.load(canonical) instanceof SendReq);
    }

    @Test
    public void reloadsIncomingControlRowsAndRejectsUnsupportedStorageTypes() throws Exception {
        int[] types = {0x82, 0x83, 0x85, 0x86, 0x87, 0x88};
        Class<?>[] expected = {
            NotificationInd.class,
            NotifyRespInd.class,
            AcknowledgeInd.class,
            DeliveryInd.class,
            ReadRecInd.class,
            ReadOrigInd.class
        };
        for (int i = 0; i < types.length; i++) {
            rows.type = types[i];
            assertEquals(expected[i], storage.load(inbox).getClass());
        }
        rows.type = PduHeaders.MESSAGE_TYPE_FORWARD_REQ;
        assertThrows(MmsException.class, () -> storage.load(inbox));
        assertFalse(PduCache.getInstance().isUpdating(inbox));
    }

    @Test
    public void reassignedMessageSurvivesFormerBoxAndThreadInvalidation() {
        PduCache cache = PduCache.getInstance();
        Uri uri = Uri.parse("content://mms/15");
        cache.put(inbox, new PduCacheEntry(new SendReq(), 1, 1));
        PduCacheEntry moved = new PduCacheEntry(new SendReq(), 2, 2);
        cache.put(uri, moved);
        cache.purge(Uri.parse("content://mms/inbox"));
        cache.purge(Uri.parse("content://mms-sms/conversations/1"));
        assertSame(moved, cache.get(uri));
        cache.purge(Uri.parse("content://mms/sent"));
        assertNull(cache.get(uri));
    }

    static final class Rows extends ContentProvider {
        int report = PduHeaders.VALUE_NO;
        int type = PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;

        public boolean onCreate() {
            return true;
        }

        public Cursor query(
                Uri uri, String[] projection, String where, String[] args, String order) {
            MatrixCursor rows = new MatrixCursor(projection);
            if ("part".equals(uri.getLastPathSegment())) {
                append(
                        rows,
                        projection,
                        "_id",
                        62,
                        "chset",
                        4,
                        "ct",
                        "text/plain",
                        "text",
                        "café",
                        "fn",
                        "caption.txt",
                        "cid",
                        "root",
                        "cl",
                        "caption",
                        "cd",
                        "inline",
                        "name",
                        "caption");
                append(rows, projection, "_id", 63, "ct", "audio/amr");
            } else if ("addr".equals(uri.getLastPathSegment())) {
                append(rows, projection, "type", 137, "address", "sender", "charset", 106);
                append(rows, projection, "type", 151, "address", "first", "charset", 106);
            } else {
                append(
                        rows,
                        projection,
                        "_id",
                        15,
                        "m_type",
                        type,
                        "rr",
                        report,
                        "m_id",
                        "id",
                        "v",
                        18,
                        "msg_box",
                        1,
                        "thread_id",
                        42,
                        "date",
                        42,
                        "ct_t",
                        "application/vnd.wap.multipart.related",
                        "sub",
                        "subject",
                        "sub_cs",
                        106);
            }
            return rows;
        }

        private void append(MatrixCursor rows, String[] projection, Object... fields) {
            Object[] data = new Object[projection.length];
            for (int i = 0; i < projection.length; i++)
                for (int j = 0; j < fields.length; j += 2)
                    if (projection[i].equals(fields[j])) data[i] = fields[j + 1];
            rows.addRow(data);
        }

        public String getType(Uri uri) {
            return null;
        }

        public Uri insert(Uri uri, ContentValues values) {
            throw new UnsupportedOperationException();
        }

        public int delete(Uri uri, String where, String[] args) {
            return 0;
        }

        public int update(Uri uri, ContentValues values, String where, String[] args) {
            throw new UnsupportedOperationException();
        }
    }
}
