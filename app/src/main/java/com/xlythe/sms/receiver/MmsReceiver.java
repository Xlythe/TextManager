package com.xlythe.sms.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.xlythe.sms.MainActivity;
import com.xlythe.sms.R;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.ImageAttachment;
import com.xlythe.textmanager.text.Text;

import java.io.IOException;

public class MmsReceiver extends com.xlythe.textmanager.text.TextReceiver {

    @Override
    public void onMessageReceived(Context context, Text text) {
        Notifications.buildNotification(context, text);
    }
}