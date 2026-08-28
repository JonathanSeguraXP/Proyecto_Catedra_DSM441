package com.example.the_white_smile

import android.os.Bundle
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

        btnCreate.setOnClickListener { register() }
        btnBack.setOnClickListener { finish() }
    }

    private fun register() {
        val name = etName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirm = etConfirm.text?.toString().orEmpty()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, R.string.login_error_empty, Toast.LENGTH_SHORT).show()
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
        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        btnCreate.isEnabled = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnCreate.isEnabled = true
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(name).build()
                    )
                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val message = task.exception?.message ?: getString(R.string.register_failed)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }
}
