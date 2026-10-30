package com.example.the_white_smile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.ui.register.RegisterEvent
import com.example.the_white_smile.ui.register.RegisterUiState
import com.example.the_white_smile.ui.register.RegisterViewModel
import com.example.the_white_smile.util.FirebaseErrors
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Pantalla de registro.
 * Solo se encarga de la vista: delega validaciones y llamadas a [RegisterViewModel].
 */
class RegisterActivity : BaseActivity() {

    private val viewModel: RegisterViewModel by viewModels { RegisterViewModel.factory(this) }

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnCreate: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var layoutLoading: View
    private lateinit var tvLoading: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        bindViews()
        setupListeners()
        observeViewModel()
    }

    // ---------- Vista ----------

    private fun bindViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etRegisterEmail)
        etPassword = findViewById(R.id.etRegisterPassword)
        etConfirm = findViewById(R.id.etRegisterConfirm)
        btnCreate = findViewById(R.id.btnCreateAccount)
        btnBack = findViewById(R.id.btnBackToLogin)
        layoutLoading = findViewById(R.id.layoutLoading)
        tvLoading = findViewById(R.id.tvLoading)
    }

    private fun setupListeners() {
        btnCreate.setOnClickListener {
            viewModel.register(
                name = etName.text?.toString().orEmpty(),
                email = etEmail.text?.toString().orEmpty(),
                password = etPassword.text?.toString().orEmpty(),
                confirm = etConfirm.text?.toString().orEmpty()
            )
        }
        btnBack.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: RegisterUiState) {
        tvLoading.setText(state.loadingMessage)
        if (state.isLoading) showLoading() else hideLoading()
    }

    private fun handleEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.Message -> toastShort(event.messageRes)
            is RegisterEvent.Error -> {
                if (!event.cause.isUserCancelled()) {
                    toastLong(FirebaseErrors.message(this, event.cause))
                }
            }
            RegisterEvent.Registered -> {
                toastLong(getString(R.string.register_success))
                finish()
            }
        }
    }

    private fun showLoading() {
        if (layoutLoading.visibility == View.VISIBLE) return
        layoutLoading.visibility = View.VISIBLE
        layoutLoading.alpha = 0f
        layoutLoading.animate().alpha(1f).setDuration(200).start()
    }

    private fun hideLoading() {
        if (layoutLoading.visibility != View.VISIBLE) return
        layoutLoading.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction { layoutLoading.visibility = View.GONE }
            .start()
    }

    private fun toastShort(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun toastLong(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
