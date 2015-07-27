package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import java.io.IOException;

/**
 * Created by Niko on 7/22/15.
 */
public class TestMms {
    Context mContext;

    static final String[] PROJECTION = new String[] {
            Telephony.Mms.CONTENT_LOCATION,
            Telephony.Mms.LOCKED
    };

    static final int COLUMN_CONTENT_LOCATION = 0;

    public TestMms(Context context) {
        mContext = context;
    }

    protected byte[] getPdu(Uri uri) throws IOException {
        Cursor cursor = mContext.getContentResolver().query(uri, PROJECTION, null, null, null);

        String url = "";

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    url = cursor.getString(COLUMN_CONTENT_LOCATION);
                }
            } finally {
                cursor.close();
            }
        }

        ApnDefaults.ApnParameters apnParameters = ApnDefaults.getApnParameters(mContext);
        return HttpUtils.httpConnection(
                mContext, SendingProgressTokenManager.NO_TOKEN,
                url, null, HttpUtils.HTTP_GET_METHOD,
                apnParameters.isProxySet(),
                apnParameters.getProxyAddress(),
                apnParameters.getProxyPort());
    }
}
