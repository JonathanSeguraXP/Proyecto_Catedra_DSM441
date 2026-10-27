package com.example.the_white_smile.ui.login

import androidx.annotation.StringRes
import com.example.the_white_smile.R

/** Estado persistente de la pantalla de login. */
data class LoginUiState(
    val isLoading: Boolean = false,
    @param:StringRes val loadingMessage: Int = R.string.loading_login
)
