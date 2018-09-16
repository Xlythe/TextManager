package com.xlythe.sms.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.xlythe.sms.MessageActivity;
import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.sms.notification.NotificationChannel;
import com.xlythe.sms.notification.NotificationMessage;
import com.xlythe.sms.notification.MessageBasedNotificationManager;
import com.xlythe.sms.notification.NotificationThread;
import com.xlythe.sms.util.BitmapUtils;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.TextReceiver;
import com.xlythe.view.camera.Image;

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

        // Global state for all notifications
        NotificationChannel notificationChannel = new NotificationChannel.Builder()
                .id(MESSAGE_CHANNEL_ID)
                .appIcon(R.drawable.fetch_icon_notif)
                .accentColor(context.getResources().getColor(R.color.colorPrimary))
                .channelName(R.string.notification_channel_name)
                .channelDescription(R.string.notification_channel_description)
                .build();

        // State specific to the conversation
        NotificationThread notificationThread = new NotificationThread.Builder(notificationChannel)
                .id(text.getThreadId())
                .icon(BitmapUtils.toBitmap(new ProfileDrawable(context, textManager.getMembersExceptMe(text).get())))
                .setOnClickIntent(createOnClickIntent(context, text.getThreadId()))
                .setQuickReplyIntent(createQuickReplyIntent(context, text))
                .build();

        // State specific to the single text
        NotificationMessage notificationMessage = new NotificationMessage.Builder(notificationThread)
                .id(text.getId())
                .sender(textManager.getSender(text).get().getDisplayName())
                .icon(BitmapUtils.toBitmap(new ProfileDrawable(context, textManager.getSender(text).get())))
                .body(body(context, text))
                .image(asBitmap(context, text)) // Null is acceptable
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

    private PendingIntent createQuickReplyIntent(Context context, Text text) {
        Intent intent = new Intent(context, OnQuickReplyReceiver.class);
        intent.putExtra(EXTRA_TEXT, text);
        return PendingIntent.getBroadcast(context, text.getId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
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

    public static final class OnQuickReplyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextManager textManager = TextManager.getInstance(context);
            Text originalText = intent.getParcelableExtra(EXTRA_TEXT);
            if (originalText == null) {
                Log.w(TAG, "Attempted to send a reply, but the original text was forgotten");
                return;
            }

            String reply = intent.getStringExtra(MessageBasedNotificationManager.EXTRA_REPLY);
            Log.d(TAG, "User typed message " + reply + " in reply to " + originalText.getBody());
            textManager.send(reply).to(originalText);
            Log.d(TAG, "Reply successfully sent");
        }
    }
}