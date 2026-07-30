package com.xlythe.textmanager.text.sms;

import java.util.List;

/** Platform SMS operations used by {@link SmsSendCoordinator}. */
public interface SmsGateway<C> {
    List<String> divideMessage(String body);

    void sendText(String recipient, String body, C sentCallback, C deliveredCallback);

    void sendMultipartText(
            String recipient,
            List<String> parts,
            List<C> sentCallbacks,
            List<C> deliveredCallbacks);
}
