package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.domain.Dentist
import com.example.the_white_smile.ui.dentists.DentistAdapter
import com.example.the_white_smile.ui.dentists.DentistsEvent
import com.example.the_white_smile.ui.dentists.DentistsUiState
import com.example.the_white_smile.ui.dentists.DentistsViewModel
import com.example.the_white_smile.util.FirebaseErrors
import com.example.the_white_smile.util.FirestoreErrors
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch

/**
 * Pantalla del administrador para gestionar dentistas.
 * Solo administra la vista: las cuentas y los estados los maneja
 * [DentistsViewModel].
 */
class DentistsActivity : AppCompatActivity() {

    private val viewModel: DentistsViewModel by viewModels {
        DentistsViewModel.factory(this)
    }

    private lateinit var adapter: DentistAdapter
    private lateinit var rvDentists: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var layoutLoading: View
    private var createDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dentists)

        setupToolbar()
        bindViews()
        setupList()
        setupListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()

        if (!viewModel.hasActiveSession()) {
            goToLogin()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        createDialog?.dismiss()
        createDialog = null
        super.onDestroy()
    }

    // ---------- Vista ----------

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDentists)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindViews() {
        rvDentists = findViewById(R.id.rvDentists)
        layoutEmpty = findViewById(R.id.layoutDentistsEmpty)
        layoutLoading = findViewById(R.id.layoutDentistsLoading)
    }

    private fun setupList() {
        adapter = DentistAdapter { dentist, active -> viewModel.setActive(dentist, active) }
        rvDentists.adapter = adapter
    }

    private fun setupListeners() {
        findViewById<FloatingActionButton>(R.id.fabNewDentist)
            .setOnClickListener { showNewDentistDialog() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: DentistsUiState) {
        adapter.submitList(state.dentists)
        layoutEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        rvDentists.visibility = if (state.isEmpty) View.GONE else View.VISIBLE
        layoutLoading.visibility = if (state.isCreating) View.VISIBLE else View.GONE
    }

    // ---------- Nuevo dentista ----------

    private fun showNewDentistDialog() {
        if (createDialog?.isShowing == true) return

        val content = layoutInflater.inflate(R.layout.dialog_new_dentist, null)
        val etName = content.findViewById<TextInputEditText>(R.id.etDentistName)
        val etSpecialty = content.findViewById<TextInputEditText>(R.id.etDentistSpecialty)
        val etEmail = content.findViewById<TextInputEditText>(R.id.etDentistEmail)
        val etPassword = content.findViewById<TextInputEditText>(R.id.etDentistPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dentists_dialog_title)
            .setView(content)
            .setPositiveButton(R.string.dentists_create, null)
            .setNegativeButton(R.string.exit_cancel, null)
            .create()

        // El dialogo se cierra al crear la cuenta; si hay un error se mantiene abierto.
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                viewModel.createDentist(
                    displayName = etName.text?.toString().orEmpty(),
                    specialty = etSpecialty.text?.toString().orEmpty(),
                    email = etEmail.text?.toString().orEmpty(),
                    password = etPassword.text?.toString().orEmpty()
                )
            }
        }
        dialog.setOnDismissListener { createDialog = null }

        createDialog = dialog
        dialog.show()
    }

    // ---------- Eventos ----------

    private fun handleEvent(event: DentistsEvent) {
        when (event) {
            is DentistsEvent.Message -> toastShort(event.messageRes)
            is DentistsEvent.Created -> {
                createDialog?.dismiss()
                toastLong(getString(R.string.dentists_created, event.displayName))
            }
            is DentistsEvent.Error -> {
                if (!event.cause.isUserCancelled()) {
                    toastLong(errorMessage(event.cause))
                }
            }
        }
    }

    /** Los errores de cuenta los traduce Firebase Auth; el resto, Firestore. */
    private fun errorMessage(cause: Throwable?): String =
        if (cause is FirebaseAuthException) {
            FirebaseErrors.message(this, cause)
        } else {
            FirestoreErrors.message(this, cause)
        }

    // ---------- Utilidades ----------

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
