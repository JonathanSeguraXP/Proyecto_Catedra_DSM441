package com.example.the_white_smile.ui.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Logica de la pantalla de ayuda.
 * Mantiene las preguntas frecuentes y permite desplegar una respuesta
 * a la vez.
 */
class HelpViewModel(
    faqs: List<FaqItem> = FaqCatalog.items
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HelpUiState(entries = faqs.map { FaqEntry(it) })
    )
    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

    /** Despliega la respuesta indicada o la cierra si ya estaba abierta. */
    fun toggle(id: String) {
        _uiState.update { state ->
            val selected = state.entries.firstOrNull { it.faq.id == id }
                ?: return@update state
            val expand = !selected.isExpanded

            state.copy(
                entries = state.entries.map { entry ->
                    entry.copy(isExpanded = expand && entry.faq.id == id)
                }
            )
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { HelpViewModel() }
        }
    }
}
