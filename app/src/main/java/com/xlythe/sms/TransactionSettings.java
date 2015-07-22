package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

/**
 * Container of transaction settings. Instances of this class are contained
 * within Transaction instances to allow overriding of the default APN
 * settings or of the MMS Client.
 */
public class TransactionSettings {
    private static final String TAG = "Transaction Settings";

    private String mServiceCenter;
    private String mProxyAddress;
    private int mProxyPort = -1;

    private static final String[] APN_PROJECTION = {
            Telephony.Carriers.TYPE,            // 0
            Telephony.Carriers.MMSC,            // 1
            Telephony.Carriers.MMSPROXY,        // 2
            Telephony.Carriers.MMSPORT          // 3
    };
    private static final int COLUMN_TYPE         = 0;
    private static final int COLUMN_MMSC         = 1;
    private static final int COLUMN_MMSPROXY     = 2;
    private static final int COLUMN_MMSPORT      = 3;

    /**
     * Constructor that uses the default settings of the MMS Client.
     *
     * @param context The context of the MMS Client
     */
    public TransactionSettings(Context context, String apnName) {
        Log.v(TAG, "TransactionSettings: apnName: " + apnName);
        String selection = Telephony.Carriers.CURRENT + " IS NOT NULL";
        String[] selectionArgs = null;
        if (!TextUtils.isEmpty(apnName)) {
            selection += " AND " + Telephony.Carriers.APN + "=?";
            selectionArgs = new String[]{ apnName.trim() };
        }

        Cursor cursor = context.getContentResolver().query(Telephony.Carriers.CONTENT_URI,
                APN_PROJECTION, selection, selectionArgs, null);

        Log.v(TAG, "TransactionSettings looking for apn: " + selection + " returned: " +
                    (cursor ==null ? "null cursor" : (cursor.getCount() + " hits")));

        if (cursor == null) {
            Log.e(TAG, "Apn is not found in Database!");
            return;
        }

        boolean sawValidApn = false;
        try {
            while (cursor.moveToNext() && TextUtils.isEmpty(mServiceCenter)) {
                // Read values from APN settings
                if (isValidApnType(cursor.getString(COLUMN_TYPE), PhoneConstants.APN_TYPE_MMS)) {
                    sawValidApn = true;

                    String mmsc = cursor.getString(COLUMN_MMSC);
                    if (mmsc == null) {
                        continue;
                    }

                    mServiceCenter = NetworkUtils.trimV4AddrZeros(mmsc.trim());
                    mProxyAddress = NetworkUtils.trimV4AddrZeros(cursor.getString(COLUMN_MMSPROXY));
                    if (isProxySet()) {
                        String portString = cursor.getString(COLUMN_MMSPORT);
                        try {
                            mProxyPort = Integer.parseInt(portString);
                        } catch (NumberFormatException e) {
                            if (TextUtils.isEmpty(portString)) {
                                Log.w(TAG, "mms port not set!");
                            } else {
                                Log.e(TAG, "Bad port number format: " + portString, e);
                            }
                        }
                    }
                }
            }
        } finally {
            cursor.close();
        }

        Log.v(TAG, "APN setting: MMSC: " + mServiceCenter + " looked for: " + selection);

        if (sawValidApn && TextUtils.isEmpty(mServiceCenter)) {
            Log.e(TAG, "Invalid APN setting: MMSC is empty");
        }
    }

    /**
     * Constructor that overrides the default settings of the MMS Client.
     *
     * @param mmscUrl The MMSC URL
     * @param proxyAddr The proxy address
     * @param proxyPort The port used by the proxy address
     * immediately start a SendTransaction upon completion of a NotificationTransaction,
     * false otherwise.
     */
    public TransactionSettings(String mmscUrl, String proxyAddr, int proxyPort) {
        mServiceCenter = mmscUrl != null ? mmscUrl.trim() : null;
        mProxyAddress = proxyAddr;
        mProxyPort = proxyPort;

        Log.v(TAG, "TransactionSettings: " + mServiceCenter + " proxyAddress: " + mProxyAddress + " proxyPort: " + mProxyPort);
    }

    public String getMmscUrl() {
        return mServiceCenter;
    }

    public String getProxyAddress() {
        return mProxyAddress;
    }

    public int getProxyPort() {
        return mProxyPort;
    }

    public boolean isProxySet() {
        return (mProxyAddress != null) && (mProxyAddress.trim().length() != 0);
    }

    static private boolean isValidApnType(String types, String requestType) {
        // If APN type is unspecified, assume APN_TYPE_ALL.
        if (TextUtils.isEmpty(types)) {
            return true;
        }

        for (String t : types.split(",")) {
            if (t.equals(requestType) || t.equals(PhoneConstants.APN_TYPE_ALL)) {
                return true;
            }
        }
        return false;
    }
}
