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
 * Utility class để monitor network status
 */
public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";
    private static NetworkMonitor instance;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isConnected = true;
    private NetworkStatusListener listener;

    public interface NetworkStatusListener {
        void onNetworkStatusChanged(boolean isConnected);
    }

    private NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        checkNetworkStatus(context);
        registerNetworkCallback(context);
    }

    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Kiểm tra trạng thái mạng hiện tại
     */
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
            // Deprecated API cho Android < M
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    /**
     * Kiểm tra trạng thái mạng và cập nhật
     */
    private void checkNetworkStatus(Context context) {
        boolean wasConnected = isConnected;
        isConnected = isNetworkAvailable();

        if (wasConnected != isConnected) {
            Log.d(TAG, "Network status changed: " + (isConnected ? "ONLINE" : "OFFLINE"));
            if (listener != null) {
                listener.onNetworkStatusChanged(isConnected);
            }
        }
    }

    /**
     * Đăng ký network callback để lắng nghe thay đổi
     */
    private void registerNetworkCallback(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    boolean wasConnected = isConnected;
                    isConnected = true;
                    Log.d(TAG, "Network available - ONLINE");
                    if (listener != null && !wasConnected) {
                        listener.onNetworkStatusChanged(true);
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    boolean wasConnected = isConnected;
                    isConnected = false;
                    Log.d(TAG, "Network lost - OFFLINE");
                    if (listener != null && wasConnected) {
                        listener.onNetworkStatusChanged(false);
                    }
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);
                    checkNetworkStatus(context);
                }
            };

            NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    /**
     * Đăng ký listener để nhận thông báo khi network status thay đổi
     */
    public void setNetworkStatusListener(NetworkStatusListener listener) {
        this.listener = listener;
        // Gửi trạng thái hiện tại ngay lập tức
        if (listener != null) {
            listener.onNetworkStatusChanged(isConnected);
        }
    }

    /**
     * Hủy đăng ký listener
     */
    public void removeNetworkStatusListener() {
        this.listener = null;
    }

    /**
     * Unregister network callback (gọi khi không cần dùng nữa)
     */
    public void unregister() {
        if (networkCallback != null && connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }

    /**
     * Get current connection status
     */
    public boolean getIsConnected() {
        return isConnected;
    }
}

