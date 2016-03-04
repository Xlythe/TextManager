package com.xlythe.sms.receiver;

import android.app.Notification;
import android.app.NotificationManager;
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
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import com.xlythe.sms.MainActivity;
import com.xlythe.sms.MessageActivity;
import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Niko on 2/25/16.
 */
public class Notifications {
    public static final String TEXTS_VISIBLE_IN_NOTIFICATION = "texts_visible_in_notification";
    public static final String NOTIFICATIONS = "notifications";

    public static void buildNotification(Context context, Text text){
        Set<Text> texts = getVisibleTexts(context, text);

        ProfileDrawable icon = new ProfileDrawable(context, text.getMembersExceptMe(context));

        Intent intent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        NotificationCompat.BigPictureStyle pictureStyle = new NotificationCompat.BigPictureStyle();
        NotificationCompat.BigTextStyle textStyle = new NotificationCompat.BigTextStyle();

        // Only one message, can be text or Image/Video thumbnail
        if (texts.size() == 1) {
            Text txt = texts.iterator().next();

            intent = new Intent(context, MessageActivity.class);
            intent.putExtra(MessageActivity.EXTRA_THREAD, TextManager.getThread(txt.getThreadId(), context));
            stackBuilder.addParentStack(MessageActivity.class);

            if (txt.getAttachment() != null && txt.getAttachment().getType() == Attachment.Type.IMAGE) {
                try {
                    Spanned s = Html.fromHtml("<i>" + context.getString(R.string.picture) + "</i>");
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), txt.getAttachment().getUri());
                    builder.setLargeIcon(bitmap)
                            .setContentTitle(txt.getSender().getDisplayName())
                            .setContentText(s)
                            .setStyle(pictureStyle);
                    pictureStyle.setBigContentTitle(txt.getSender().getDisplayName())
                            .bigLargeIcon(drawableToBitmap(icon))
                            .setSummaryText(s)
                            .bigPicture(bitmap);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

            } else {
                builder.setLargeIcon(drawableToBitmap(icon))
                        .setContentTitle(txt.getSender().getDisplayName())
                        .setStyle(textStyle);
                textStyle.setBigContentTitle(txt.getSender().getDisplayName());
                // Maybe add video too, but we have a problem with thumbnails without glide
                if (txt.getAttachment() != null && txt.getAttachment().getType() == Attachment.Type.VIDEO) {
                    Spanned s = Html.fromHtml("<i>" + context.getString(R.string.video) + "</i>");
                    builder.setContentText(s);
                    textStyle.bigText(s);
                } else if (txt.getAttachment() != null && txt.getAttachment().getType() == Attachment.Type.VOICE) {
                    Spanned s = Html.fromHtml("<i>" + context.getString(R.string.voice) + "</i>");
                    builder.setContentText(s);
                    textStyle.bigText(s);
                } else {
                    builder.setContentText(txt.getBody());
                    textStyle.bigText(txt.getBody());
                }
            }
        }

        // Multiple messages, should all look the same unless its only one conversation
        else {
            stackBuilder.addParentStack(MainActivity.class);
            Set<String> names = new HashSet<>();
            inboxStyle.setBigContentTitle(texts.size() + context.getString(R.string.new_message));
            List<Text> sortedTexts = new ArrayList<>(texts);
            Collections.sort(sortedTexts);
            for (Text txt: sortedTexts) {
                if (txt.getAttachment() != null && txt.getAttachment().getType() == Attachment.Type.VIDEO) {
                    String s = "<i>" + context.getString(R.string.video) + "</i>";
                    inboxStyle.addLine(Html.fromHtml("<b>" + txt.getSender().getDisplayName() + " </b>" + s));
                } else if (txt.getAttachment() != null && txt.getAttachment().getType() == Attachment.Type.VOICE) {
                    String s = "<i>" + context.getString(R.string.voice) + "</i>";
                    inboxStyle.addLine(Html.fromHtml("<b>" + txt.getSender().getDisplayName() + " </b>" + s));
                } else if (txt.getAttachment() != null && txt.getAttachment().getType() == Attachment.Type.IMAGE) {
                    String s = "<i>" + context.getString(R.string.picture) + "</i>";
                    inboxStyle.addLine(Html.fromHtml("<b>" + txt.getSender().getDisplayName() + " </b>" + s));
                } else {
                    inboxStyle.addLine(Html.fromHtml("<b>" + txt.getSender().getDisplayName() + " </b>" + txt.getBody()));
                }
                names.add(txt.getSender().getDisplayName());
            }
            builder.setContentTitle(texts.size() + context.getString(R.string.new_message))
                    .setContentText(TextUtils.join(", ", names))
                    .setStyle(inboxStyle);
        }

        Intent dismissIntent = new Intent(context.getApplicationContext(), OnDismissReceiver.class);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context.getApplicationContext(), 12345, dismissIntent, 0);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent2 = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT);

        builder.setSmallIcon(R.drawable.fetch_icon_notif)
                .setColor(Color.argb(255, 0, 150, 136))
                .setAutoCancel(true)
                .setDeleteIntent(pendingIntent1)
                .setLights(Color.WHITE, 500, 1500)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent2);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(12345, builder.build());
    }

    public static class OnDismissReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            clearNotifications(context);
        }
    }

    public static void clearNotifications(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(TEXTS_VISIBLE_IN_NOTIFICATION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    private static Set<Text> getVisibleTexts(Context context, Text text) {
        Set<Text> texts = new HashSet<>();

        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(TEXTS_VISIBLE_IN_NOTIFICATION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> dataSet = prefs.getStringSet(NOTIFICATIONS, new HashSet<String>());
        dataSet.add(Utils.bytesToHex(text.toBytes()));
        editor.putStringSet(NOTIFICATIONS, dataSet);
        editor.apply();

        for (String serializedData : dataSet) {
            texts.add(Text.fromBytes(Utils.hexToBytes(serializedData)));
        }

        return texts;
    }

    private static Bitmap drawableToBitmap (Drawable drawable) {
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
}