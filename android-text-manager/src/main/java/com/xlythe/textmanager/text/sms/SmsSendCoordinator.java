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
        send(request, false, true);
    }

    /** Sends each segment independently for carriers without concatenated SMS support. */
    public void send(SmsSendRequest request, boolean separateParts, boolean deliveryReports) {
        List<String> parts = gateway.divideMessage(request.getBody());
        if (parts == null || parts.isEmpty()) {
            throw new IllegalStateException("SMS gateway returned no message parts");
        }
        int partCount = parts.size();
        if (callbackFactory != null) callbackFactory.initialize(request, partCount);
        List<C> sent = new ArrayList<>(partCount);
        List<C> delivered = new ArrayList<>(partCount);
        for (int index = 0; index < partCount; index++) {
            C sentCallback = callbackFactory == null ? null : callbackFactory.createSent(request, index, partCount);
            C deliveredCallback = callbackFactory == null || !deliveryReports ? null
                    : callbackFactory.createDelivered(request, index, partCount);
            if (separateParts || partCount == 1) {
                gateway.sendText(request.getRecipient(), parts.get(index), sentCallback, deliveredCallback);
            } else {
                sent.add(sentCallback);
                delivered.add(deliveredCallback);
            }
        }
        if (partCount > 1 && !separateParts) {
            gateway.sendMultipartText(request.getRecipient(), parts, sent, delivered);
        }
    }
}
