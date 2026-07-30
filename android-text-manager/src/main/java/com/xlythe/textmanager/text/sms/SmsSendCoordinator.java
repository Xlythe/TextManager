package com.xlythe.textmanager.text.sms;

import java.util.ArrayList;
import java.util.List;

/** Coordinates segmentation and dispatch independently from Android framework classes. */
public final class SmsSendCoordinator<C> {
    private final SmsGateway<C> gateway;
    private final SmsCallbackFactory<C> callbackFactory;

    public SmsSendCoordinator(SmsGateway<C> gateway, SmsCallbackFactory<C> callbackFactory) {
        this.gateway = gateway;
        this.callbackFactory = callbackFactory;
    }

    public void send(SmsSendRequest request) {
        List<String> parts = gateway.divideMessage(request.getBody());
        int partCount = parts.size();
        if (partCount > 1) {
            List<C> sentCallbacks = new ArrayList<>();
            List<C> deliveredCallbacks = new ArrayList<>();
            sentCallbacks.add(callbackFactory.createSent(request, 0, partCount));
            deliveredCallbacks.add(callbackFactory.createDelivered(request, 0, partCount));
            gateway.sendMultipartText(
                    request.getRecipient(), parts, sentCallbacks, deliveredCallbacks);
        } else {
            gateway.sendText(
                    request.getRecipient(),
                    request.getBody(),
                    callbackFactory.createSent(request, 0, partCount),
                    callbackFactory.createDelivered(request, 0, partCount));
        }
    }
}
