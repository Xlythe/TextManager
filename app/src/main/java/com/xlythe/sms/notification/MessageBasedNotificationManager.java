package com.xlythe.sms.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import com.xlythe.sms.R;
import com.xlythe.textmanager.text.util.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * A NotificationManager that specifically displays notifications related to messages. Notifications
 * are split into 3 levels.
 *
 * 1) NotificationChannel is the channel for all notifications. It has app-specific state, such
 * as the app icon.
 *
 * 2) NotificationThread is a single conversation with another person. All notifications are grouped
 * into a single thread which may (or may not) be expandable into the various messages that make it
 * up (depends on Android OS).
 *
 * 3) NotificationMessage is a single message in a thread.
 *
 * Every thread is given its own notification, so conversations with 2 different contacts will
 * create 2 different notifications. When there is only one unread message, that message is
 * displayed within the notification. When there are multiple unread messages, a shortened summary
 * of each message is created instead.
 */
public class MessageBasedNotificationManager {
    private static final String TAG = "NotificationManager";

    private static final String PERSISTENT_STORAGE = "message_based_notification_manager";
    private static final String NOTIFICATION_MESSAGES = "notification_messages";
    private static final String NOTIFICATION_THREADS = "notification_threads";
    private static final String NOTIFICATION_CHANNELS = "notification_channels";

    private static final String EXTRA_INLINE_REPLY = "inline_reply";
    private static final String EXTRA_ON_CLICK_INTENT = "on_click_intent";
    private static final String EXTRA_ON_QUICK_REPLY_INTENT = "on_quick_reply_intent";
    public static final String EXTRA_REPLY = "reply";
    private static final String EXTRA_THREAD_ID = "thread_id";
    private static final String EXTRA_MESSAGE_ID = "message_id";

    private final Context context;
    private final NotificationManagerCompat notificationManagerCompat;

    private MessageBasedNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        notificationManagerCompat = NotificationManagerCompat.from(context);
    }

    public static MessageBasedNotificationManager from(Context context) {
        return new MessageBasedNotificationManager(context);
    }

    private SharedPreferences getPersistentStorage() {
        return context.getSharedPreferences(PERSISTENT_STORAGE, Context.MODE_PRIVATE);
    }

    private Set<String> getVisibleNotificationMessages(String threadId) {
        return getPersistentStorage().getStringSet(NOTIFICATION_MESSAGES + "_" + threadId, new HashSet<String>());
    }

    private Set<String> getVisibleNotificationThreads(String channelId) {
        return getPersistentStorage().getStringSet(NOTIFICATION_THREADS + "_" + channelId, new HashSet<String>());
    }

    private Set<String> getVisibleNotificationChannels() {
        return getPersistentStorage().getStringSet(NOTIFICATION_CHANNELS, new HashSet<String>());
    }

    private void persistNotificationMessage(NotificationMessage notificationMessage) {
        NotificationThread notificationThread = notificationMessage.getNotificationThread();
        NotificationChannel notificationChannel = notificationThread.getNotificationChannel();
        Log.d(TAG, "Persisting message " + notificationMessage.getId()
                + " for thread " + notificationThread.getId()
                + " in channel " + notificationChannel.getId());

        // This is the set of all messages for a given thread.
        Set<String> visibleNotificationMessages = getVisibleNotificationMessages(notificationThread.getId());
        visibleNotificationMessages.add(notificationMessage.getId());

        // This is the set of all threads for a given channel.
        Set<String> visibleNotificationThreads = getVisibleNotificationThreads(notificationChannel.getId());
        visibleNotificationThreads.add(notificationThread.getId());

        // This is the set of all channels.
        Set<String> visibleNotificationChannels = getVisibleNotificationChannels();
        visibleNotificationChannels.add(notificationChannel.getId());

        Log.d(TAG, "There are now " + visibleNotificationMessages.size() + " messages for this thread,"
                + " and " + visibleNotificationThreads.size() + " threads for this channel");

        getPersistentStorage()
                .edit()
                .putStringSet(NOTIFICATION_MESSAGES + "_" + notificationThread.getId(), visibleNotificationMessages)
                .putStringSet(NOTIFICATION_THREADS + "_" + notificationThread.getId(), visibleNotificationThreads)
                .putStringSet(NOTIFICATION_CHANNELS , visibleNotificationChannels)
                .apply();
    }

    public void cancelMessage(String messageId) {
        Log.d(TAG, "Canceling message " + messageId);

        // Iterate over all channels to find which one contains the message.
        Set<String> visibleNotificationChannels = getVisibleNotificationChannels();
        for (String channelId : visibleNotificationChannels) {
            // Iterate over all threads to find which one contains the message.
            Set<String> visibleNotificationThreads = getVisibleNotificationThreads(channelId);
            try {
                for (String threadId : visibleNotificationThreads) {
                    // Iterate over all messages to find the message.
                    Set<String> visibleNotificationMessages = getVisibleNotificationMessages(threadId);
                    try {
                        if (visibleNotificationMessages.remove(messageId)) {
                            // If found, update the persistent storage.
                            getPersistentStorage()
                                    .edit()
                                    .putStringSet(NOTIFICATION_MESSAGES + "_" + threadId, visibleNotificationMessages)
                                    .apply();

                            // And dismiss the notification.
                            notificationManagerCompat.cancel(messageId.hashCode());

                            // Break out.
                            return;
                        }
                    } finally {
                        // If this was the last message within the thread, dismiss the thread too.
                        if (visibleNotificationMessages.isEmpty()) {
                            Log.d(TAG, "All messages for thread " + threadId + " were cancelled, so canceling thread too");
                            visibleNotificationThreads.remove(threadId);
                            getPersistentStorage()
                                    .edit()
                                    .putStringSet(NOTIFICATION_THREADS + "_" + channelId, visibleNotificationThreads)
                                    .apply();
                            notificationManagerCompat.cancel(threadId.hashCode());
                        } else {
                            Log.d(TAG, "There are " + visibleNotificationMessages.size() + " messages left in thread " + threadId);
                        }
                    }
                }
            } finally {
                // If this was the last thread within the channel, dismiss the channel too.
                if (visibleNotificationThreads.isEmpty()) {
                    Log.d(TAG, "All threads for channel " + channelId + " were cancelled, so canceling channel too");
                    visibleNotificationChannels.remove(channelId);
                    getPersistentStorage()
                            .edit()
                            .putStringSet(NOTIFICATION_CHANNELS, visibleNotificationChannels)
                            .apply();
                } else {
                    Log.d(TAG, "There are " + visibleNotificationThreads.size() + " threads left in channel " + channelId);
                }
            }
        }
    }

    public void cancelThread(String threadId) {
        Log.d(TAG, "Canceling thread " + threadId);
        for (String messageId : getVisibleNotificationMessages(threadId)) {
            cancelMessage(messageId);
        }
    }

    public void cancelChannel(String channelId) {
        Log.d(TAG, "Canceling channel " + channelId);
        for (String threadId : getVisibleNotificationThreads(channelId)) {
            cancelThread(threadId);
        }
    }

    public void cancelAll() {
        Log.d(TAG, "Canceling all notifications");
        for (String channelId : getVisibleNotificationChannels()) {
            cancelChannel(channelId);
        }
    }

    /**
     * NotificationCompat recycles its builder, so we need to make a separate one for the group
     * summary and for the original notification. That's where this method comes in to play.
     */
    private NotificationCompat.Builder build(NotificationMessage notificationMessage) {
        NotificationThread notificationThread = notificationMessage
                .getNotificationThread();

        NotificationChannel notificationChannel = notificationThread
                .getNotificationChannel();

        if (Build.VERSION.SDK_INT >= 26) {
            ensureNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(notificationChannel.getAppIcon())
                .setColor(notificationChannel.getAccentColor())
                .setLights(Color.WHITE, 500, 1500)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setChannel(notificationChannel.getId())
                .setAutoCancel(true)
                .setContentTitle(notificationMessage.getSender())
                .setLargeIcon(notificationMessage.getIcon())
                .addAction(buildReplyAction(context, notificationThread))
                .setContentIntent(notificationThread.getOnClickIntent())
                .setGroup(notificationThread.getId())
                .setGroupSummary(false);

        if (notificationMessage.getImage() != null) {
            builder.setContentText(notificationMessage.getBody());
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .setBigContentTitle(notificationMessage.getSender())
                    .bigLargeIcon(notificationMessage.getIcon())
                    .setSummaryText(Html.fromHtml(notificationMessage.getBody()))
                    .bigPicture(notificationMessage.getImage()));
        } else {
            builder.setContentText(notificationMessage.getBody());
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .setBigContentTitle(notificationMessage.getSender())
                    .bigText(notificationMessage.getBody()));
        }
        return builder;
    }

    public void notify(NotificationMessage notificationMessage) {
        NotificationThread notificationThread = notificationMessage
                .getNotificationThread();

        notificationManagerCompat.notify(notificationMessage.getId().hashCode(),
                build(notificationMessage)
                        // Only the child is high priority, so that only it shows up as a
                        // heads up notification.
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDeleteIntent(buildOnDismissIntent(notificationMessage))
                        .build());
        notificationManagerCompat.notify(notificationMessage.getNotificationThread().getId().hashCode(),
                build(notificationMessage)
                        .setDeleteIntent(buildOnDismissIntent(notificationThread))
                        .setGroupSummary(true)
                        .build());
        persistNotificationMessage(notificationMessage);
    }

    private PendingIntent buildOnDismissIntent(NotificationMessage notificationMessage) {
        Intent intent = new Intent(context, OnDismissReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                notificationMessage.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent buildOnDismissIntent(NotificationThread notificationThread) {
        Intent intent = new Intent(context, OnDismissReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                notificationThread.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static NotificationCompat.Action buildReplyAction(Context context, NotificationThread notificationThread) {
        Intent intent = new Intent(context, InlineReplyReceiver.class);
        intent.putExtra(EXTRA_ON_CLICK_INTENT, notificationThread.getOnClickIntent());
        intent.putExtra(EXTRA_ON_QUICK_REPLY_INTENT, notificationThread.getOnQuickReplyIntent());
        intent.putExtra(EXTRA_THREAD_ID, notificationThread.getId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationThread.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_INLINE_REPLY)
                .setLabel(context.getString(R.string.action_reply))
                .build();
        return new NotificationCompat.Action.Builder(R.drawable.ic_send, context.getString(R.string.action_reply), pendingIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    @TargetApi(26)
    private void ensureNotificationChannel(NotificationChannel notificationChannel) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        for (android.app.NotificationChannel channel : notificationManager.getNotificationChannels()) {
            if (notificationChannel.getId().equals(channel.getId())) {
                return;
            }
        }

        android.app.NotificationChannel channel = new android.app.NotificationChannel(
                notificationChannel.getId(),
                context.getString(notificationChannel.getName()),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(notificationChannel.getDescription()));
        channel.enableLights(true);
        channel.setLightColor(Color.WHITE);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{150});
        notificationManager.createNotificationChannel(channel);
    }

    public static final class OnDismissReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Notification dismissed");
            MessageBasedNotificationManager notificationManager = MessageBasedNotificationManager.from(context);
            if (intent.hasExtra(EXTRA_THREAD_ID)) {
                String threadId = intent.getStringExtra(EXTRA_THREAD_ID);
                notificationManager.cancelThread(threadId);
            } else if (intent.hasExtra(EXTRA_MESSAGE_ID)) {
                String messageId = intent.getStringExtra(EXTRA_MESSAGE_ID);
                notificationManager.cancelMessage(messageId);
            } else {
                Log.w(TAG, "Unrecognized notification with no id attached");
            }
        }
    }

    public static class InlineReplyReceiver extends BroadcastReceiver {
        @CallSuper
        @Override
        public void onReceive(Context context, Intent intent) {
            final MessageBasedNotificationManager notificationManager = MessageBasedNotificationManager.from(context);
            final String threadId = getNotificationThreadId(intent);

            String reply = getReply(intent);
            if (!TextUtils.isEmpty(reply)) {
                Log.d(TAG, "Sending inline notification reply");
                intent.putExtra(EXTRA_REPLY, reply);
                try {
                    getOnQuickReplyIntent(intent).send(context, 0, intent, new PendingIntent.OnFinished() {
                        @Override
                        public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String s, Bundle bundle) {
                            Log.d(TAG, "Inline reply successfully sent");
                            notificationManager.cancelThread(threadId);
                        }
                    }, new Handler());
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Failed to send onReplyIntent", e);
                    notificationManager.cancelThread(threadId);
                }
            } else {
                Log.w(TAG, "Was told to send a reply, but there was no message. Calling onClickIntent instead.");
                PendingIntent onClickIntent = getOnClickIntent(intent);
                try {
                    onClickIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Failed to send onClickIntent", e);
                }
                notificationManager.cancelThread(threadId);
            }
        }

        private String getReply(Intent intent) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                CharSequence reply = remoteInput.getCharSequence(EXTRA_INLINE_REPLY);
                if (reply != null) {
                    return reply.toString();
                }
            }
            return null;
        }

        private PendingIntent getOnClickIntent(Intent intent) {
            return intent.getParcelableExtra(EXTRA_ON_CLICK_INTENT);
        }

        private PendingIntent getOnQuickReplyIntent(Intent intent) {
            return intent.getParcelableExtra(EXTRA_ON_QUICK_REPLY_INTENT);
        }

        private String getNotificationThreadId(Intent intent) {
            return intent.getStringExtra(EXTRA_THREAD_ID);
        }
    }
}
