package com.xlythe.textmanager.text;

import static org.junit.Assert.*;

import android.content.*;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.telephony.SmsMessage;

import com.xlythe.textmanager.text.receiver.SmsReceiver;
import com.xlythe.textmanager.text.util.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class IncomingSmsTest {
    private final Context context = RuntimeEnvironment.getApplication();
    private final Inbox inbox = new Inbox();

    @Before
    public void setUp() {
        for (String authority : new String[] {"sms", "mms-sms"}) {
            Inbox provider = authority.equals("sms") ? inbox : new Inbox();
            ProviderInfo info = new ProviderInfo();
            info.authority = authority;
            provider.attachInfo(context, info);
            ShadowContentResolver.registerProviderInternal(authority, provider);
        }
    }

    private byte[] gsm(int coding, String payload) {
        return Utils.hexToBytes(
                "00000B916105550521F300"
                        + String.format("%02x", coding)
                        + "62117230000000"
                        + String.format("%02x", payload.length() / 2)
                        + payload);
    }

    private void deliver(byte[]... pdus) {
        Intent broadcast = new Intent("android.provider.Telephony.SMS_DELIVER");
        broadcast.putExtra("format", "3gpp").putExtra("subscription", 3).putExtra("pdus", pdus);
        new SmsReceiver().onReceive(context, broadcast);
    }

    private Intent lastBroadcast() {
        ShadowApplication app = Shadow.extract(RuntimeEnvironment.getApplication());
        List<Intent> messages = app.getBroadcastIntents();
        return messages.get(messages.size() - 1);
    }

    @Test
    public void unicodeSegmentsPersistTogetherAndRetainTheReceivingSim() {
        deliver(gsm(8, "0041000C"), gsm(8, "00E9"));
        assertEquals("A\né", inbox.saved.getAsString("body"));
        assertEquals(3, inbox.saved.getAsInteger("sub_id").intValue());
        assertEquals(42, inbox.saved.getAsLong("thread_id").longValue());
        Text result = lastBroadcast().getParcelableExtra(TextReceiver.EXTRA_TEXT);
        assertEquals(12, result.getIdAsLong());
        assertEquals(3, result.getSubscriptionId());
        assertEquals("A\né", result.getBody());
    }

    @Test
    public void flashSmsHasAnEphemeralCallbackWithoutWritingOrCreatingAThread() {
        deliver(gsm(0x18, "0041"));
        assertNull(inbox.saved);
        assertEquals(TextReceiver.ACTION_FLASH_SMS, lastBroadcast().getAction());
        Text result = lastBroadcast().getParcelableExtra(TextReceiver.EXTRA_TEXT);
        assertEquals(-1, result.getThreadIdAsLong());
        assertEquals("A", result.getBody());
    }

    @Test
    public void disappearingInboxRowDoesNotProduceANullMessageBroadcast() {
        inbox.disappear = true;
        SmsMessage sms = SmsMessage.createFromPdu(gsm(8, "0041"), "3gpp");
        assertNull(Receive.storeMessage(context, new SmsMessage[] {sms}, 0));
    }

    @Test
    public void emailThreadRequestsUseTheAddressInsideAngleBrackets() {
        assertEquals(42, Receive.getOrCreateThreadId(context, "Ada <ada@example.com>"));
        assertEquals("ada@example.com", Inbox.recipient);
    }

    public static final class Inbox extends ContentProvider {
        static String recipient;
        ContentValues saved;
        boolean disappear;

        public boolean onCreate() {
            return true;
        }

        public Cursor query(
                Uri uri, String[] projection, String clause, String[] args, String order) {
            MatrixCursor rows = new MatrixCursor(projection);
            if ("mms-sms".equals(uri.getAuthority())) {
                recipient = uri.getQueryParameter("recipient");
                rows.addRow(new Object[] {42L});
            } else if (!disappear) {
                ContentValues data = new ContentValues(saved);
                data.put("_id", 12);
                data.put("type", 1);
                data.put("status", -1);
                Object[] values = new Object[projection.length];
                for (int i = 0; i < values.length; i++) values[i] = data.get(projection[i]);
                rows.addRow(values);
            }
            return rows;
        }

        public Uri insert(Uri uri, ContentValues values) {
            saved = new ContentValues(values);
            return ContentUris.withAppendedId(uri, 12);
        }

        public int update(Uri u, ContentValues v, String w, String[] a) {
            return 0;
        }

        public int delete(Uri u, String w, String[] a) {
            return 0;
        }

        public String getType(Uri u) {
            return null;
        }
    }
}
