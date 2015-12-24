package com.xlythe.sms.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.xlythe.sms.ManagerActivity;
import com.xlythe.sms.R;
import com.xlythe.textmanager.text.Text;

public class MmsReceiver extends com.xlythe.textmanager.text.TextReceiver {

    @Override
    public void onMessageReceived(Context context, Text text) {
        Intent dismissIntent = new Intent(context, ManagerActivity.class);
        PendingIntent piDismiss = PendingIntent.getService(context, 0, dismissIntent, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setLargeIcon(text.getAttachments().get(0))
                        .setSmallIcon(R.drawable.user_icon)
                        .setContentTitle(text.getAddress())
                        .setContentText(text.getBody())
                        .setAutoCancel(true)
                        .setLights(Color.WHITE, 500, 1500)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .addAction(R.mipmap.ic_launcher, "Reply", piDismiss);

        NotificationCompat.BigPictureStyle notiStyle = new NotificationCompat.BigPictureStyle();
        notiStyle.setBigContentTitle(text.getAddress());
        notiStyle.setSummaryText(text.getBody());
        notiStyle.bigPicture(text.getAttachments().get(0));
        builder.setStyle(notiStyle);


        Intent resultIntent = new Intent(context, ManagerActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(ManagerActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(12345, builder.build());
    }
}