package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.ui.profile.ProfileEvent
import com.example.the_white_smile.ui.profile.ProfileUiState
import com.example.the_white_smile.ui.profile.ProfileViewModel
import com.example.the_white_smile.util.FirebaseErrors
import com.example.the_white_smile.util.FirestoreErrors
import com.example.the_white_smile.util.ProfilePhotoCodec
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla de perfil.
 * Solo administra la vista: los datos y las actualizaciones los maneja [ProfileViewModel].
 */
class ProfileActivity : BaseActivity() {

    private val viewModel: ProfileViewModel by viewModels { ProfileViewModel.factory(this) }

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileSpecialty: TextView
    private lateinit var ivProfileLogo: ImageView
    private lateinit var btnChangeProfilePhoto: MaterialButton
    private lateinit var tvEmailStatus: TextView
    private lateinit var btnVerifyEmail: MaterialButton
    private lateinit var etProfileName: TextInputEditText
    private lateinit var layoutLoading: View
    private lateinit var tvLoading: TextView
    private lateinit var profilePhotoPicker: ActivityResultLauncher<String>

    /** Evita sobrescribir lo que el usuario esta escribiendo. */
    private var lastRenderedName: String? = null
    private var lastRenderedPhoto: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setupToolbar()
        profilePhotoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) encodeAndSavePhoto(uri)
        }
        bindViews()
        setupListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()

        if (!viewModel.hasActiveSession()) {
            goToLogin()
            return
        }

        // Refresca el estado de verificacion al volver a la pantalla.
        viewModel.refresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // ---------- Vista ----------

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarProfile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindViews() {
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfileSpecialty = findViewById(R.id.tvProfileSpecialty)
        ivProfileLogo = findViewById(R.id.ivProfileLogo)
        btnChangeProfilePhoto = findViewById(R.id.btnChangeProfilePhoto)
        tvEmailStatus = findViewById(R.id.tvEmailStatus)
        btnVerifyEmail = findViewById(R.id.btnVerifyEmail)
        etProfileName = findViewById(R.id.etProfileName)
        layoutLoading = findViewById(R.id.layoutLoading)
        tvLoading = findViewById(R.id.tvLoading)
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.btnSaveName).setOnClickListener {
            viewModel.saveName(etProfileName.text?.toString().orEmpty())
        }
        btnVerifyEmail.setOnClickListener { viewModel.sendVerificationEmail() }
        findViewById<MaterialButton>(R.id.btnChangePassword).setOnClickListener {
            viewModel.sendPasswordReset()
        }
        btnChangeProfilePhoto.setOnClickListener { profilePhotoPicker.launch("image/*") }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: ProfileUiState) {
        tvProfileName.text = state.name.ifBlank { getString(R.string.profile_title) }
        tvProfileEmail.text = state.email.orEmpty()
        tvProfileSpecialty.visibility = if (state.specialty.isNullOrBlank()) View.GONE else View.VISIBLE
        if (!state.specialty.isNullOrBlank()) {
            tvProfileSpecialty.text = getString(R.string.profile_specialty, state.specialty)
        }
        btnChangeProfilePhoto.visibility = View.VISIBLE
        btnChangeProfilePhoto.setText(
            if (state.profilePhotoBase64.isNullOrBlank()) {
                R.string.profile_btn_add_photo
            } else {
                R.string.profile_btn_change_photo
            }
        )
        if (lastRenderedPhoto != state.profilePhotoBase64) {
            lastRenderedPhoto = state.profilePhotoBase64
            val bitmap = ProfilePhotoCodec.decode(state.profilePhotoBase64)
            if (bitmap == null) ivProfileLogo.setImageResource(R.mipmap.ic_launcher)
            else ivProfileLogo.setImageBitmap(bitmap)
        }

        // Solo se escribe el campo cuando el nombre cambia desde el ViewModel.
        if (lastRenderedName != state.name) {
            lastRenderedName = state.name
            if (etProfileName.text?.toString() != state.name) {
                etProfileName.setText(state.name)
            }
        }

        tvEmailStatus.setText(
            if (state.isEmailVerified) {
                R.string.profile_email_verified
            } else {
                R.string.profile_email_unverified
            }
        )
        tvEmailStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (state.isEmailVerified) R.color.tertiary else R.color.secondary
            )
        )
        btnVerifyEmail.visibility = if (state.isEmailVerified) View.GONE else View.VISIBLE

        tvLoading.setText(state.loadingMessage)
        if (state.isLoading) showLoading() else hideLoading()
    }

    private fun handleEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.Message -> toastShort(event.messageRes)
            is ProfileEvent.Error -> {
                if (!event.cause.isUserCancelled()) {
                    val message = if (event.cause is FirebaseFirestoreException) {
                        FirestoreErrors.message(this, event.cause)
                    } else {
                        FirebaseErrors.message(this, event.cause)
                    }
                    toastLong(message)
                }
            }
        }
    }

    private fun encodeAndSavePhoto(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val encoded = withContext(Dispatchers.IO) {
                    ProfilePhotoCodec.encode(contentResolver, uri)
                }
                viewModel.saveProfilePhoto(encoded)
            } catch (_: Exception) {
                toastShort(R.string.profile_photo_read_error)
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

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
