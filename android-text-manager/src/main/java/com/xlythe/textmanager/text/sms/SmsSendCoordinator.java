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
        if (parts == null || parts.isEmpty()) {
            throw new IllegalStateException("SMS gateway returned no message parts");
        }
        int partCount = parts.size();
        callbackFactory.initialize(request, partCount);
        if (partCount > 1) {
            List<C> sentCallbacks = new ArrayList<>(partCount);
            List<C> deliveredCallbacks = new ArrayList<>(partCount);
            for (int partIndex = 0; partIndex < partCount; partIndex++) {
                sentCallbacks.add(callbackFactory.createSent(request, partIndex, partCount));
                deliveredCallbacks.add(
                        callbackFactory.createDelivered(request, partIndex, partCount));
            }
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
