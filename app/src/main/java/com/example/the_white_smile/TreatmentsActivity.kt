package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.domain.Treatment
import com.example.the_white_smile.ui.treatments.TreatmentAdapter
import com.example.the_white_smile.ui.treatments.TreatmentsUiState
import com.example.the_white_smile.ui.treatments.TreatmentsViewModel
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/**
 * Pantalla con el catalogo de tratamientos.
 * Solo administra la vista: el catalogo lo entrega [TreatmentsViewModel].
 */
class TreatmentsActivity : BaseActivity() {

    private val viewModel: TreatmentsViewModel by viewModels {
        TreatmentsViewModel.factory()
    }

    private lateinit var rvTreatments: RecyclerView
    private lateinit var tvTreatmentsEmpty: TextView
    private lateinit var adapter: TreatmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treatments)

        setupToolbar()
        bindViews()
        setupList()
        observeViewModel()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarTreatments)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindViews() {
        rvTreatments = findViewById(R.id.rvTreatments)
        tvTreatmentsEmpty = findViewById(R.id.tvTreatmentsEmpty)
    }

    private fun setupList() {
        adapter = TreatmentAdapter(::openDetail)
        rvTreatments.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: TreatmentsUiState) {
        adapter.submitList(state.treatments)
        rvTreatments.visibility = if (state.isEmpty) View.GONE else View.VISIBLE
        tvTreatmentsEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
    }

    private fun openDetail(treatment: Treatment) {
        startActivity(
            Intent(this, TreatmentDetailActivity::class.java)
                .putExtra(TreatmentDetailActivity.EXTRA_TREATMENT_ID, treatment.id)
        )
    }
}
