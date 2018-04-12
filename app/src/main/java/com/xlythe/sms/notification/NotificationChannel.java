package com.xlythe.sms.notification;

import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

public class NotificationChannel {
    private final String id;

    @DrawableRes
    private final int appIconRes;

    @ColorInt
    private final int accentColor;

    @StringRes
    private final int channelNameRes;

    @StringRes
    private final int channelDescriptionRes;

    private NotificationChannel(
            String id,
            @DrawableRes int appIcon,
            @ColorInt int accentColor,
            @StringRes int channelName,
            @StringRes int channelDescription) {
        this.id = id;
        this.appIconRes = appIcon;
        this.accentColor = accentColor;
        this.channelNameRes = channelName;
        this.channelDescriptionRes = channelDescription;
    }

    public String getId() {
        return id;
    }

    @DrawableRes
    public int getAppIcon() {
        return appIconRes;
    }

    @ColorInt
    public int getAccentColor() {
        return accentColor;
    }

    @StringRes
    public int getName() {
        return channelNameRes;
    }

    @StringRes
    public int getDescription() {
        return channelDescriptionRes;
    }

    public static final class Builder {
        private String id;
        @DrawableRes
        private int appIcon;
        @ColorInt
        private int accentColor;
        @StringRes
        private int channelName;
        @StringRes
        private int channelDescription;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder appIcon(@DrawableRes int appIcon) {
            this.appIcon = appIcon;
            return this;
        }

        public Builder accentColor(@ColorInt int color) {
            this.accentColor = color;
            return this;
        }

        public Builder channelName(@StringRes int resId) {
            this.channelName = resId;
            return this;
        }

        public Builder channelDescription(@StringRes int resId) {
            this.channelDescription = resId;
            return this;
        }

        public NotificationChannel build() {
            return new NotificationChannel(id, appIcon, accentColor, channelName, channelDescription);
        }
    }
}
