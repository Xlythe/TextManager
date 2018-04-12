package com.xlythe.sms.notification;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

public class NotificationMessage {
    private final NotificationThread notificationThread;
    private final String id;
    private final Bitmap icon;
    private final String sender;
    @Nullable
    private final Bitmap image;
    private final String body;

    private NotificationMessage(
            NotificationThread notificationThread,
            String id,
            Bitmap icon,
            String sender,
            @Nullable Bitmap image,
            String body) {
        this.notificationThread = notificationThread;
        this.id = id;
        this.icon = icon;
        this.sender = sender;
        this.image = image;
        this.body = body;
    }

    public static NotificationMessage fromBytes(byte[] bytes) {
        return new NotificationMessage(null, null, null, null, null, null);
    }

    public byte[] toBytes() {
        return new byte[0];
    }

    public NotificationThread getNotificationThread() {
        return notificationThread;
    }

    public String getId() {
        return id;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public String getSender() {
        return sender;
    }

    @Nullable
    public Bitmap getImage() {
        return image;
    }

    public String getBody() {
        return body;
    }

    public static final class Builder {
        private final NotificationThread notificationThread;
        private String id;
        private Bitmap icon;
        private String sender;
        @Nullable
        private Bitmap image;
        private String body;

        public Builder(NotificationThread notificationThread) {
            this.notificationThread = notificationThread;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sender(String sender) {
            this.sender = sender;
            return this;
        }

        public Builder icon(Bitmap icon) {
            this.icon = icon;
            return this;
        }

        public Builder image(@Nullable Bitmap bitmap) {
            this.image = bitmap;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public NotificationMessage build() {
            return new NotificationMessage(notificationThread, id, icon, sender, image, body);
        }
    }
}
