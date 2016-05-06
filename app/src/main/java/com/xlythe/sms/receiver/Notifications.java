package com.xlythe.sms.receiver;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;

import com.xlythe.sms.MainActivity;
import com.xlythe.sms.MessageActivity;
import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;
import com.xlythe.textmanager.text.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Notifications {
    private static final String TAG = "Notifications";
    private static final String EXTRA_INLINE_REPLY = "inline_reply";
    private static final String EXTRA_TEXT = "text";
    private static final String EXTRA_THREAD_ID = "thread_id";
    private static final String TEXTS_VISIBLE_IN_NOTIFICATION = "texts_visible_in_notification";
    private static final String NOTIFICATIONS = "notifications";
    private static final String NOTIFICATION_IDS = "notification_ids";
    private static final int GROUP_SUMMARY_ID = 12345;

    public static void buildNotification(Context context, Text text) {
        if (MessageActivity.isVisible(text.getThreadId())) {
            Log.w(TAG, "This thread is already visible to the user");
            return;
        }

        Set<Text> texts = getVisibleTexts(context, text);
        buildNotification(context, getTextsFromSameSender(text, texts), text.getThreadId().hashCode());
        buildGroupSummary(context, texts, GROUP_SUMMARY_ID);
    }

    public static void buildNotification(Context context, Set<Text> texts, int id) {
        Log.v(TAG, "Building a notification");
        context = context.getApplicationContext();
        addNotificationId(context, id);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.fetch_icon_notif)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true)
                .setDeleteIntent(buildOnDismissIntent(context, id, texts, false /*dismissAll*/))
                .setContentIntent(buildOnClickIntent(context, id, texts))
                .setLights(Color.WHITE, 500, 1500)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setGroup(TAG);

        if (sameSender(texts)) {
            Log.v(TAG, "All texts are from the same sender");
            Text randomText = texts.iterator().next();
            ProfileDrawable icon = new ProfileDrawable(context, randomText.getMembersExceptMe(context).get());
            builder.setLargeIcon(drawableToBitmap(icon))
                    .addAction(buildReplyAction(context, id, randomText));
        }

        if (texts.size() == 1) {
            Log.v(TAG, "There's only one text");
            buildDetailedNotification(context, texts.iterator().next(), builder);
        } else {
            buildSummaryNotification(context, texts, builder);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(id, builder.build());
    }

    public static void buildGroupSummary(Context context, Set<Text> texts, int id) {
        Log.v(TAG, "Building group summary");
        context = context.getApplicationContext();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.fetch_icon_notif)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true)
                .setDeleteIntent(buildOnDismissIntent(context, id, texts, true /*dismissAll*/))
                .setContentIntent(buildOnClickIntent(context, id, texts))
                .setLights(Color.WHITE, 500, 1500)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setGroup(TAG)
                .setGroupSummary(true);

        if (sameSender(texts)) {
            Log.v(TAG, "All texts are from the same sender");
            Text randomText = texts.iterator().next();
            ProfileDrawable icon = new ProfileDrawable(context, randomText.getMembersExceptMe(context).get());
            builder.setLargeIcon(drawableToBitmap(icon))
                    .addAction(buildReplyAction(context, id, randomText));
        }

        if (texts.size() == 1) {
            Log.v(TAG, "There's only one text");
            buildDetailedNotification(context, texts.iterator().next(), builder);
        } else {
            buildSummaryNotification(context, texts, builder);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(id, builder.build());
    }

    private static boolean sameSender(Set<Text> texts) {
        String prevNumber = null;
        for (Text text : texts) {
            if (prevNumber != null) {
                if (!prevNumber.equals(text.getThreadId())) {
                    return false;
                }
            }
            prevNumber = text.getThreadId();
        }
        return true;
    }

    /**
     * Dismisses all notifications for the given thread
     */
    public static void dismissNotification(Context context, Thread thread) {
        dismissNotification(context, thread.getId());
    }

    /**
     * Dismisses all notifications for the given thread
     */
    public static void dismissNotification(Context context, String threadId) {
        // Clean up any data regarding this thread
        clearNotification(context, threadId);

        // Dismiss the notification for the thread
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(threadId.hashCode());

        // If there are no notifications left, dismiss the summary too
        Set<Text> texts = getVisibleTexts(context);
        if (texts.isEmpty()) {
            notificationManager.cancel(GROUP_SUMMARY_ID);
        }
    }

    /**
     * Dismisses all notifications
     */
    public static void dismissAllNotifications(Context context) {
        // Dismiss all notifications we've created
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();

        // Stop persisting the data from those notifications
        clearAllNotifications(context);
    }

    /**
     * Stops persisting any notifications related to the given thread
     */
    private static void clearNotification(Context context, String threadId) {
        // Grab all the notifications
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> dataSet = prefs.getStringSet(NOTIFICATIONS, new HashSet<String>());

        // Loop over them and remove any that have the same id as the thread
        Iterator<String> iterator = dataSet.iterator();
        while (iterator.hasNext()) {
            String serializedData = iterator.next();
            Text text = Text.fromBytes(Utils.hexToBytes(serializedData));

            if (threadId.equals(text.getThreadId())) {
                iterator.remove();
            }
        }

        // Re-save the notifications
        editor.putStringSet(NOTIFICATIONS, dataSet);
        editor.apply();
    }

    /**
     * Stops persisting notification data
     */
    private static void clearAllNotifications(Context context) {
        context = context.getApplicationContext();
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Returns the texts we're currently showing in the notification.
     */
    private static Set<Text> getVisibleTexts(Context context) {
        return getVisibleTexts(context, null);
    }

    /**
     * Returns the texts we're currently showing in the notification. Text, if not null, is added to that set.
     */
    private static Set<Text> getVisibleTexts(Context context, Text text) {
        Set<Text> texts = new HashSet<>();

        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> dataSet = prefs.getStringSet(NOTIFICATIONS, new HashSet<String>());

        if (text != null) {
            dataSet.add(Utils.bytesToHex(text.toBytes()));
            editor.putStringSet(NOTIFICATIONS, dataSet);
            editor.apply();
        }

        for (String serializedData : dataSet) {
            texts.add(Text.fromBytes(Utils.hexToBytes(serializedData)));
        }

        return texts;
    }

    private static void addNotificationId(Context context, int id) {
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> idsAsStrings = prefs.getStringSet(NOTIFICATION_IDS, new HashSet<String>());
        idsAsStrings.add(Integer.toString(id));
        editor.putStringSet(NOTIFICATION_IDS, idsAsStrings);
        editor.apply();
    }

    private static Set<Integer> getNotificationIds(Context context) {
        Set<Integer> ids = new HashSet<>();

        SharedPreferences prefs = getSharedPreferences(context);
        Set<String> idsAsStrings = prefs.getStringSet(NOTIFICATION_IDS, new HashSet<String>());

        for (String string : idsAsStrings) {
            ids.add(Integer.parseInt(string));
        }

        return ids;
    }

    private static Set<Text> getTextsFromSameSender(Text original, Set<Text> texts) {
        Set<Text> sameGroup = new HashSet<>();
        for (Text text : texts) {
            if (text.getThreadIdAsLong() == original.getThreadIdAsLong()) {
                sameGroup.add(text);
            }
        }
        return sameGroup;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private static PendingIntent buildOnClickIntent(Context context, int requestCode, Set<Text> texts) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        Intent intent;
        if (sameSender(texts)) {
            intent = new Intent(context, MessageActivity.class);
            stackBuilder.addParentStack(MessageActivity.class);

            String threadId = texts.iterator().next().getThreadId();
            intent.putExtra(MessageActivity.EXTRA_THREAD_ID, threadId);
        } else {
            intent = new Intent(context, MainActivity.class);
            stackBuilder.addParentStack(MainActivity.class);
        }

        stackBuilder.addNextIntent(intent);
        return stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent buildOnDismissIntent(Context context, int requestCode, Set<Text> texts, boolean dismissAll) {
        Intent dismissIntent = new Intent(context, OnDismissReceiver.class);
        if (!dismissAll) {
            // All texts are by the same sender, so grab one of the threads
            String threadId = texts.iterator().next().getThreadId();
            dismissIntent.putExtra(EXTRA_THREAD_ID, threadId);
        }
        return PendingIntent.getBroadcast(context, requestCode, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static NotificationCompat.Action buildReplyAction(Context context, int requestCode, Text text) {
        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_INLINE_REPLY)
                .setLabel(context.getString(R.string.action_reply))
                .build();
        Intent replyIntent = new Intent(context, OnReplyReceiver.class);
        replyIntent.putExtra(EXTRA_TEXT, text);
        PendingIntent onReplyPendingIntent = PendingIntent.getBroadcast(context, requestCode, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Action.Builder(R.drawable.ic_send, context.getString(R.string.action_reply), onReplyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    /**
     * Only one message, can be text or Image/Video thumbnail
     */
    private static void buildDetailedNotification(Context context, Text text, NotificationCompat.Builder builder) {
        builder.setContentTitle(text.getSender(context).get().getDisplayName());
        if (text.getAttachment() != null && text.getAttachment().getType() == Attachment.Type.IMAGE) {
            NotificationCompat.BigPictureStyle pictureStyle = new NotificationCompat.BigPictureStyle();
            try {
                Spanned s = Html.fromHtml(italic(context.getString(R.string.notification_label_picture)));
                ProfileDrawable icon = new ProfileDrawable(context, text.getMembersExceptMe(context).get());
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), text.getAttachment().getUri());
                builder.setLargeIcon(bitmap)
                        .setContentText(s)
                        .setStyle(pictureStyle);
                pictureStyle.setBigContentTitle(text.getSender(context).get().getDisplayName())
                        .bigLargeIcon(drawableToBitmap(icon))
                        .setSummaryText(s)
                        .bigPicture(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            NotificationCompat.BigTextStyle textStyle = new NotificationCompat.BigTextStyle();
            builder.setStyle(textStyle);
            textStyle.setBigContentTitle(text.getSender(context).get().getDisplayName());
            // Maybe add video too, but we have a problem with thumbnails without glide
            if (text.getAttachment() != null && text.getAttachment().getType() == Attachment.Type.VIDEO) {
                Spanned s = Html.fromHtml(italic(context.getString(R.string.notification_label_video)));
                builder.setContentText(s);
                textStyle.bigText(s);
            } else if (text.getAttachment() != null && text.getAttachment().getType() == Attachment.Type.VOICE) {
                Spanned s = Html.fromHtml(italic(context.getString(R.string.notification_label_voice)));
                builder.setContentText(s);
                textStyle.bigText(s);
            } else {
                builder.setContentText(text.getBody());
                textStyle.bigText(text.getBody());
            }
        }
    }

    /**
     * Multiple messages, should all look the same unless its only one conversation
     */
    private static void buildSummaryNotification(Context context, Set<Text> texts, NotificationCompat.Builder builder) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        Set<String> names = new HashSet<>();
        inboxStyle.setBigContentTitle(context.getString(R.string.notification_label_new_messages, texts.size()));
        List<Text> sortedTexts = new ArrayList<>(texts);
        Collections.sort(sortedTexts);
        for (Text text: sortedTexts) {
            if (text.getAttachment() != null) {
                int typeString = 0;
                switch(text.getAttachment().getType()) {
                    case IMAGE:
                        typeString = R.string.notification_label_picture;
                        break;
                    case VIDEO:
                        typeString = R.string.notification_label_video;
                        break;
                    case VOICE:
                        typeString = R.string.notification_label_voice;
                        break;
                }
                inboxStyle.addLine(Html.fromHtml(
                        bold(text.getSender(context).get().getDisplayName()) + " " + italic(context.getString(typeString))));
            } else {
                String body = text.getBody();
                if (body == null) body = "";
                inboxStyle.addLine(Html.fromHtml(
                        bold(text.getSender(context).get().getDisplayName()) + " " + body));
            }
            names.add(text.getSender(context).get().getDisplayName());
        }
        builder.setContentTitle(context.getString(R.string.notification_label_new_messages, texts.size()))
                .setContentText(TextUtils.join(", ", names))
                .setStyle(inboxStyle);
    }

    private static String italic(String string) {
        return "<i>" + string + "</i>";
    }

    private static String bold(String string) {
        return "<b>" + string + "</b>";
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(TEXTS_VISIBLE_IN_NOTIFICATION, Context.MODE_PRIVATE);
    }

    public static final class OnReplyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            CharSequence reply = getMessageText(intent);
            Text text = getText(intent);
            if (!TextUtils.isEmpty(reply)) {
                Log.d(TAG, "Sending reply");
                TextManager.getInstance(context).send(new Text.Builder()
                        .message(reply.toString())
                        .addRecipients(text.getMembersExceptMe(context).get())
                        .build());
            } else {
                Log.w(TAG, "Was told to send a reply, but there was no message. Opening activity for thread " +text.getThreadId());
                Intent startActivity = new Intent(context, MessageActivity.class);
                startActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity.putExtra(MessageActivity.EXTRA_THREAD_ID, text.getThreadId());
                context.startActivity(startActivity);
            }
            Notifications.dismissNotification(context, text.getThreadId());
        }

        private CharSequence getMessageText(Intent intent) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                return remoteInput.getCharSequence(EXTRA_INLINE_REPLY);
            }
            return null;
        }

        private Text getText(Intent intent) {
            return intent.getParcelableExtra(EXTRA_TEXT);
        }
    }

    public static final class OnDismissReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Clearing notifications");
            if (intent.hasExtra(EXTRA_THREAD_ID)) {
                String threadId = intent.getStringExtra(EXTRA_THREAD_ID);
                Notifications.clearNotification(context, threadId);
            } else {
                Notifications.clearAllNotifications(context);
            }
        }
    }
}
