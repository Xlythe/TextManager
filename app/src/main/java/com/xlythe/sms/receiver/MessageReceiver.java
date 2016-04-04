package com.xlythe.sms.receiver;

import android.content.Context;

import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextReceiver;

public class MessageReceiver extends TextReceiver {
    @Override
    public void onMessageReceived(Context context, Text text) {
        Notifications.buildNotification(context, text);
    }
}