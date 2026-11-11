package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/**
 * NetworkMonitor monitors live internet connectivity using Android's ConnectivityManager.
 * It provides a hot StateFlow<Boolean> indicating whether an active and validated internet connection is present.
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE.let { Context.CONNECTIVITY_SERVICE }) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        private val connectedNetworks = mutableSetOf<Network>()

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            if (hasInternet) {
                connectedNetworks.add(network)
                _isOnline.value = true
                Log.d("NetworkMonitor", "Ağ bağlantısı sağlandı: $network (Toplam aktif ağ: ${connectedNetworks.size})")
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            connectedNetworks.remove(network)
            _isOnline.value = connectedNetworks.isNotEmpty() || checkInitialConnectivity()
            Log.d("NetworkMonitor", "Ağ bağlantısı koptu: $network (Çevrimiçi mi: ${_isOnline.value})")
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (hasInternet) {
                connectedNetworks.add(network)
                _isOnline.value = true
            }
        }
    }

    init {
        registerCallback()
    }

    private fun registerCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.w("NetworkMonitor", "NetworkCallback kaydı hatası: ${e.message}")
        }
    }

    fun checkInitialConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Flow-based observer for coroutines
     */
    fun observeNetworkState(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, callback)
        trySend(checkInitialConnectivity())

        awaitClose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignore unregister errors
            }
        }
    }.distinctUntilChanged()
}
