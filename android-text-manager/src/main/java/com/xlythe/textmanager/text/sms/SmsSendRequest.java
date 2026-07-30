package com.xlythe.textmanager.text.sms;

/** Immutable data required to dispatch an SMS. */
public final class SmsSendRequest {
    private final String messageId;
    private final String recipient;
    private final String body;

    public SmsSendRequest(String messageId, String recipient, String body) {
        this.messageId = messageId;
        this.recipient = recipient;
        this.body = body;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getBody() {
        return body;
    }
}
