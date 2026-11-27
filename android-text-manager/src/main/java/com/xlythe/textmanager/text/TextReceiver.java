package com.xlythe.textmanager.text;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsMessage;
import android.util.Log;

import com.xlythe.textmanager.R;
import com.xlythe.textmanager.text.util.ContentType;

import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.SMS_DELIVER_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;
import static com.xlythe.textmanager.text.Mock.Telephony.Sms.Intents.getMessagesFromIntent;

public abstract class TextReceiver extends BroadcastReceiver {
    private static final String TAG = TextReceiver.class.getSimpleName();
    public static final String ACTION_TEXT_RECEIVED = "com.xlythe.textmanager.text.ACTION_TEXT_RECEIVED";
    public static final String ACTION_FLASH_SMS = "com.xlythe.textmanager.text.ACTION_FLASH_SMS";
    public static final String ACTION_MMS_REPORT = "com.xlythe.textmanager.text.ACTION_MMS_REPORT";
    public static final String EXTRA_TEXT = "text";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive{action=" + intent.getAction() + "}");
        if (ACTION_TEXT_RECEIVED.equals(intent.getAction())) {
            onMessageReceived(context, intent.getParcelableExtra(EXTRA_TEXT));
        } else if (ContentType.MMS_MESSAGE.equals(intent.getType()) && (WAP_PUSH_DELIVER_ACTION.equals(intent.getAction())
                || (Build.VERSION.SDK_INT < 19 && WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction())
                && context.getResources().getBoolean(R.bool.textmanager_handle_legacy_mms_push)))) {
            MmsReceiveService.schedule(context, intent);
        } else if (SMS_DELIVER_ACTION.equals(intent.getAction())
                || (Build.VERSION.SDK_INT < 19 && SMS_RECEIVED_ACTION.equals(intent.getAction()))) {
            SmsMessage[] messages = getMessagesFromIntent(intent);
            if (messages == null || messages.length == 0) return;
            for (SmsMessage message : messages) {
                if (message == null) return;
            }
            if (messages[0].getMessageClass() == SmsMessage.MessageClass.CLASS_0) {
                context.sendBroadcast(new Intent(ACTION_FLASH_SMS).setPackage(context.getPackageName())
                        .putExtra(EXTRA_TEXT, Receive.fromBroadcast(messages, -1)));
                return;
            }
            // Before KitKat the stock messaging app persists SMS_RECEIVED.
            Text text = Build.VERSION.SDK_INT < 19 ? Receive.fromBroadcast(context, messages)
                    : Receive.storeMessage(context, messages, 0, MmsReceiveService.getSubscriptionId(intent));
            if (text != null) onMessageReceived(context, text);
        }
    }

    public abstract void onMessageReceived(Context context, Text text);
}
