package com.xlythe.textmanager.text;

import static com.xlythe.textmanager.text.TextManager.TAG;

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
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.CheckResult;
import androidx.annotation.WorkerThread;

public class Network {
    private static final String ENABLE_MMS = "enableMMS";
    private static final int ALREADY_ACTIVE = 0;
    private static final int MOBILE_NETWORK_TIMEOUT_SEC = 10;

    @CheckResult
    @WorkerThread
    public static boolean forceDataConnection(Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            return request(context);
        } else {
            return requestLegacy(context);
        }
    }

    @WorkerThread
    @TargetApi(21)
    private static boolean request(Context context) {
        // Request a data connection
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        // Use a countdownlatch because this may never return, and we want to mark the MMS
        // as failed in that case.
        final CountDownLatch latch = new CountDownLatch(1);
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
            return latch.await(MOBILE_NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timed out waiting to switch to the mobile network", e);
        } finally {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }

        return false;
    }

    // startUsingNetworkFeature was removed from the SDK as of api 26+, so we have to use reflection
    // to access it on older versions.
    private static int startUsingNetworkFeature(ConnectivityManager connectivityManager, int type, String cmd) {
        try {
            return (int) ConnectivityManager.class.getMethod("startUsingNetworkFeature", Integer.TYPE, String.class)
                    .invoke(connectivityManager, type, cmd);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call ConnectivityManager.startUsingNetworkFeature", e);
        }
        return -1;
    }

    @WorkerThread
    private static boolean requestLegacy(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        int result = startUsingNetworkFeature(connectivityManager, ConnectivityManager.TYPE_MOBILE, ENABLE_MMS);

        if (result == ALREADY_ACTIVE) {
            return true;
        }

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        final CountDownLatch latch = new CountDownLatch(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                String action = intent.getAction();
                if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                    return;
                }

                NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (networkInfo == null || networkInfo.getType() != ConnectivityManager.TYPE_MOBILE_MMS) {
                    return;
                }

                if (networkInfo.isConnected()) {
                    latch.countDown();
                }
            }
        };
        context.registerReceiver(receiver, filter);
        try {
            return latch.await(MOBILE_NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timed out waiting to switch to the mobile network", e);
        } finally {
            context.unregisterReceiver(receiver);
        }

        return false;
    }
}
