package com.xlythe.textmanager.text.sms;

/** Immutable, validated data required to dispatch an SMS. */
public final class SmsSendRequest {
    private final String messageId;
    private final String recipient;
    private final String body;

    public SmsSendRequest(String messageId, String recipient, String body) {
        this.messageId = requireNotBlank(messageId, "messageId");
        this.recipient = requireNotBlank(recipient, "recipient");
        this.body = requireNotBlank(body, "body");
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

    private static String requireNotBlank(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
