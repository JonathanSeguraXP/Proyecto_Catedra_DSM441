package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.ui.login.LoginEvent
import com.example.the_white_smile.ui.login.LoginUiState
import com.example.the_white_smile.ui.login.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Pantalla de login.
 * Solo se encarga de la vista: delega validaciones y llamadas a [LoginViewModel].
 */
class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels { LoginViewModel.factory(this) }

    private lateinit var credentialManager: CredentialManager

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnForgotPassword: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnGithub: MaterialButton
    private lateinit var cbRemember: MaterialCheckBox
    private lateinit var layoutLoading: View
    private lateinit var tvLoading: TextView

    private val googleClientId =
        "1047584081836-dmsvs530vuk2ginqtkno4jo3fsenhnec.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        credentialManager = CredentialManager.create(this)

        bindViews()
        restoreRememberedEmail()
        setupListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()

        // Si ya existe una sesion activa, se entra directo al menu principal.
        if (viewModel.hasActiveSession()) {
            goToMenu()
            return
        }

        // Flujo OAuth que quedo pendiente si el sistema recreo la Activity.
        ServiceLocator.auth.pendingAuthResult?.addOnSuccessListener { goToMenu() }
    }

    // ---------- Vista ----------

    private fun bindViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnGithub = findViewById(R.id.btnGithub)
        cbRemember = findViewById(R.id.cbRemember)
        layoutLoading = findViewById(R.id.layoutLoading)
        tvLoading = findViewById(R.id.tvLoading)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            viewModel.login(
                email = etEmail.text?.toString().orEmpty(),
                password = etPassword.text?.toString().orEmpty()
            )
        }
        btnForgotPassword.setOnClickListener {
            viewModel.recoverPassword(etEmail.text?.toString().orEmpty())
        }
        btnRegister.setOnClickListener { goToRegister() }
        btnGoogle.setOnClickListener { startGoogleSignIn() }
        btnGithub.setOnClickListener { startGithubSignIn() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: LoginUiState) {
        tvLoading.setText(state.loadingMessage)
        if (state.isLoading) showLoading() else hideLoading()
    }

    private fun handleEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.Message -> toastShort(event.messageRes)
            is LoginEvent.Error -> {
                if (!event.cause.isUserCancelled()) {
                    toastLong(FirebaseErrors.message(this, event.cause))
                }
            }
            is LoginEvent.LoggedIn -> {
                saveRememberedEmail(event.email)
                toastShort(R.string.login_success)
                goToMenu()
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

    // ---------- Accesos externos ----------

    private fun startGoogleSignIn() {
        if (!viewModel.isOnline()) return

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(googleClientId)
                    .setAutoSelectEnabled(true)
                    .build()
            )
            .build()

        viewModel.onExternalFlowStarted()
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.signInWithGoogle(googleIdToken.idToken)
                } else {
                    viewModel.onExternalFlowCancelled()
                    toastLong(getString(R.string.login_failed))
                }
            } catch (e: Exception) {
                viewModel.onExternalFlowCancelled()
                if (!e.isUserCancelled()) viewModel.reportError(e)
            }
        }
    }

    private fun startGithubSignIn() {
        if (!viewModel.isOnline()) return
        viewModel.signInWithGithub(this)
    }

    // ---------- Recordar correo ----------

    private fun restoreRememberedEmail() {
        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_REMEMBER, false)) {
            etEmail.setText(preferences.getString(KEY_EMAIL, "").orEmpty())
            cbRemember.isChecked = true
        }
    }

    private fun saveRememberedEmail(email: String) {
        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = preferences.edit()
        val remember = cbRemember.isChecked && email.isNotBlank()
        editor.putBoolean(KEY_REMEMBER, remember)
        if (remember) {
            editor.putString(KEY_EMAIL, email)
        } else {
            editor.remove(KEY_EMAIL)
        }
        editor.apply()
    }

    // ---------- Navegacion ----------

    private fun goToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun goToMenu() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private companion object {
        const val PREFS_NAME = "the_white_smile_prefs"
        const val KEY_REMEMBER = "remember_email"
        const val KEY_EMAIL = "saved_email"
    }
}
