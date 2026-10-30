package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.ui.agenda.DentistAgendaAdapter
import com.example.the_white_smile.ui.agenda.DentistAgendaEvent
import com.example.the_white_smile.ui.agenda.DentistAgendaUiState
import com.example.the_white_smile.ui.agenda.DentistAgendaViewModel
import com.example.the_white_smile.util.FirestoreErrors
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/**
 * Pantalla de agenda del dentista.
 * Solo administra la vista: las citas asignadas y el completado los maneja
 * [DentistAgendaViewModel].
 */
class DentistAgendaActivity : BaseActivity() {

    private val viewModel: DentistAgendaViewModel by viewModels {
        DentistAgendaViewModel.factory(this)
    }

    private lateinit var rvAgenda: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var layoutLoading: View
    private lateinit var tvLoading: TextView
    private lateinit var adapter: DentistAgendaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dentist_agenda)

        setupToolbar()
        bindViews()
        setupList()
        findViewById<View>(R.id.btnOpenDentistStats).setOnClickListener {
            startActivity(Intent(this, ConsultationStatsActivity::class.java))
        }
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()

        if (!viewModel.hasActiveSession()) {
            goToLogin()
            return
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAgenda)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindViews() {
        rvAgenda = findViewById(R.id.rvAgenda)
        layoutEmpty = findViewById(R.id.layoutAgendaEmpty)
        layoutLoading = findViewById(R.id.layoutAgendaLoading)
        tvLoading = findViewById(R.id.tvAgendaLoading)
    }

    private fun setupList() {
        adapter = DentistAgendaAdapter(::confirmComplete, ::editSuggestion)
        rvAgenda.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: DentistAgendaUiState) {
        adapter.submitList(state.appointments)

        tvLoading.setText(state.loadingMessage)
        layoutLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        layoutEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        rvAgenda.visibility = if (state.isLoading || state.isEmpty) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun confirmComplete(appointment: Appointment) {
        val patientName = appointment.patientName?.trim().orEmpty()
            .ifEmpty { getString(R.string.agenda_patient_unknown) }

        AlertDialog.Builder(this)
            .setTitle(R.string.agenda_complete_title)
            .setMessage(getString(R.string.agenda_complete_message, patientName))
            .setPositiveButton(R.string.agenda_complete_confirm) { _, _ ->
                viewModel.completeAppointment(appointment)
            }
            .setNegativeButton(R.string.exit_cancel, null)
            .show()
    }

    private fun editSuggestion(appointment: Appointment) {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.agenda_suggestion_hint)
            setText(appointment.dentistSuggestion.orEmpty())
            minLines = 3
            maxLines = 6
            setPadding(48, 24, 48, 0)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.agenda_suggestion)
            .setView(input)
            .setNegativeButton(R.string.exit_cancel, null)
            .setPositiveButton(R.string.save_changes) { _, _ ->
                viewModel.saveSuggestion(appointment, input.text?.toString().orEmpty())
            }
            .show()
    }

    private fun handleEvent(event: DentistAgendaEvent) {
        when (event) {
            is DentistAgendaEvent.Message -> {
                Toast.makeText(this, event.messageRes, Toast.LENGTH_SHORT).show()
            }
            is DentistAgendaEvent.Error -> {
                if (!event.cause.isUserCancelled()) {
                    Toast.makeText(
                        this,
                        FirestoreErrors.message(this, event.cause),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
