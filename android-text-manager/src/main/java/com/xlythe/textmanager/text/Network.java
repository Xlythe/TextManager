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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Niko on 5/20/16.
 */
public class Network {
    public static void forceDataConnection(Context context, final Callback callback) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
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
        new java.lang.Thread(new Runnable() {
            public void run() {
                connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(android.net.Network network) {
                        super.onAvailable(network);
                        latch.countDown();
                        ConnectivityManager.setProcessDefaultNetwork(network);
                        callback.onSuccess();
                        connectivityManager.unregisterNetworkCallback(this);
                    }
                });
            }
        }).start();
        try {
            success = latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!success) {
            callback.onFail();
        }
    }

    private static void requestLegacy(Context context, final Callback callback) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final int result = connMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableMMS");

        if (result != 0) {
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

                    if (!mNetworkInfo.isConnected()) {
                        return;
                    } else {
                        new java.lang.Thread(new Runnable() {
                            public void run() {
                                callback.onSuccess();
                                latch.countDown();
                            }
                        }).start();
                    }
                }
            };
            context.registerReceiver(receiver, filter);
            try {
                success = latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!success) {
                callback.onFail();
            }
            context.unregisterReceiver(receiver);
        } else {
            new java.lang.Thread(new Runnable() {
                public void run() {
                    callback.onSuccess();
                }
            }).start();
        }
    }

    public interface Callback{
        void onSuccess();
        void onFail();
    }
}
