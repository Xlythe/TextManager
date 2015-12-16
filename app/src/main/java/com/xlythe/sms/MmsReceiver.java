package com.xlythe.sms;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class MmsReceiver extends com.xlythe.textmanager.text.MmsReceiver {
    private Context mContext;
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        mContext = context;
        onMessageReceived(new OnReceive() {
            @Override
            public void onSuccess(Bitmap bitmap) {

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(mContext)
                                .setLargeIcon(bitmap)
                                .setSmallIcon(R.drawable.user_icon)
                                .setContentTitle("")
                                .setContentText("picture");
                // Creates an explicit intent for an Activity in your app
                Intent resultIntent = new Intent(mContext, ManagerActivity.class);

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(ManagerActivity.class);
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                // mId allows you to update the notification later on.
                mNotificationManager.notify(12345, mBuilder.build());
            }
        });

    }
}
