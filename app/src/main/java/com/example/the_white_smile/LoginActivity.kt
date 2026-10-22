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
                    if (!isUserCancelled(e)) {
                        Toast.makeText(this, FirebaseErrors.message(this, e), Toast.LENGTH_LONG).show()
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
        if (!isOnline()) {
            return
        }

        btnLogin.isEnabled = false
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true
                if (task.isSuccessful) {
                    goToMenu()
                } else {
                    Toast.makeText(
                        this,
                        FirebaseErrors.message(this, task.exception),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun signInWithGoogle() {
        if (!isOnline()) {
            return
        }

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
                                Toast.makeText(
                                    this@LoginActivity,
                                    FirebaseErrors.message(this@LoginActivity, task.exception),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                } else {
                    Toast.makeText(this@LoginActivity, R.string.login_failed, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (!isUserCancelled(e)) {
                    Toast.makeText(
                        this@LoginActivity,
                        FirebaseErrors.message(this@LoginActivity, e),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun signInWithGithub() {
        if (!isOnline()) {
            return
        }

        val provider = OAuthProvider.newBuilder("github.com")
            .setScopes(listOf("user:email"))
            .build()

        auth.startActivityForSignInWithProvider(this, provider)
            .addOnSuccessListener { result ->
                goToMenu()
            }
            .addOnFailureListener { e ->
                if (!isUserCancelled(e)) {
                    Toast.makeText(
                        this,
                        FirebaseErrors.message(this, e),
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
        if (!isOnline()) {
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, R.string.reset_email_sent, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        this,
                        FirebaseErrors.message(this, task.exception),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    /** Verifica la conexión a internet y avisa al usuario si no la hay. */
    private fun isOnline(): Boolean {
        if (NetworkChecker.isOnline(this)) {
            return true
        }
        Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_LONG).show()
        return false
    }

    /** El usuario cerró el selector de cuenta: no es un error que debamos mostrar. */
    private fun isUserCancelled(e: Throwable?): Boolean =
        e?.message?.contains("User cancelled", ignoreCase = true) == true

    private fun goToMenu() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
