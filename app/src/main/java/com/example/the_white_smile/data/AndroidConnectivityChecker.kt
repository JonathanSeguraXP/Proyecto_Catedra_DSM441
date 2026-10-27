package com.example.the_white_smile.data

import android.content.Context
import com.example.the_white_smile.NetworkChecker
import com.example.the_white_smile.domain.ConnectivityChecker

/** Implementacion de [ConnectivityChecker] sobre el estado de red del sistema. */
class AndroidConnectivityChecker(context: Context) : ConnectivityChecker {

    private val appContext = context.applicationContext

    override fun isOnline(): Boolean = NetworkChecker.isOnline(appContext)
}
