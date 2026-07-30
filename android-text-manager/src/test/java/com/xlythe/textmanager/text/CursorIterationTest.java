package com.xlythe.textmanager.text;

import static org.junit.Assert.assertEquals;

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
    }}
