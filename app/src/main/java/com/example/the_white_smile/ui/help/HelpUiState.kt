package com.example.the_white_smile.ui.help

/** Pregunta con el estado de su respuesta. */
data class FaqEntry(
    val faq: FaqItem,
    val isExpanded: Boolean = false
)

/** Estado de la pantalla de ayuda: preguntas y cual esta desplegada. */
data class HelpUiState(
    val entries: List<FaqEntry> = emptyList()
)
