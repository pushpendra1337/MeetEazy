package net.intensecorp.meeteazy.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import java.io.IOException;
import java.util.Calendar;

public class NetworkInfoUtility {

    public static final String COMMAND_PING_GOOGLE = "/system/bin/ping -c 1 8.8.8.8";
    private static final String TAG = NetworkInfoUtility.class.getSimpleName();
    private final Context mContext;

    public NetworkInfoUtility(Context context) {
        this.mContext = context;
    }

    public boolean isConnectedToInternet() {

        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();

            if (network != null) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);

                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return isOnline();
                }
            }
        }
        return false;
    }

    private boolean isOnline() {
        long t1 = Calendar.getInstance().getTimeInMillis();

        Runtime runtime = Runtime.getRuntime();
        try {
            // Ping to Google server
            Process ipProcess = runtime.exec(COMMAND_PING_GOOGLE);
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "Ping failed: " + e.getMessage());
        } finally {
            long t2 = Calendar.getInstance().getTimeInMillis();
            Log.i(TAG, "Network check time: " + (t2 - t1) + "ms");
        }
        return false;
    }
}
