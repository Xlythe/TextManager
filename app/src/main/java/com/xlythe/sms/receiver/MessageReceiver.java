package com.xlythe.sms.receiver;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.xlythe.manager.notifications.messages.MessageBasedNotificationManager;
import com.xlythe.manager.notifications.messages.NotificationChannel;
import com.xlythe.manager.notifications.messages.NotificationContact;
import com.xlythe.manager.notifications.messages.NotificationMessage;
import com.xlythe.manager.notifications.messages.NotificationThread;
import com.xlythe.sms.MessageActivity;
import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.sms.util.BitmapUtils;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.TextReceiver;

import java.util.concurrent.ExecutionException;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.app.TaskStackBuilder;

public class MessageReceiver extends TextReceiver {
    private static final String TAG = "Fetch";
    private static final String MESSAGE_CHANNEL_ID = "messages";

    @Override
    public void onMessageReceived(Context context, Text text) {
        if (MessageActivity.isVisible(text.getThreadId())) {
            Log.w(TAG, "Received message, but not showing a notification because the thread is already visible to the user");
            return;
        }

        new Thread(() -> notify(context, text)).start();
    }

    @WorkerThread
    private void notify(Context context, Text text) {
        TextManager textManager = TextManager.getInstance(context);
        MessageBasedNotificationManager notificationManager = MessageBasedNotificationManager.from(context);

        Contact sender = textManager.getSender(text).get();
        Contact self = textManager.getSelf();

        // Contact state.
        NotificationContact notificationContact = new NotificationContact.Builder()
                .id(sender.getId())
                .phoneNumber(sender.getNumber())
                .name(sender.getDisplayName())
                .icon(BitmapUtils.toBitmap(new ProfileDrawable(context, sender)))
                .build();
        NotificationContact selfContact = new NotificationContact.Builder()
                .id(self.getId())
                .phoneNumber(self.getNumber())
                .name(self.getDisplayName())
                .icon(BitmapUtils.toBitmap(new ProfileDrawable(context, self)))
                .build();

        // Global state for all notifications
        NotificationChannel notificationChannel = new NotificationChannel.Builder()
                .id(MESSAGE_CHANNEL_ID)
                .appIcon(R.drawable.fetch_icon_notif)
                .accentColor(context.getResources().getColor(R.color.colorPrimary))
                .channelName(R.string.notification_channel_name)
                .channelDescription(R.string.notification_channel_description)
                .self(selfContact)
                .build();

        // State specific to the conversation
        NotificationThread notificationThread = new NotificationThread.Builder(notificationChannel)
                .id(text.getThreadId())
                .icon(BitmapUtils.toBitmap(new ProfileDrawable(context, textManager.getMembersExceptMe(text).get())))
                .setOnClickIntent(createOnClickIntent(context, text.getThreadId()))
                .setOnDismissIntent(createOnDismissIntent(context, text))
                .setOnQuickReplyIntent(createQuickReplyIntent(context, text))
                .build();

        // State specific to the single text
        NotificationMessage notificationMessage = new NotificationMessage.Builder(notificationThread)
                .id(text.getId())
                .sender(notificationContact)
                .body(body(context, text))
                .image(asBitmap(context, text)) // Null is acceptable
                .setOnDismissIntent(createOnDismissIntent(context, text))
                .build();

        // Show the notification
        notificationManager.notify(notificationMessage);
    }

    private PendingIntent createOnClickIntent(Context context, String threadId) {
        Intent intent = new Intent(context, MessageActivity.class);
        intent.putExtra(MessageActivity.EXTRA_THREAD_ID, threadId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MessageActivity.class);
        stackBuilder.addNextIntent(intent);
        return stackBuilder.getPendingIntent(threadId.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent createOnDismissIntent(Context context, Text text) {
        Intent intent = new Intent(context, OnDismissReceiver.class);

        // Note: The extras get lost when sending the broadcast, but the data is preserved.
        // Not sure why, but we avoid the problem if we just stick to using the data uri. Meh.
        intent.setData(Uri.parse("sms://threads/" + text.getThreadId()));

        return PendingIntent.getBroadcast(context, text.getThreadId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    private PendingIntent createQuickReplyIntent(Context context, Text text) {
        Intent intent = new Intent(context, OnQuickReplyReceiver.class);

        // Note: The extras get lost when sending the broadcast, but the data is preserved.
        // Not sure why, but we avoid the problem if we just stick to using the data uri. Meh.
        intent.setData(Uri.parse("sms://threads/" + text.getThreadId()));

        return PendingIntent.getBroadcast(context, text.getThreadId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    @Nullable
    private String body(Context context, Text text) {
        if (text.getBody() != null) {
            return text.getBody();
        }
        if (text.getAttachment() != null) {
            int typeString = 0;
            switch (text.getAttachment().getType()) {
                case IMAGE:
                    // fall through
                case HIGH_RES_IMAGE:
                    typeString = R.string.notification_label_picture;
                    break;
                case VIDEO:
                    typeString = R.string.notification_label_video;
                    break;
                case VOICE:
                    typeString = R.string.notification_label_voice;
                    break;
            }
            return italic(context.getString(typeString));
        }

        return null;
    }

    @WorkerThread
    @Nullable
    private Bitmap asBitmap(Context context, Text text) {
        if (text.getAttachment() != null) {
            switch (text.getAttachment().getType()) {
                case IMAGE:
                    // fall through
                case HIGH_RES_IMAGE:
                    // fall through
                case VIDEO:
                    try {
                        return Glide
                                .with(context)
                                .asBitmap()
                                .load(text.getAttachment().getUri())
                                .submit(1024, 512)
                                .get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        Log.e(TAG, "Failed to download attachment as a bitmap", e);
                    }
                    break;
                default:
                    Log.w(TAG, "Unable to attach an attachment of type " + text.getAttachment().getType() + " to a notification");
                    break;
            }
        }
        return null;
    }

    private static String italic(String string) {
        return "<i>" + string + "</i>";
    }

    public static final class OnDismissReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getData() == null) {
                Log.w(TAG, "Attempted to mark thread as read, but the original text was forgotten");
                setResultCode(Activity.RESULT_CANCELED);
                return;
            }

            TextManager textManager = TextManager.getInstance(context);
            String threadId = intent.getData().getLastPathSegment();

            textManager.markAsRead(textManager.getThread(threadId).get());
            Log.d(TAG, "Notification dismissed. Marked thread as read.");
        }
    }

    public static final class OnQuickReplyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getData() == null) {
                Log.w(TAG, "Attempted to send a reply, but the original text was forgotten");
                setResultCode(Activity.RESULT_CANCELED);
                return;
            }

            TextManager textManager = TextManager.getInstance(context);
            String threadId = intent.getData().getLastPathSegment();

            String reply = intent.getStringExtra(MessageBasedNotificationManager.EXTRA_REPLY);
            com.xlythe.textmanager.text.Thread thread = textManager.getThread(threadId).get();
            textManager.markAsRead(thread);
            textManager.send(reply).to(thread);
            Log.d(TAG, "Reply successfully sent");

            // We don't know the message ID of the reply yet, so just make one up.
            setResultData(Long.toString(System.currentTimeMillis()));
        }
    }
}