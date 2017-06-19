package com.xlythe.textmanager.text;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Network {
    private static final String ENABLE_MMS = "enableMMS";
    private static final int ALREADY_ACTIVE = 0;
    private static final int TIMEOUT = 10;


    public static void forceDataConnection(Context context, final Callback callback) {
        if (Build.VERSION.SDK_INT >= 21) {
            request(context, callback);
        } else {
            requestLegacy(context, callback);
        }
    }

    @TargetApi(21)
    private static void request(Context context, final Callback callback) {
        // Request a data connection
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        // Use a countdownlatch because this may never return, and we want to mark the MMS
        // as failed in that case.
        final CountDownLatch latch = new CountDownLatch(1);
        boolean success = false;
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(android.net.Network network) {
                super.onAvailable(network);
                ConnectivityManager.setProcessDefaultNetwork(network);
                latch.countDown();
            }
        };
        connectivityManager.requestNetwork(networkRequest, networkCallback);
        try {
            success = latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (success) {
            callback.onSuccess();
        } else {
            callback.onFail();
        }
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    private static void requestLegacy(Context context, final Callback callback) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final int result = connectivityManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, ENABLE_MMS);

        if (result != ALREADY_ACTIVE) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            final CountDownLatch latch = new CountDownLatch(1);
            boolean success = false;
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, Intent intent) {
                    String action = intent.getAction();
                    if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        return;
                    }

                    NetworkInfo mNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

                    if ((mNetworkInfo == null) || (mNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE_MMS)) {
                        return;
                    }

                    if (mNetworkInfo.isConnected()) {
                        latch.countDown();
                    }
                }
            };
            context.registerReceiver(receiver, filter);
            try {
                success = latch.await(TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (success) {
                callback.onSuccess();
            } else {
                callback.onFail();
            }
            context.unregisterReceiver(receiver);
        } else {
            callback.onSuccess();
        }
    }

    public interface Callback{
        void onSuccess();
        void onFail();
    }
}
