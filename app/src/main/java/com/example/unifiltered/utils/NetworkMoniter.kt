package com.example.unifiltered.utils // Update to match your package

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NetworkMonitor(context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // This Flow will emit 'true' when online, and 'false' when offline
    val isConnected: Flow<Boolean> = callbackFlow {

        // 1. Define what happens when the network changes
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true) // Internet is back!
            }

            override fun onLost(network: Network) {
                trySend(false) // Connection dropped!
            }
        }

        // 2. Tell Android what kind of network we care about (Internet access)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // 3. Start listening
        connectivityManager.registerNetworkCallback(request, callback)

        // 4. Send the initial state right when the app opens
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(hasInternet)

        // 5. Clean up when the app closes
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}