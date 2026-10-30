package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.ui.appointments.AppointmentAdapter
import com.example.the_white_smile.ui.history.HistoryEvent
import com.example.the_white_smile.ui.history.HistoryUiState
import com.example.the_white_smile.ui.history.HistoryViewModel
import com.example.the_white_smile.util.FirestoreErrors
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/**
 * Pantalla de historial de citas.
 * Solo administra la vista: el filtrado y el orden los maneja
 * [HistoryViewModel], y cada elemento se muestra con el adaptador de citas.
 */
class HistoryActivity : BaseActivity() {

    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModel.factory()
    }

    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var layoutLoading: View
    private lateinit var adapter: AppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setupToolbar()
        bindViews()
        setupList()
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
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarHistory)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindViews() {
        rvHistory = findViewById(R.id.rvHistory)
        layoutEmpty = findViewById(R.id.layoutHistoryEmpty)
        layoutLoading = findViewById(R.id.layoutHistoryLoading)
    }

    private fun setupList() {
        // En el historial no se cancela: el adaptador oculta esa accion.
        adapter = AppointmentAdapter { }
        rvHistory.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: HistoryUiState) {
        adapter.submitList(state.appointments)

        layoutLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        layoutEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        rvHistory.visibility = if (state.isLoading || state.isEmpty) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun handleEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.Error -> {
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
