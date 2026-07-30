package com.xlythe.textmanager.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.database.MatrixCursor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class CursorIterationTest {
    @Test
    public void firstUnreadRowIsCounted() {
        MatrixCursor threads = new MatrixCursor(new String[] {Thread.THREAD_ID});
        threads.addRow(new Object[] {42L});
        threads.moveToFirst();
        MatrixCursor unread = new MatrixCursor(new String[] {Thread.THREAD_ID});
        unread.addRow(new Object[] {42L});

        Thread.ThreadCursor cursor =
                new Thread.ThreadCursor(threads, unread, Collections.emptyList());

        assertEquals(1, cursor.getThread().getUnreadCount());
    }

    @Test
    public void textCursorClosesWithoutMmsCursor() {
        MatrixCursor messages = new MatrixCursor(new String[] {"_id"});
        Text.TextCursor cursor = new Text.TextCursor(messages, null);

        cursor.close();

        assertEquals(true, messages.isClosed());
    }

    @Test
    public void mmsPartSelectionHandlesEmptyAndMultipleMessageSets() {
        assertNull(TextManager.buildMmsPartSelection(0));
        assertEquals("mid = ?", TextManager.buildMmsPartSelection(1));
        assertEquals("mid = ? OR mid = ? OR mid = ?", TextManager.buildMmsPartSelection(3));
    }

    @Test
    public void smsCursorPreservesSubscriptionId() {
        MatrixCursor messages = new MatrixCursor(new String[] {
                Mock.Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN,
                "_id",
                Mock.Telephony.Sms.Conversations.THREAD_ID,
                Mock.Telephony.Sms.Conversations.DATE,
                Mock.Telephony.Sms.ADDRESS,
                Mock.Telephony.Sms.BODY,
                Mock.Telephony.Sms.TYPE,
                Mock.Telephony.Sms.STATUS,
                Mock.Telephony.Sms.SUBSCRIPTION_ID
        });
        messages.addRow(new Object[] {"sms", 1L, 2L, 3L, "+15555550100", "body",
                Mock.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX, 0, 7});
        messages.moveToFirst();

        Text text = new Text(messages, null);

        assertEquals(7, text.getSubscriptionId());
    }

    @Test
    public void mmsCursorPreservesSubscriptionId() {
        MatrixCursor messages = new MatrixCursor(new String[] {
                Mock.Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN,
                "_id",
                Mock.Telephony.Sms.Conversations.THREAD_ID,
                Mock.Telephony.Sms.Conversations.DATE,
                Mock.Telephony.Mms.MESSAGE_BOX,
                Mock.Telephony.Mms.STATUS,
                Mock.Telephony.Mms.SUBSCRIPTION_ID
        });
        messages.addRow(new Object[] {"mms", 4L, 5L, 6L,
                Mock.Telephony.Mms.MESSAGE_BOX_INBOX, 0, 8});
        messages.moveToFirst();

        Text text = new Text(messages, null);

        assertEquals(8, text.getSubscriptionId());
    }
}
