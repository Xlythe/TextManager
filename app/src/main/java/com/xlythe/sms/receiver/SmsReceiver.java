package com.xlythe.sms.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.xlythe.sms.MainActivity;
import com.xlythe.sms.R;
import com.xlythe.textmanager.text.Text;

public class SmsReceiver extends com.xlythe.textmanager.text.TextReceiver {

    @Override
    public void onMessageReceived(Context context, NotificationText text) {
        Intent dismissIntent = new Intent(context, MainActivity.class);
        PendingIntent piDismiss = PendingIntent.getService(context, 0, dismissIntent, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.user_icon)
                        .setContentTitle(text.getSender())
                        .setContentText(text.getMessage())
                        .setAutoCancel(true)
                        .setLights(Color.WHITE, 500, 1500)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .addAction(R.mipmap.ic_launcher, "Reply", piDismiss);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("X new messages");
        inboxStyle.addLine(text.getMessage());
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
