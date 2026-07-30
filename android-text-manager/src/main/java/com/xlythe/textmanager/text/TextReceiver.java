package com.xlythe.textmanager.text;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;

import com.xlythe.textmanager.text.util.ContentType;

import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.SMS_DELIVER_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.getMessagesFromIntent;

public abstract class TextReceiver extends BroadcastReceiver {
    private static final String TAG = TextReceiver.class.getSimpleName();
    public static final String ACTION_TEXT_RECEIVED = "com.xlythe.textmanager.text.ACTION_TEXT_RECEIVED";
    public static final String EXTRA_TEXT = "text";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive{action=" + intent.getAction() + "}");
        if (ACTION_TEXT_RECEIVED.equals(intent.getAction())) {
            onMessageReceived(context, intent.getParcelableExtra(EXTRA_TEXT));
        } else if (WAP_PUSH_DELIVER_ACTION.equals(intent.getAction()) && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            MmsReceiveService.schedule(context, intent);
        } else if (SMS_DELIVER_ACTION.equals(intent.getAction())) {
            SmsMessage[] messages = getMessagesFromIntent(intent);
            Text text = Receive.storeMessage(context, messages, 0);
            onMessageReceived(context, text);
        }
    }

    public abstract void onMessageReceived(Context context, Text text);
}
