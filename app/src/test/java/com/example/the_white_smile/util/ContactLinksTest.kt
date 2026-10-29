package com.example.the_white_smile.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas de los enlaces que abren las aplicaciones externas. */
class ContactLinksTest {

    @Test
    fun `el telefono conserva el prefijo internacional y descarta espacios y guiones`() {
        assertEquals("tel:+50312345678", ContactLinks.tel("+503 1234-5678"))
    }

    @Test
    fun `el telefono sin prefijo queda solo con los digitos`() {
        assertEquals("tel:22223344", ContactLinks.tel("2222-3344"))
    }

    @Test
    fun `whatsapp usa solo digitos y codifica el mensaje`() {
        assertEquals(
            "https://wa.me/50312345678?text=Hola%2C%20quiero%20una%20cita",
            ContactLinks.whatsapp("+503 1234-5678", "Hola, quiero una cita")
        )
    }

    @Test
    fun `el correo conserva la direccion y codifica el asunto`() {
        assertEquals(
            "mailto:contacto@thewhitesmile.com?subject=Consulta%20desde%20The%20White%20Smile",
            ContactLinks.email(
                "contacto@thewhitesmile.com",
                "Consulta desde The White Smile"
            )
        )
    }

    @Test
    fun `el mapa codifica la direccion como consulta`() {
        assertEquals(
            "geo:0,0?q=The%20White%20Smile%2C%20San%20Salvador",
            ContactLinks.map("The White Smile, San Salvador")
        )
    }
}
