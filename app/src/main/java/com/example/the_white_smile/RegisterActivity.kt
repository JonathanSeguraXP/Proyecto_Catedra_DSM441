package com.example.the_white_smile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

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

        auth = FirebaseAuth.getInstance()

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etRegisterEmail)
        etPassword = findViewById(R.id.etRegisterPassword)
        etConfirm = findViewById(R.id.etRegisterConfirm)
        btnCreate = findViewById(R.id.btnCreateAccount)
        btnBack = findViewById(R.id.btnBackToLogin)
        layoutLoading = findViewById(R.id.layoutLoading)
        tvLoading = findViewById(R.id.tvLoading)

        btnCreate.setOnClickListener { register() }
        btnBack.setOnClickListener { finish() }
    }

    private fun register() {
        val name = etName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirm = etConfirm.text?.toString().orEmpty()

        if (name.isEmpty()) {
            Toast.makeText(this, R.string.register_error_name_empty, Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, R.string.register_error_empty, Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.login_error_invalid_email, Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirm) {
            Toast.makeText(this, R.string.register_password_mismatch, Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, R.string.register_password_short, Toast.LENGTH_SHORT).show()
            return
        }
        if (!isOnline()) {
            return
        }

        btnCreate.isEnabled = false
        showLoading(getString(R.string.loading_register))
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveDisplayName(name)
                } else {
                    hideLoading()
                    btnCreate.isEnabled = true
                    Toast.makeText(
                        this,
                        FirebaseErrors.message(this, task.exception),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    /**
     * Guarda el nombre del usuario antes de volver al login, para que el menu
     * principal pueda saludarlo por su nombre.
     */
    private fun saveDisplayName(name: String) {
        val user = auth.currentUser
        val profile = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        user?.updateProfile(profile)?.addOnCompleteListener {
            hideLoading()
            btnCreate.isEnabled = true
            Toast.makeText(this, R.string.register_success, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // ---------- Capa de carga ----------

    private fun showLoading(message: String) {
        tvLoading.text = message
        layoutLoading.visibility = View.VISIBLE
        layoutLoading.alpha = 0f
        layoutLoading.animate().alpha(1f).setDuration(200).start()
    }

    private fun hideLoading() {
        layoutLoading.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction { layoutLoading.visibility = View.GONE }
            .start()
    }

    // ---------- Utilidades ----------

    /** Verifica la conexion a internet y avisa al usuario si no la hay. */
    private fun isOnline(): Boolean {
        if (NetworkChecker.isOnline(this)) {
            return true
        }
        Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_LONG).show()
        return false
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
