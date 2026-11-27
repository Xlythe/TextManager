package com.xlythe.textmanager.text;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.robolectric.shadow.api.Shadow;

import android.content.ContentResolver;
import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowContentResolver.UpdateStatement;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class MessageStoreTest {
    private final TextManager manager = TextManager.getInstance(RuntimeEnvironment.getApplication());
    private final ContentResolver resolver = RuntimeEnvironment.getApplication().getContentResolver();
    private final ShadowContentResolver provider = Shadow.extract(resolver);

    private static Text message(boolean mms) {
        return new Text(4, 42, 1, 4, 0, "hello", true, mms, "+15555550100",
                Collections.emptySet(), null);
    }

    @Test public void smsAndMmsWithMatchingIdsUpdateOnlyTheirOwnInbox() {
        manager.markAsRead(message(true));
        manager.markAsRead(message(false));
        List<UpdateStatement> updates = provider.getUpdateStatements();
        assertEquals(2, updates.size());
        assertRead(updates.get(0), "mms", "_id", "4");
        assertRead(updates.get(1), "sms", "_id", "4");
    }

    @Test public void markingConversationReadIncludesBothInboxes() {
        manager.markAsRead(new Thread(42, 2, 2, message(true)));
        List<UpdateStatement> updates = provider.getUpdateStatements();
        assertEquals(2, updates.size());
        assertRead(updates.get(0), "sms", "thread_id", "42");
        assertRead(updates.get(1), "mms", "thread_id", "42");
    }

    private static void assertRead(UpdateStatement update, String authority, String key, String id) {
        assertEquals(Uri.parse("content://" + authority + "/inbox"), update.getUri());
        assertEquals(key + " = ? AND read = 0", update.getWhere());
        assertArrayEquals(new String[]{id}, update.getSelectionArgs());
        assertEquals(Integer.valueOf(1), update.getContentValues().getAsInteger("read"));
    }

    @Test public void deleteDistinguishesMessagesFromConversations() {
        manager.delete(message(true), message(false));
        manager.delete(new Thread(42, 2, 0, message(false)));
        assertEquals(3, provider.getDeleteStatements().size());
        assertEquals("content://mms/4", provider.getDeleteStatements().get(0).getUri().toString());
        assertEquals("content://sms/4", provider.getDeleteStatements().get(1).getUri().toString());
        assertEquals("content://mms-sms/conversations/42", provider.getDeleteStatements().get(2).getUri().toString());
    }
}
