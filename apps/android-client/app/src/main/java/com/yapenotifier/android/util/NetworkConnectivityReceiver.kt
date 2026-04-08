package com.yapenotifier.android.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles network connectivity changes and triggers service rebind when connectivity is restored.
 *
 * This receiver works in two modes:
 * 1. For API 24+: Uses NetworkCallback for more reliable detection
 * 2. For API 23-: Uses BroadcastReceiver with CONNECTIVITY_ACTION
 *
 * When network connectivity is restored and the NotificationListenerService is disconnected,
 * this receiver will attempt to rebind the service automatically.
 */
class NetworkConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION ||
            intent.action == "android.net.conn.CONNECTIVITY_CHANGE") {

            Log.d(TAG, "Received connectivity change broadcast")

            // Check if we now have connectivity
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val hasConnectivity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(network)
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnected == true
            }

            if (hasConnectivity) {
                Log.i(TAG, "Network connectivity restored - checking service state")
                FileLogger.log("SYSTEM", "Network connectivity restored", "info")
                checkAndRebindService(context)
            } else {
                Log.d(TAG, "Network disconnected")
                FileLogger.log("SYSTEM", "Network disconnected", "info")
            }
        }
    }

    /**
     * Check if service needs rebinding and attempt it if necessary.
     */
    private fun checkAndRebindService(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                // Safety net: ensure ServiceStatusManager is hydrated
                ServiceStatusManager.init(context)

                // Small delay to let network stabilize
                delay(REBIND_DELAY_MS)

                val isConnected = ServiceStatusManager.isServiceConnected()
                val hasPermission = NotificationAccessChecker.isNotificationAccessEnabled(context)

                Log.d(TAG, "Service check - connected=$isConnected, hasPermission=$hasPermission")

                if (!isConnected && hasPermission) {
                    Log.i(TAG, "Service disconnected but has permission - attempting rebind")
                    FileLogger.logReconnect(
                        success = false,
                        attempt = ServiceStatusManager.incrementRebindAttempt(),
                        delayMs = REBIND_DELAY_MS
                    )

                    val success = ServiceRebinder.rebindNotificationListener(context)

                    if (success) {
                        // Wait and check
                        delay(VERIFICATION_DELAY_MS)
                        val connectedAfterRebind = ServiceStatusManager.isServiceConnected()

                        if (connectedAfterRebind) {
                            Log.i(TAG, "Service successfully reconnected after network restore")
                            FileLogger.logReconnect(
                                success = true,
                                attempt = ServiceStatusManager.getRebindAttempts(),
                                delayMs = VERIFICATION_DELAY_MS
                            )
                            ServiceStatusManager.resetRebindAttempts()
                        } else {
                            Log.w(TAG, "Service still disconnected after rebind attempt")
                        }
                    } else {
                        Log.e(TAG, "Rebind request failed")
                        FileLogger.log("ERROR", "NetworkReceiver: Rebind request failed", "error")
                    }
                } else if (isConnected) {
                    Log.d(TAG, "Service already connected - no action needed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in rebind check", e)
                FileLogger.logError("NetworkConnectivityReceiver", e)
            }
        }
    }

    companion object {
        private const val TAG = "NetworkConnectivityRcvr"

        // Delay before attempting rebind after network restore
        private const val REBIND_DELAY_MS = 2000L

        // Delay to verify service connected after rebind
        private const val VERIFICATION_DELAY_MS = 3000L

        /**
         * For API 24+, we use NetworkCallback instead of BroadcastReceiver.
         * This provides more reliable network state detection.
         */
        private var networkCallback: ConnectivityManager.NetworkCallback? = null

        /**
         * Register the network callback for API 24+.
         * Call this from Application.onCreate()
         */
        fun registerNetworkCallback(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                // Unregister any existing callback first
                unregisterNetworkCallback(context)

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    @Volatile
                    private var wasAvailable = false

                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "NetworkCallback: onAvailable")

                        // Only trigger rebind if this is a RESTORATION of connectivity
                        // (not the initial callback when registering)
                        if (!wasAvailable) {
                            wasAvailable = true
                            // Skip first availability (registration)
                            if (ServiceStatusManager.getRebindAttempts() > 0 ||
                                !ServiceStatusManager.isServiceConnected()) {
                                handleNetworkRestored(context)
                            }
                        } else {
                            handleNetworkRestored(context)
                        }
                    }

                    override fun onLost(network: Network) {
                        Log.d(TAG, "NetworkCallback: onLost")
                        wasAvailable = false
                        FileLogger.log("SYSTEM", "Network lost (callback)", "info")
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        val hasInternet = networkCapabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_INTERNET
                        )
                        Log.d(TAG, "NetworkCallback: capabilities changed, hasInternet=$hasInternet")
                    }
                }

                try {
                    cm.registerNetworkCallback(request, networkCallback!!)
                    Log.i(TAG, "NetworkCallback registered successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register NetworkCallback", e)
                }
            }
        }

        /**
         * Unregister the network callback.
         * Call this when no longer needed (e.g., Application.onTerminate())
         */
        fun unregisterNetworkCallback(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkCallback?.let { callback ->
                    try {
                        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        cm.unregisterNetworkCallback(callback)
                        Log.d(TAG, "NetworkCallback unregistered")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unregistering NetworkCallback", e)
                    }
                }
                networkCallback = null
            }
        }

        /**
         * Handle network restoration - attempt service rebind if needed.
         */
        private fun handleNetworkRestored(context: Context) {
            Log.i(TAG, "Network restored - checking service state")
            FileLogger.log("SYSTEM", "Network restored (callback)", "info")

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                try {
                    // Safety net: ensure ServiceStatusManager is hydrated
                    ServiceStatusManager.init(context)

                    // Delay to let network stabilize
                    delay(REBIND_DELAY_MS)

                    val isConnected = ServiceStatusManager.isServiceConnected()
                    val hasPermission = NotificationAccessChecker.isNotificationAccessEnabled(context)

                    if (!isConnected && hasPermission) {
                        Log.i(TAG, "Attempting service rebind after network restore")
                        FileLogger.logReconnect(
                            success = false,
                            attempt = ServiceStatusManager.incrementRebindAttempt(),
                            delayMs = REBIND_DELAY_MS
                        )

                        val success = ServiceRebinder.rebindNotificationListener(context)

                        if (success) {
                            delay(VERIFICATION_DELAY_MS)
                            val connectedAfterRebind = ServiceStatusManager.isServiceConnected()

                            if (connectedAfterRebind) {
                                Log.i(TAG, "Service reconnected successfully")
                                FileLogger.logReconnect(
                                    success = true,
                                    attempt = ServiceStatusManager.getRebindAttempts(),
                                    delayMs = VERIFICATION_DELAY_MS
                                )
                                ServiceStatusManager.resetRebindAttempts()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling network restore", e)
                }
            }
        }

        /**
         * Check if network is currently available.
         */
        fun isNetworkAvailable(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnected == true
            }
        }
    }
}
