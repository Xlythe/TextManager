package com.xlythe.textmanager.text.service;

import android.app.IntentService;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.util.MessageUtils;

public class HeadlessSmsSendService extends IntentService {

    public HeadlessSmsSendService() {
        super(HeadlessSmsSendService.class.getSimpleName());
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (!TelephonyManager.ACTION_RESPOND_VIA_MESSAGE.equals(action)) {
            return;
        }

        Text text = MessageUtils.parse(this, intent);
        if (text != null) {
            TextManager.getInstance(this).send(text);
        }
    }
}
