package com.xlythe.sms.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.xlythe.sms.BuildConfig;
import com.xlythe.sms.MainActivity;
import com.xlythe.sms.MessageActivity;
import com.xlythe.textmanager.text.ShadowTextManager;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPendingIntent;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Notifications for Fetch create a single group for all of Fetch's notifications. Then, for each
 * thread, there will be a separate child notification. If Alice texts you 3 times, there will be
 * one summary notification and one child notification for Alice's thread. If Bob then texts you,
 * there will be 3 notifications (1 summary, 1 for Alice, 1 for Bob). Viewing the MainActivity will
 * clear all active notifications. Viewing a MessageActivity will clear the notifications for that
 * thread, but will leave the other notifications active.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk=23, shadows={ShadowTextManager.class}, constants = BuildConfig.class)
public class NotificationsTest {
    private static final String ME = "111-111-1111";
    private static final String ALICE= "222-222-2222";
    private static final String BOB = "333-333-3333";

    private static final long THREAD_ALICE = 1l;
    private static final long THREAD_BOB = 2l;

    private Context mContext;
    private ShadowNotificationManager mNotificationManager;

    @Before
    public void setup() {
        // Print out everything from logcat to the console instead
        ShadowLog.stream = System.out;

        // Setup variables
        mContext = RuntimeEnvironment.application;
        mNotificationManager = Shadows.shadowOf(
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE));

        // Update the owner's contact in TextManager
        ShadowTextManager shadowTextManager = (ShadowTextManager) ShadowExtractor.extract(TextManager.getInstance(mContext));
        shadowTextManager.setSelf(ME);
    }

    @Test
    public void singleThread() {
        sendTextFromAlice("Hello World");

        // One summary, and one child notification
        assertEquals(2, mNotificationManager.size());
        verifyReturnsMessageActivity(THREAD_ALICE);
    }

    @Test
    public void multipleThreads() {
        sendTextFromAlice("Hello World");
        sendTextFromBob("Goodbye World");

        // One summary, and two child notifications
        assertEquals(3, mNotificationManager.size());
        verifyReturnsMainActivity();
    }

    @Test
    public void multipleNotificationsFromSameSender() {
        sendTextFromAlice("1");
        sendTextFromAlice("2");
        sendTextFromAlice("3");

        // One summary, and 1 child
        assertEquals(2, mNotificationManager.size());
    }

    @Test
    public void notificationDismissable() {
        sendTextFromAlice("Hello World");

        Notifications.dismissNotification(mContext, THREAD_ALICE);

        // All notifications viewed, none left
        assertEquals(0, mNotificationManager.size());
    }

    @Test
    public void multipleThreadsDismissOne() {
        sendTextFromAlice("Hello World");
        sendTextFromBob("Goodbye World");

        Notifications.dismissNotification(mContext, THREAD_ALICE);

        // 1 summary, and Bob's notification
        assertEquals(2, mNotificationManager.size());
    }

    @Test
    public void multipleThreadsDismissAll() {
        sendTextFromAlice("Hello World");
        sendTextFromBob("Goodbye World");

        Notifications.dismissAllNotifications(mContext);

        // Nothing left
        assertEquals(0, mNotificationManager.size());
    }

    @Test
    public void correctTargetEvenAfterDismissing() {
        // Show Alice's text, and then dismiss it
        sendTextFromAlice("Hello World");
        verifyReturnsMessageActivity(THREAD_ALICE);
        Notifications.dismissNotification(mContext, THREAD_ALICE);

        // Show Bob's text, verify it goes to Bob, then dismiss
        sendTextFromBob("Goodbye World");
        verifyReturnsMessageActivity(THREAD_BOB);
        Notifications.dismissNotification(mContext, THREAD_BOB);

        // Show several texts from Alice, verify it goes to Alice
        sendTextFromAlice("1");
        sendTextFromAlice("2");
        sendTextFromAlice("3");
        verifyReturnsMessageActivity(THREAD_ALICE);

        // Add some from Bob
        sendTextFromBob("1");
        sendTextFromBob("2");
        verifyReturnsMainActivity();

        // Dismiss Alice and verify it goes to Bob
        Notifications.dismissNotification(mContext, THREAD_ALICE);
        verifyReturnsMessageActivity(THREAD_BOB);
    }

    private void sendTextFromAlice(String msg) {
        Notifications.buildNotification(mContext, new Text.DebugBuilder()
                .setThreadId(THREAD_ALICE)
                .setSender(ALICE)
                .addRecipient(ME)
                .message(msg)
                .build());
    }

    private void sendTextFromBob(String msg) {
        Notifications.buildNotification(mContext, new Text.DebugBuilder()
                .setThreadId(THREAD_BOB)
                .setSender(BOB)
                .addRecipient(ME)
                .message(msg)
                .build());
    }

    private void verifyReturnsMainActivity() {
        // Tapping on the summary should go to MainActivity
        for (Notification notification : mNotificationManager.getAllNotifications()) {
            if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != Notification.FLAG_GROUP_SUMMARY) {
                continue;
            }

            ShadowPendingIntent pendingIntent = Shadows.shadowOf(notification.contentIntent);

            // We're launching an activity
            assertTrue(pendingIntent.isActivityIntent());

            // There should be 1 intent. The root (MainActivity)
            assertEquals(1, pendingIntent.getSavedIntents().length);
            Intent mainActivity = pendingIntent.getSavedIntent();
            assertEquals(MainActivity.class.getCanonicalName(), mainActivity.getComponent().getClassName());
        }
    }

    private void verifyReturnsMessageActivity(long expectedThreadId) {
        for (Notification notification : mNotificationManager.getAllNotifications()) {
            ShadowPendingIntent pendingIntent = Shadows.shadowOf(notification.contentIntent);

            // We're launching an activity
            assertTrue(pendingIntent.isActivityIntent());

            // There should be 2 intents. The root (MainActivity) and the child (MessageActivity).
            assertEquals(2, pendingIntent.getSavedIntents().length);
            Intent mainActivity = pendingIntent.getSavedIntents()[0];
            Intent messageActivity = pendingIntent.getSavedIntents()[1];
            assertEquals(MainActivity.class.getCanonicalName(), mainActivity.getComponent().getClassName());
            assertEquals(MessageActivity.class.getCanonicalName(), messageActivity.getComponent().getClassName());

            // MessageActivity should have extras for the thread we care about
            // We don't care about the implementation (thread vs id).
            String threadId = messageActivity.getStringExtra(MessageActivity.EXTRA_THREAD_ID);
            Thread thread = messageActivity.getParcelableExtra(MessageActivity.EXTRA_THREAD);
            if (threadId != null) {
                assertEquals(Long.toString(expectedThreadId), threadId);
            } else if (thread != null) {
                assertEquals(Long.toString(expectedThreadId), thread.getId());
            } else {
                throw new IllegalStateException("No thread or thread id given");
            }
        }
    }
}
