package com.xlythe.sms.receiver;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.pm.ShortcutInfoCompat;

import com.xlythe.sms.MessageActivity;
import com.xlythe.sms.R;
import com.xlythe.sms.util.ShortcutUtils;
import com.xlythe.textmanager.text.FileAttachment;
import com.xlythe.textmanager.text.Text;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPendingIntent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class MessageReceiverTest {
    private final Context context = RuntimeEnvironment.getApplication();

    @Test
    public void notificationTapOpensTheRightThreadWithAnImmutableIntent() {
        PendingIntent tap = MessageReceiver.createOnClickIntent(context, "42");
        ShadowPendingIntent pending = Shadow.extract(tap);
        assertTrue((pending.getFlags() & PendingIntent.FLAG_IMMUTABLE) != 0);
        Intent intent = pending.getSavedIntent();
        assertEquals(MessageActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("42", intent.getStringExtra(MessageActivity.EXTRA_THREAD_ID));
    }

    @Test
    @Config(minSdk = 25)
    public void fileOnlyMessageHasANotificationBodyAndConversationShortcut() {
        Text text = new Text.DebugBuilder().setThreadId(42).setSender("FAMILY")
                .attach(new FileAttachment(Uri.parse("content://mms/part/4"), "application/pdf"))
                .build();
        assertEquals("<i>File</i>", MessageReceiver.body(context, text));
        ShortcutInfoCompat shortcut = ShortcutUtils.createShortcutInfo(context, text);
        assertEquals("FAMILY", shortcut.getShortLabel().toString());
        assertEquals("42", shortcut.getId());
    }

    @Test
    @Config(minSdk = 25)
    public void missingConversationMembersStillGiveTheShortcutALabel() {
        Text text = new Text.DebugBuilder().setThreadId(42).build();
        assertEquals(context.getString(R.string.notification_channel_name),
                ShortcutUtils.createShortcutInfo(context, text).getShortLabel().toString());
    }
}
