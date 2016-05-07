package com.xlythe.sms.receiver;

import android.app.NotificationManager;
import android.content.Context;

import com.xlythe.sms.BuildConfig;
import com.xlythe.textmanager.text.ShadowTextManager;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;

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

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
@Config(shadows={ShadowTextManager.class}, constants = BuildConfig.class)
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
    public void singleNotification() {
        Notifications.buildNotification(mContext, new Text.DebugBuilder()
                .setThreadId(THREAD_ALICE)
                .setSender(ALICE)
                .addRecipient(ME)
                .message("Hello World")
                .build());

        // One summary, and one child notification
        assertEquals(2, mNotificationManager.size());
    }

    @Test
    public void multipleNotifications() {
        Notifications.buildNotification(mContext, new Text.DebugBuilder()
                .setThreadId(THREAD_ALICE)
                .setSender(ALICE)
                .addRecipient(ME)
                .message("Hello World")
                .build());

        assertEquals(2, mNotificationManager.size());

        Notifications.buildNotification(mContext, new Text.DebugBuilder()
                .setThreadId(THREAD_BOB)
                .setSender(BOB)
                .addRecipient(ME)
                .message("Goodbye World")
                .build());

        // One summary, and two child notifications
        assertEquals(3, mNotificationManager.size());
    }
}
