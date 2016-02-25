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

        if (texts.size() == 1) {
            //TODO: Ill finish up in the morning
        }


        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.user_icon)
                        .setContentTitle(text.getSender().getDisplayName())
                        .setContentText(text.getBody())
                        .setAutoCancel(true)
                        .setLights(Color.WHITE, 500, 1500)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setCategory(Notification.CATEGORY_MESSAGE);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("X new messages");
        inboxStyle.addLine(text.getBody());
        builder.setStyle(inboxStyle);

        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(12345, builder.build());
    }
}