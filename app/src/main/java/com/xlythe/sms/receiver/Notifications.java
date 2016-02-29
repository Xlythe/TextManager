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
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.xlythe.sms.MainActivity;
import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Niko on 2/25/16.
 */
public class Notifications {
    public static final String TEXTS_VISIBLE_IN_NOTIFICATION = "texts_visible_in_notification";
    public static final String NOTIFICATIONS = "notifications";

    public static void buildNotification(Context context, Text text){
        Set<Text> texts = new HashSet<>();

        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(TEXTS_VISIBLE_IN_NOTIFICATION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        Set<String> jsonSet = prefs.getStringSet(NOTIFICATIONS, new HashSet<String>());
        jsonSet.add(gson.toJson(text));
        editor.putStringSet(NOTIFICATIONS, jsonSet);
        editor.apply();

        for (String json: jsonSet) {
            texts.add(gson.fromJson(json, Text.class));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();


        // Maybe make a helper class for this
        String address = "";
        Uri uri = null;
        for (Contact member: text.getMembersExceptMe(context)) {
            // TODO: Fix icon for group messaging
            if (!address.isEmpty()) {
                address += ", ";
            }
            address += member.getDisplayName();
            uri = member.getPhotoUri();
        }
        int color = ColorUtils.getColor(Long.parseLong(text.getThreadId()));
        ProfileDrawable icon = new ProfileDrawable(context,
                address.charAt(0),
                color,
                uri);

        // Only one message, can be text or Image/Video thumbnail
        if (texts.size() == 1) {
            Text txt = texts.iterator().next();
            if (txt.getAttachment() == null) {
                builder.setLargeIcon(drawableToBitmap(icon))
                        .setContentTitle(txt.getSender().getDisplayName())
                        .setContentText(txt.getBody());

                inboxStyle.setBigContentTitle(txt.getSender().getDisplayName())
                        .addLine(txt.getBody());
            } else {
                builder.setLargeIcon(drawableToBitmap(icon))
                        .setContentTitle(txt.getSender().getDisplayName())
                        .setContentText(txt.getBody());
                inboxStyle.setBigContentTitle(txt.getSender().getDisplayName())
                        .addLine(txt.getBody());
            }
        }

        // Multiple messages, should all look the same unless its only one conversation
        else {
            Set<String> names = new HashSet<>();
            inboxStyle.setBigContentTitle(texts.size() + " new messages");
            for (Text txt: texts) {
                inboxStyle.addLine(Html.fromHtml("<b>" + txt.getSender().getDisplayName() + " </b>" + txt.getBody()));
                names.add(txt.getSender().getDisplayName());
            }
            builder.setContentTitle(texts.size() + " new messages")
                    .setContentText(TextUtils.join(", ", names));
        }

        Intent intent = new Intent(context, OnDismissReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 12345, intent, 0);

        builder.setSmallIcon(R.drawable.fetch_icon_notif)
                .setColor(Color.argb(255, 0, 150, 136))
                .setAutoCancel(true)
                .setDeleteIntent(pendingIntent)
                .setLights(Color.WHITE, 500, 1500)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setStyle(inboxStyle);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(12345, builder.build());
    }

    public static class OnDismissReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(TEXTS_VISIBLE_IN_NOTIFICATION, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
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