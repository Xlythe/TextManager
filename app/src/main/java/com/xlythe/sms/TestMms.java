package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Niko on 7/22/15.
 */
public class TestMms {
    Context mContext;
//    Uri mUri;
//    boolean mLocked;
//
//    static final String[] PROJECTION = new String[] {
//            Telephony.Mms.CONTENT_LOCATION,
//            Telephony.Mms.LOCKED
//    };
//
//    // The indexes of the columns which must be consistent with above PROJECTION.
//    static final int COLUMN_CONTENT_LOCATION      = 0;
//    static final int COLUMN_LOCKED                = 1;

    public TestMms(Context context) {
        mContext = context;
    }

    /**
     * A common method to retrieve a PDU from MMSC.
     *
     * @return A byte array which contains the data of the PDU.
     *         If the status code is not correct, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] getPdu(String url) throws IOException {

        Log.d("TestMms", "getting");

        ApnDefaults.ApnParameters apnParameters = ApnDefaults.getApnParameters(mContext);
        Log.d("TestMms", apnParameters.getProxyAddress()+"");
        Log.d("TestMms", apnParameters.getProxyPort()+"");
        return HttpUtils.httpConnection(
                mContext, SendingProgressTokenManager.NO_TOKEN,
                url, null, HttpUtils.HTTP_GET_METHOD,
                false,
                apnParameters.getProxyAddress(),
                0);
    }

}
