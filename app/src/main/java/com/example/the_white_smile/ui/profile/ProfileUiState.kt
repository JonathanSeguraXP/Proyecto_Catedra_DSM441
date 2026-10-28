package com.example.the_white_smile.ui.profile

import androidx.annotation.StringRes
import com.example.the_white_smile.R

/** Estado de la pantalla de perfil. */
data class ProfileUiState(
    val name: String = "",
    val email: String? = null,
    val isEmailVerified: Boolean = false,
    val isLoading: Boolean = false,
    @param:StringRes val loadingMessage: Int = R.string.loading_profile
)
