package com.fptu.prm392.mad.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Utility class để theo dõi trạng thái kết nối mạng của thiết bị.
 */
public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";
    private static NetworkMonitor instance;

    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isConnected = true;
    private NetworkStatusListener listener;

    public interface NetworkStatusListener {
        void onNetworkStatusChanged(boolean isConnected);
    }

    private NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        checkNetworkStatus();
        registerNetworkCallback();
    }

    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // Deprecated APIs cho Android < M
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    private void checkNetworkStatus() {
        boolean previous = isConnected;
        isConnected = isNetworkAvailable();

        if (previous != isConnected) {
            Log.d(TAG, "Network status changed: " + (isConnected ? "ONLINE" : "OFFLINE"));
            if (listener != null) {
                listener.onNetworkStatusChanged(isConnected);
            }
        }
    }

    private void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    boolean previous = isConnected;
                    isConnected = true;
                    if (listener != null && !previous) {
                        listener.onNetworkStatusChanged(true);
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    boolean previous = isConnected;
                    isConnected = false;
                    if (listener != null && previous) {
                        listener.onNetworkStatusChanged(false);
                    }
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);
                    checkNetworkStatus();
                }
            };

            NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    public void setNetworkStatusListener(NetworkStatusListener listener) {
        this.listener = listener;
        if (listener != null) {
            listener.onNetworkStatusChanged(isConnected);
        }
    }

    public void removeNetworkStatusListener() {
        this.listener = null;
    }

    public void unregister() {
        if (networkCallback != null && connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }
}
