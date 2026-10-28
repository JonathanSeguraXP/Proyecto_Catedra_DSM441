package com.example.the_white_smile.ui.register

import androidx.annotation.StringRes
import com.example.the_white_smile.R

/** Estado persistente de la pantalla de registro. */
data class RegisterUiState(
    val isLoading: Boolean = false,
    @param:StringRes val loadingMessage: Int = R.string.loading_register
)
