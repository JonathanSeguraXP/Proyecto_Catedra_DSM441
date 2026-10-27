package com.example.the_white_smile.domain

/** Permite consultar si hay conexion sin depender del Context de Android. */
interface ConnectivityChecker {

    fun isOnline(): Boolean
}
