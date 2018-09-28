package com.xlythe.textmanager.text;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.xlythe.textmanager.text.util.ContentType;

import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.SMS_DELIVER_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.getMessagesFromIntent;

public abstract class TextReceiver extends BroadcastReceiver {
    private static final String TAG = TextReceiver.class.getSimpleName();
    private static final String TAG_TEXT = "sms:text";

    private static final long TIMEOUT_TEXT = 1000;

    public static final String ACTION_TEXT_RECEIVED = "com.xlythe.textmanager.text.ACTION_TEXT_RECEIVED";
    public static final String EXTRA_TEXT = "text";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive{action=" + intent.getAction() + "}");
        if (ACTION_TEXT_RECEIVED.equals(intent.getAction())) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_TEXT);
            wl.acquire(TIMEOUT_TEXT);
            onMessageReceived(context, intent.getParcelableExtra(EXTRA_TEXT));
        } else if (WAP_PUSH_DELIVER_ACTION.equals(intent.getAction()) && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            MmsReceiveService.schedule(context, intent);
        } else if (SMS_DELIVER_ACTION.equals(intent.getAction())) {
            SmsMessage[] messages = getMessagesFromIntent(intent);
            Text text = Receive.storeMessage(context, messages, 0);
            onMessageReceived(context, text);
        } else if (Build.VERSION.SDK_INT < 19 && SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            // This is purely to display the notification! because main sms app stores the sms
            SmsMessage[] messages = getMessagesFromIntent(intent);
            onMessageReceived(context, new Text.Converter().toText(context, messages));
        }
    }

    public abstract void onMessageReceived(Context context, Text text);
}
