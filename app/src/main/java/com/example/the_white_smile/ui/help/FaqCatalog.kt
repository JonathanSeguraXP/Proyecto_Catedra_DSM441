package com.example.the_white_smile.ui.help

import androidx.annotation.StringRes
import com.example.the_white_smile.R

/** Pregunta frecuente con su respuesta, ambas en recursos de texto. */
data class FaqItem(
    val id: String,
    @param:StringRes val questionRes: Int,
    @param:StringRes val answerRes: Int
)

/**
 * Preguntas frecuentes incluidas en la aplicacion.
 * Es contenido de ejemplo y puede ampliarse sin cambiar la pantalla.
 */
object FaqCatalog {

    val items: List<FaqItem> = listOf(
        FaqItem(
            id = "agendar-cita",
            questionRes = R.string.help_faq_book_question,
            answerRes = R.string.help_faq_book_answer
        ),
        FaqItem(
            id = "cancelar-cita",
            questionRes = R.string.help_faq_cancel_question,
            answerRes = R.string.help_faq_cancel_answer
        ),
        FaqItem(
            id = "historial",
            questionRes = R.string.help_faq_history_question,
            answerRes = R.string.help_faq_history_answer
        ),
        FaqItem(
            id = "cambiar-contrasena",
            questionRes = R.string.help_faq_password_question,
            answerRes = R.string.help_faq_password_answer
        ),
        FaqItem(
            id = "verificar-correo",
            questionRes = R.string.help_faq_verification_question,
            answerRes = R.string.help_faq_verification_answer
        ),
        FaqItem(
            id = "actualizar-datos",
            questionRes = R.string.help_faq_profile_question,
            answerRes = R.string.help_faq_profile_answer
        ),
        FaqItem(
            id = "tratamientos",
            questionRes = R.string.help_faq_treatments_question,
            answerRes = R.string.help_faq_treatments_answer
        ),
        FaqItem(
            id = "contacto",
            questionRes = R.string.help_faq_contact_question,
            answerRes = R.string.help_faq_contact_answer
        )
    )
}
