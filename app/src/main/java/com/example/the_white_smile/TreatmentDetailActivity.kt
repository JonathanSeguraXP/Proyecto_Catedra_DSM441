package com.example.the_white_smile

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.the_white_smile.ui.treatments.TreatmentDetailUiState
import com.example.the_white_smile.ui.treatments.TreatmentDetailViewModel
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/**
 * Pantalla de detalle de un tratamiento.
 * El tratamiento se busca por su identificador en [TreatmentDetailViewModel].
 */
class TreatmentDetailActivity : AppCompatActivity() {

    private val treatmentId: String by lazy {
        intent.getStringExtra(EXTRA_TREATMENT_ID).orEmpty()
    }

    private val viewModel: TreatmentDetailViewModel by viewModels {
        TreatmentDetailViewModel.factory(treatmentId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treatment_detail)

        setupToolbar()

        if (viewModel.uiState.value.treatment == null) {
            showNotFound()
            return
        }

        observeViewModel()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarTreatmentDetail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: TreatmentDetailUiState) {
        val treatment = state.treatment
        if (treatment == null) {
            showNotFound()
            return
        }

        supportActionBar?.title = treatment.name
        findViewById<TextView>(R.id.tvTreatmentName).text = treatment.name
        findViewById<TextView>(R.id.tvTreatmentSummary).text = treatment.summary
        findViewById<TextView>(R.id.tvTreatmentDuration).text = getString(
            R.string.treatments_duration,
            treatment.durationMinutes
        )
        findViewById<TextView>(R.id.tvTreatmentDescription).text = treatment.description
        findViewById<TextView>(R.id.tvTreatmentRecommendations).text =
            treatment.recommendations.joinToString(separator = "\n") { "• $it" }
    }

    private fun showNotFound() {
        Toast.makeText(this, R.string.treatments_not_found, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        const val EXTRA_TREATMENT_ID = "extra_treatment_id"
    }
}
