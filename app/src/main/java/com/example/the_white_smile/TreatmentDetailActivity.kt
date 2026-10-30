package com.example.the_white_smile

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AlertDialog
import com.example.the_white_smile.ui.treatments.TreatmentDetailUiState
import com.example.the_white_smile.ui.treatments.TreatmentDetailViewModel
import com.example.the_white_smile.util.MoneyFormats
import com.example.the_white_smile.domain.Treatment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/**
 * Pantalla de detalle de un tratamiento.
 * El tratamiento se busca por su identificador en [TreatmentDetailViewModel].
 */
class TreatmentDetailActivity : BaseActivity() {

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
        findViewById<TextView>(R.id.tvTreatmentPrice).text = treatment.priceCents?.let {
            getString(R.string.treatments_price_label, MoneyFormats.usd(it))
        } ?: getString(R.string.treatments_price_unconfigured)
        findViewById<TextView>(R.id.tvTreatmentDuration).text = getString(
            R.string.treatments_duration,
            treatment.durationMinutes
        )
        findViewById<TextView>(R.id.tvTreatmentDescription).text = treatment.description
        findViewById<TextView>(R.id.tvTreatmentRecommendations).text =
            treatment.recommendations.joinToString(separator = "\n") { "• $it" }
        findViewById<View>(R.id.btnEditTreatment).visibility =
            if (state.isAdmin) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnEditTreatment).setOnClickListener { showEditDialog(treatment) }
    }

    private fun showEditDialog(treatment: Treatment) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }
        fun field(label: Int, value: String, multiline: Boolean = false): TextInputEditText {
            val inputLayout = TextInputLayout(this).apply {
                hint = getString(label)
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            }
            val input = TextInputEditText(this).apply {
                setText(value)
                if (multiline) {
                    minLines = 2
                    maxLines = 5
                    gravity = android.view.Gravity.TOP
                }
            }
            inputLayout.addView(input)
            form.addView(inputLayout)
            return input
        }
        val name = field(R.string.treatment_edit_name, treatment.name)
        val summary = field(R.string.treatment_edit_summary, treatment.summary, true)
        val description = field(R.string.treatment_edit_description, treatment.description, true)
        val duration = field(R.string.treatment_edit_duration, treatment.durationMinutes.toString())
        val recommendations = field(
            R.string.treatment_edit_recommendations,
            treatment.recommendations.joinToString("\n"),
            true
        )
        val price = field(
            R.string.treatment_edit_price,
            MoneyFormats.input(treatment.priceCents)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.treatment_edit)
            .setView(form)
            .setNegativeButton(R.string.exit_cancel, null)
            .setPositiveButton(R.string.treatment_edit) { _, _ ->
                val updated = treatment.copy(
                    name = name.text?.toString().orEmpty().trim(),
                    summary = summary.text?.toString().orEmpty().trim(),
                    description = description.text?.toString().orEmpty().trim(),
                    durationMinutes = duration.text?.toString()?.toIntOrNull()
                        ?.takeIf { it in 10..480 } ?: treatment.durationMinutes,
                    recommendations = recommendations.text?.toString().orEmpty()
                        .lines().map(String::trim).filter(String::isNotEmpty),
                    priceCents = MoneyFormats.parseUsd(price.text?.toString().orEmpty())
                        ?: treatment.priceCents
                )
                lifecycleScope.launch {
                    viewModel.save(updated).onSuccess {
                        Toast.makeText(this@TreatmentDetailActivity, R.string.treatment_saved, Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(this@TreatmentDetailActivity, R.string.error_generic, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun showNotFound() {
        Toast.makeText(this, R.string.treatments_not_found, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        const val EXTRA_TREATMENT_ID = "extra_treatment_id"
    }
}
