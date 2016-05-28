package com.xlythe.sms.pojo;

import android.content.Context;
import android.net.Uri;

public class Sticker {
    private final int mThumbnailResId;
    private final int mRawResId;

    public Sticker(int thumbnailResId, int rawResId) {
        mThumbnailResId = thumbnailResId;
        mRawResId = rawResId;
    }

    public int getThumbnail() {
        return mThumbnailResId;
    }

    public Uri getUri(Context context) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + mRawResId);
    }
}
