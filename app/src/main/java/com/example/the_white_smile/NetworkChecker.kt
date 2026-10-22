package com.example.the_white_smile

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Verifica si el dispositivo tiene conexión a internet, para evitar
 * llamadas a Firebase que van a fallar sin conexión.
 */
object NetworkChecker {

    fun isOnline(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
