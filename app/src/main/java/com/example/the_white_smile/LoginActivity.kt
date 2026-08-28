package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnForgotPassword: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnGithub: MaterialButton

    private val googleClientId =
        "1047584081836-dmsvs530vuk2ginqtkno4jo3fsenhnec.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnGithub = findViewById(R.id.btnGithub)

        btnLogin.setOnClickListener { login() }
        btnForgotPassword.setOnClickListener { recoverPassword() }
        btnRegister.setOnClickListener { goToRegister() }
        btnGoogle.setOnClickListener { signInWithGoogle() }
        btnGithub.setOnClickListener { signInWithGithub() }
    }

    override fun onStart() {
        super.onStart()
        val pending = auth.pendingAuthResult
        if (pending != null) {
            pending
                .addOnSuccessListener { goToMenu() }
                .addOnFailureListener { e ->
                    if (e.message?.contains("User cancelled", ignoreCase = true) != true) {
                        Toast.makeText(this, e.message ?: getString(R.string.login_failed), Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun login() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.login_error_empty, Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.login_error_invalid_email, Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true
                if (task.isSuccessful) {
                    goToMenu()
                } else {
                    val message = task.exception?.message ?: getString(R.string.login_failed)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(googleClientId)
                    .setAutoSelectEnabled(true)
                    .build()
            )
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential =
                        GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this@LoginActivity) { task ->
                            if (task.isSuccessful) {
                                goToMenu()
                            } else {
                                val message = task.exception?.message ?: getString(R.string.login_failed)
                                Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this@LoginActivity, R.string.login_failed, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (e.message?.contains("User cancelled", ignoreCase = true) != true) {
                    Toast.makeText(this@LoginActivity, e.message ?: getString(R.string.login_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun signInWithGithub() {
        val provider = OAuthProvider.newBuilder("github.com")
            .setScopes(listOf("user:email"))
            .build()

        auth.startActivityForSignInWithProvider(this, provider)
            .addOnSuccessListener { result ->
                goToMenu()
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("User cancelled", ignoreCase = true) != true) {
                    Toast.makeText(
                        this,
                        e.message ?: getString(R.string.login_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun goToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun recoverPassword() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.login_error_invalid_email, Toast.LENGTH_SHORT).show()
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Te enviamos un correo para restablecer tu contraseña", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, task.exception?.message ?: getString(R.string.login_failed), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun goToMenu() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
