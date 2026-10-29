package com.example.the_white_smile.ui.help

import com.example.the_white_smile.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del estado de las preguntas frecuentes de la pantalla de ayuda. */
class HelpViewModelTest {

    private val faqs = listOf(
        FaqItem(
            id = "uno",
            questionRes = R.string.help_faq_book_question,
            answerRes = R.string.help_faq_book_answer
        ),
        FaqItem(
            id = "dos",
            questionRes = R.string.help_faq_cancel_question,
            answerRes = R.string.help_faq_cancel_answer
        ),
        FaqItem(
            id = "tres",
            questionRes = R.string.help_faq_history_question,
            answerRes = R.string.help_faq_history_answer
        )
    )

    private fun createViewModel() = HelpViewModel(faqs)

    private fun HelpViewModel.expandedIds(): List<String> =
        uiState.value.entries.filter { it.isExpanded }.map { it.faq.id }

    @Test
    fun `inicia con todas las preguntas cerradas`() {
        val viewModel = createViewModel()

        assertEquals(3, viewModel.uiState.value.entries.size)
        assertTrue(viewModel.uiState.value.entries.none { it.isExpanded })
    }

    @Test
    fun `al abrir una pregunta solo se despliega esa respuesta`() {
        val viewModel = createViewModel()

        viewModel.toggle("dos")

        assertEquals(listOf("dos"), viewModel.expandedIds())
    }

    @Test
    fun `al tocar la pregunta abierta se vuelve a cerrar`() {
        val viewModel = createViewModel()

        viewModel.toggle("dos")
        viewModel.toggle("dos")

        assertTrue(viewModel.expandedIds().isEmpty())
    }

    @Test
    fun `al abrir otra pregunta se cierra la anterior`() {
        val viewModel = createViewModel()

        viewModel.toggle("uno")
        viewModel.toggle("tres")

        assertEquals(listOf("tres"), viewModel.expandedIds())
    }

    @Test
    fun `el catalogo tiene identificadores unicos y textos definidos`() {
        val catalog = FaqCatalog.items

        assertTrue(catalog.isNotEmpty())
        assertEquals(catalog.size, catalog.map { it.id }.toSet().size)
        catalog.forEach { faq ->
            assertFalse(faq.id.isBlank())
            assertTrue(faq.questionRes != 0)
            assertTrue(faq.answerRes != 0)
        }
    }
}
