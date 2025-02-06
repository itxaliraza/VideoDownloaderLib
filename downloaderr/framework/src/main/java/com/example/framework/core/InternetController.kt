package com.example.framework.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


class InternetController(
    private val appContext: Context
) {
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isInternetConnected: Boolean
        get() {
            try {
                val network = connectivityManager.activeNetwork ?: return false
                val nc = connectivityManager.getNetworkCapabilities(network) ?: return false
                return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            } catch (_: Exception) {
            }
            return false
        }

    fun observeInternet() = callbackFlow {
        val builder = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.addTransportType(NetworkCapabilities.TRANSPORT_USB)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            builder.addTransportType(NetworkCapabilities.TRANSPORT_LOWPAN)
        }
        val callBack = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(false)
            }
        }

        connectivityManager.registerNetworkCallback(builder.build(), callBack)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callBack)
        }
    }

    fun getContext(): Context {
        return appContext
    }

}