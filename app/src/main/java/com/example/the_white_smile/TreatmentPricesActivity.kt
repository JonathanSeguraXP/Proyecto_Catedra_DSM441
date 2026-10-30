package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.domain.Treatment
import com.example.the_white_smile.ui.treatments.TreatmentPriceAdapter
import com.example.the_white_smile.ui.treatments.TreatmentPricesEvent
import com.example.the_white_smile.ui.treatments.TreatmentPricesUiState
import com.example.the_white_smile.ui.treatments.TreatmentPricesViewModel
import com.example.the_white_smile.util.FirebaseErrors
import com.example.the_white_smile.util.FirestoreErrors
import com.example.the_white_smile.util.MoneyFormats
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch

class TreatmentPricesActivity : BaseActivity() {

    private val viewModel: TreatmentPricesViewModel by viewModels {
        TreatmentPricesViewModel.factory(this)
    }

    private lateinit var adapter: TreatmentPriceAdapter
    private var accessDeniedHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treatment_prices)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarTreatmentPrices)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = TreatmentPriceAdapter(::showPriceDialog)
        findViewById<RecyclerView>(R.id.rvTreatmentPrices).adapter = adapter
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        if (!viewModel.hasActiveSession()) goToLogin()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: TreatmentPricesUiState) {
        if (state.isAdmin == false && !accessDeniedHandled) {
            accessDeniedHandled = true
            Toast.makeText(this, R.string.treatment_prices_admin_only, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        adapter.submitList(state.treatments)
    }

    private fun showPriceDialog(treatment: Treatment) {
        val content = layoutInflater.inflate(R.layout.dialog_treatment_price, null)
        val inputLayout = content as TextInputLayout
        val input = content.findViewById<TextInputEditText>(R.id.etTreatmentPrice)
        input.setText(MoneyFormats.input(treatment.priceCents))

        val dialog = AlertDialog.Builder(this)
            .setTitle(treatment.name)
            .setView(content)
            .setPositiveButton(R.string.profile_btn_save, null)
            .setNegativeButton(R.string.exit_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val price = MoneyFormats.parseUsd(input.text?.toString().orEmpty())
                if (price == null) {
                    inputLayout.error = getString(R.string.treatment_prices_invalid)
                } else {
                    inputLayout.error = null
                    viewModel.savePrice(treatment, price)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun handleEvent(event: TreatmentPricesEvent) {
        when (event) {
            is TreatmentPricesEvent.Message ->
                Toast.makeText(this, event.messageRes, Toast.LENGTH_SHORT).show()
            is TreatmentPricesEvent.Error -> if (!event.cause.isUserCancelled()) {
                val message = if (event.cause is FirebaseFirestoreException) {
                    FirestoreErrors.message(this, event.cause)
                } else {
                    FirebaseErrors.message(this, event.cause)
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
