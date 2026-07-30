package com.xlythe.textmanager.text.sms;

/** Creates transport callbacks without coupling the SMS core to Android pending intents. */
public interface SmsCallbackFactory<C> {
    void initialize(SmsSendRequest request, int partCount);
    C createSent(SmsSendRequest request, int partIndex, int partCount);

    C createDelivered(SmsSendRequest request, int partIndex, int partCount);
}
