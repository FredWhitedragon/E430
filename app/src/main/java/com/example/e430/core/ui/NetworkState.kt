package com.example.e430.core.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@Composable
fun rememberIsActiveNetworkMetered(): State<Boolean> {
    val context = LocalContext.current.applicationContext
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    return produceState(initialValue = connectivityManager.isActiveNetworkMetered, connectivityManager) {
        callbackFlow {
            fun sendCurrent() {
                trySend(connectivityManager.isActiveNetworkMetered)
            }
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = sendCurrent()
                override fun onLost(network: Network) = sendCurrent()
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = sendCurrent()
            }
            connectivityManager.registerDefaultNetworkCallback(callback)
            sendCurrent()
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.collect { value = it }
    }
}
