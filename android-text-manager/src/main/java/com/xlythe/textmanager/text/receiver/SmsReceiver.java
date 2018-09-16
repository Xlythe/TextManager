package com.xlythe.textmanager.text.receiver;

import android.content.Context;
import android.content.Intent;

import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextReceiver;

public class SmsReceiver extends TextReceiver {
    @Override
    public void onMessageReceived(Context context, Text text) {
        Intent intent = new Intent(ACTION_TEXT_RECEIVED);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_TEXT, text);
        context.sendBroadcast(intent);
    }
}
