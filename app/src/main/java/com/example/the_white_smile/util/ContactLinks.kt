package com.example.the_white_smile.util

import java.net.URLEncoder

/**
 * Construye los enlaces que abren las aplicaciones externas de telefono,
 * WhatsApp, correo y mapas. Son cadenas puras para poder probarlas sin
 * depender de Android.
 */
object ContactLinks {

    /** Enlace para el marcador telefonico, conservando el prefijo internacional. */
    fun tel(phone: String): String {
        val clean = phone.trim()
        val prefix = if (clean.startsWith("+")) "+" else ""
        return "tel:$prefix${digits(clean)}"
    }

    /** Enlace de WhatsApp con el mensaje ya codificado. */
    fun whatsapp(phone: String, message: String): String =
        "https://wa.me/${digits(phone)}?text=${encode(message)}"

    /** Enlace mailto con el asunto predefinido. */
    fun email(address: String, subject: String): String =
        "mailto:${address.trim()}?subject=${encode(subject)}"

    /** Consulta de mapas por texto, sin coordenadas fijas. */
    fun map(query: String): String = "geo:0,0?q=${encode(query)}"

    private fun digits(phone: String): String =
        phone.filter { it.isDigit() }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
