package com.xlythe.sms.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.xlythe.sms.MainActivity;
import com.xlythe.sms.R;
import com.xlythe.textmanager.text.Text;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Niko on 2/25/16.
 */
public class Notifications {
    public static void buildNotification(Context context, Text text){
        Set<Text> texts = new HashSet<>();

        SharedPreferences prefs = context.getSharedPreferences("texts_visible_in_notification", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        Set<String> jsonSet = prefs.getStringSet("notifications", new HashSet<String>());
        jsonSet.add(gson.toJson(text));
        editor.putStringSet("notifications", jsonSet);
        editor.apply();

        for (String json: jsonSet) {
            texts.add(gson.fromJson(json, Text.class));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        // Only one message, can be text or Image/Video thumbnail
        if (texts.size() == 1) {
            Text txt = texts.iterator().next();
            if (txt.getAttachment() == null) {
                builder.setSmallIcon(R.drawable.user_icon)
                        .setContentTitle(txt.getSender().getDisplayName())
                        .setContentText(txt.getBody());

                inboxStyle.setBigContentTitle(txt.getSender().getDisplayName())
                        .addLine(txt.getBody());
            } else {
                builder.setSmallIcon(R.drawable.user_icon)
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
            builder.setSmallIcon(R.drawable.user_icon)
                    .setContentTitle(texts.size() + " new messages")
                    .setContentText(TextUtils.join(", ", names));
        }

        builder.setAutoCancel(true)
                .setLights(Color.WHITE, 500, 1500)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setStyle(inboxStyle);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(12345, builder.build());
    }
}