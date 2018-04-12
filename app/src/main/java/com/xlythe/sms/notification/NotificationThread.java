package com.xlythe.sms.notification;

import android.app.PendingIntent;
import android.graphics.Bitmap;

public class NotificationThread {
    private final NotificationChannel notificationChannel;
    private final String id;
    private final Bitmap icon;
    private final PendingIntent onClickIntent;
    private final PendingIntent quickReplyIntent;

    private NotificationThread(
            NotificationChannel notificationChannel,
            String id,
            Bitmap icon,
            PendingIntent onClickIntent,
            PendingIntent quickReplyIntent) {
        this.notificationChannel = notificationChannel;
        this.id = id;
        this.icon = icon;
        this.onClickIntent = onClickIntent;
        this.quickReplyIntent = quickReplyIntent;
    }

    public NotificationChannel getNotificationChannel() {
        return notificationChannel;
    }

    public String getId() {
        return id;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public PendingIntent getOnClickIntent() {
        return onClickIntent;
    }

    public PendingIntent getOnQuickReplyIntent() {
        return quickReplyIntent;
    }

    public static final class Builder {
        private final NotificationChannel notificationChannel;
        private String id;
        private Bitmap icon;
        private PendingIntent onClickIntent;
        private PendingIntent quickReplyIntent;

        public Builder(NotificationChannel notificationChannel) {
            this.notificationChannel = notificationChannel;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder icon(Bitmap icon) {
            this.icon = icon;
            return this;
        }

        public Builder setOnClickIntent(PendingIntent pendingIntent) {
            this.onClickIntent = pendingIntent;
            return this;
        }

        public Builder setQuickReplyIntent(PendingIntent pendingIntent) {
            this.quickReplyIntent = pendingIntent;
            return this;
        }

        public NotificationThread build() {
            return new NotificationThread(notificationChannel, id, icon, onClickIntent, quickReplyIntent);
        }
    }
}
