package com.example.the_white_smile

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentSchedule
import com.example.the_white_smile.ui.appointments.AppointmentAdapter
import com.example.the_white_smile.ui.appointments.AppointmentsEvent
import com.example.the_white_smile.ui.appointments.AppointmentsUiState
import com.example.the_white_smile.ui.appointments.AppointmentsViewModel
import com.example.the_white_smile.ui.appointments.DentistOptionAdapter
import com.example.the_white_smile.util.DateFormats
import com.example.the_white_smile.util.FirestoreErrors
import com.example.the_white_smile.util.MoneyFormats
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Pantalla de citas.
 * Solo administra la vista: los turnos y las operaciones los maneja
 * [AppointmentsViewModel].
 */
class AppointmentsActivity : BaseActivity() {

    private val viewModel: AppointmentsViewModel by viewModels {
        AppointmentsViewModel.factory(this)
    }

    private lateinit var adapter: AppointmentAdapter
    private lateinit var rvAppointments: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var layoutLoading: View
    private lateinit var tvLoading: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointments)

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
            return
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // ---------- Vista ----------

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAppointments)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindViews() {
        rvAppointments = findViewById(R.id.rvAppointments)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        layoutLoading = findViewById(R.id.layoutLoading)
        tvLoading = findViewById(R.id.tvLoading)
    }

    private fun setupList() {
        adapter = AppointmentAdapter(::confirmCancel)
        rvAppointments.adapter = adapter
    }

    private fun setupListeners() {
        findViewById<FloatingActionButton>(R.id.fabNewAppointment)
            .setOnClickListener { showNewAppointmentDialog() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: AppointmentsUiState) {
        adapter.setTreatmentPhotos(state.treatments.associate { it.id to it.profilePhotoBase64 })
        adapter.submitList(state.appointments)
        layoutEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        rvAppointments.visibility = if (state.isEmpty) View.GONE else View.VISIBLE

        tvLoading.setText(state.loadingMessage)
        if (state.isLoading) showLoading() else hideLoading()
    }

    private fun treatmentOptionLabel(treatment: com.example.the_white_smile.domain.Treatment): String =
        getString(
            R.string.appointments_treatment_option,
            treatment.name,
            treatment.priceCents?.let(MoneyFormats::usd)
                ?: getString(R.string.treatments_price_unconfigured)
        )

    // ---------- Nueva cita ----------

    private fun showNewAppointmentDialog() {
        val content = layoutInflater.inflate(R.layout.dialog_new_appointment, null)

        val actDentist = content.findViewById<AutoCompleteTextView>(R.id.actAppointmentDentist)
        val tvNoDentists = content.findViewById<TextView>(R.id.tvNoDentists)
        val actTreatment =
            content.findViewById<AutoCompleteTextView>(R.id.actAppointmentTreatment)
        val btnPickDate = content.findViewById<MaterialButton>(R.id.btnPickDate)
        val tilTime = content.findViewById<TextInputLayout>(R.id.tilAppointmentTime)
        val actTime = content.findViewById<AutoCompleteTextView>(R.id.actAppointmentTime)
        val tvAvailabilityHint = content.findViewById<TextView>(R.id.tvAvailabilityHint)
        val tvTreatmentDuration = content.findViewById<TextView>(R.id.tvTreatmentDuration)
        val tvTreatmentPrice = content.findViewById<TextView>(R.id.tvTreatmentPrice)
        val depositOptions = content.findViewById<android.widget.RadioGroup>(R.id.rgDepositOptions)
        val tvDepositSummary = content.findViewById<TextView>(R.id.tvDepositSummary)
        val tilCustomDeposit = content.findViewById<TextInputLayout>(R.id.tilCustomDeposit)
        val etCustomDeposit = content.findViewById<TextInputEditText>(R.id.etCustomDeposit)
        val rbDepositQuarter = content.findViewById<android.widget.RadioButton>(R.id.rbDepositQuarter)
        val rbDepositHalf = content.findViewById<android.widget.RadioButton>(R.id.rbDepositHalf)
        val rbDepositCustom = content.findViewById<android.widget.RadioButton>(R.id.rbDepositCustom)
        val rbDepositNone = content.findViewById<android.widget.RadioButton>(R.id.rbDepositNone)

        val state = viewModel.uiState.value
        val dentists = state.dentists
        val treatments = state.treatments

        var selectedDentistId: String? = null
        var selectedTreatmentId: String? = null
        var selectedDepositPercent = 25
        var selectedDayStart: Long? = null
        var selectedStartAt: Long? = null
        var availableTimes: List<Long> = emptyList()

        fun renderPriceBreakdown(treatment: com.example.the_white_smile.domain.Treatment) {
            val total = treatment.priceCents
            if (total == null) {
                tvTreatmentPrice.setText(R.string.appointments_price_unconfigured)
                rbDepositQuarter.visibility = View.GONE
                rbDepositHalf.visibility = View.GONE
                rbDepositCustom.visibility = View.GONE
                rbDepositNone.visibility = View.VISIBLE
                depositOptions.visibility = View.VISIBLE
                depositOptions.check(R.id.rbDepositNone)
                selectedDepositPercent = 0
                tilCustomDeposit.visibility = View.GONE
                tvDepositSummary.visibility = View.VISIBLE
                tvDepositSummary.setText(R.string.appointments_deposit_unconfigured_none)
                return
            }
            tvTreatmentPrice.text = getString(
                R.string.appointments_price_total,
                MoneyFormats.usd(total)
            )
            depositOptions.visibility = View.VISIBLE
            rbDepositQuarter.visibility = View.VISIBLE
            rbDepositHalf.visibility = View.VISIBLE
            rbDepositCustom.visibility = View.VISIBLE
            rbDepositNone.visibility = View.VISIBLE
            tvDepositSummary.visibility = View.VISIBLE
            val deposit = when (selectedDepositPercent) {
                0 -> 0L
                25, 50 -> MoneyFormats.percentageOf(total, selectedDepositPercent)
                else -> MoneyFormats.parseUsd(etCustomDeposit.text?.toString().orEmpty())
            }
            if (selectedDepositPercent == 0) {
                tvDepositSummary.text = getString(R.string.appointments_deposit_none_summary, MoneyFormats.usd(total))
            } else if (deposit == null || deposit > total) {
                tvDepositSummary.setText(R.string.appointments_deposit_custom_invalid)
            } else {
                tvDepositSummary.text = getString(
                    if (selectedDepositPercent == -1) R.string.appointments_deposit_custom_summary
                    else R.string.appointments_deposit_summary,
                    MoneyFormats.usd(deposit),
                    MoneyFormats.usd(total - deposit)
                )
            }
        }

        fun resetTime() {
            selectedStartAt = null
            availableTimes = emptyList()
            actTime.setText("", false)
            actTime.setAdapter(null)
            actTime.isEnabled = false
            tilTime.isEnabled = false
            tvAvailabilityHint.setText(R.string.appointments_availability_hint)
        }

        fun requestAvailability() {
            val dentistId = selectedDentistId ?: return
            val treatmentId = selectedTreatmentId ?: return
            val dayStart = selectedDayStart ?: return
            viewModel.requestAvailability(dentistId, treatmentId, dayStart)
        }

        // Solo se ofrecen dentistas activos, cargados por el ViewModel.
        actDentist.setAdapter(DentistOptionAdapter(this, dentists))
        actDentist.setOnItemClickListener { _, _, position, _ ->
            selectedDentistId = dentists[position].id
            resetTime()
            requestAvailability()
        }
        actDentist.setOnClickListener { actDentist.showDropDown() }
        tvNoDentists.visibility = if (dentists.isEmpty()) View.VISIBLE else View.GONE

        actTreatment.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                treatments.map(::treatmentOptionLabel)
            )
        )
        actTreatment.setOnItemClickListener { _, _, position, _ ->
            val treatment = treatments[position]
            selectedTreatmentId = treatment.id
            renderPriceBreakdown(treatment)
            tvTreatmentDuration.text = getString(
                R.string.appointments_duration,
                treatment.durationMinutes
            )
            tvTreatmentDuration.visibility = View.VISIBLE
            resetTime()
            requestAvailability()
        }
        actTreatment.setOnClickListener { actTreatment.showDropDown() }

        depositOptions.check(R.id.rbDepositQuarter)
        depositOptions.setOnCheckedChangeListener { _, checkedId ->
            selectedDepositPercent = when (checkedId) {
                R.id.rbDepositNone -> 0
                R.id.rbDepositHalf -> 50
                R.id.rbDepositCustom -> -1
                else -> 25
            }
            tilCustomDeposit.visibility = if (selectedDepositPercent == -1) View.VISIBLE else View.GONE
            treatments.firstOrNull { it.id == selectedTreatmentId }?.let(::renderPriceBreakdown)
        }
        etCustomDeposit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                treatments.firstOrNull { it.id == selectedTreatmentId }?.let(::renderPriceBreakdown)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        btnPickDate.setOnClickListener {
            val current = selectedDayStart ?: System.currentTimeMillis()
            val initial = Calendar.getInstance().apply { timeInMillis = current }

            val picker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val dayStart = AppointmentSchedule.startOfDay(
                        dateMillis(year, month, day)
                    )
                    selectedDayStart = dayStart
                    btnPickDate.text = DateFormats.date(dayStart)
                    resetTime()
                    requestAvailability()
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
            )
            // No se pueden elegir dias anteriores a hoy.
            picker.datePicker.minDate = startOfToday()
            picker.show()
        }

        actTime.setOnItemClickListener { _, _, position, _ ->
            selectedStartAt = availableTimes.getOrNull(position)
        }
        actTime.setOnClickListener {
            if (actTime.isEnabled) actTime.showDropDown()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.appointments_dialog_title)
            .setView(content)
            .setPositiveButton(R.string.appointments_dialog_confirm, null)
            .setNegativeButton(R.string.exit_cancel, null)
            .create()

        // Se reemplaza el listener para validar sin cerrar el dialogo.
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                // Si el usuario escribio el nombre sin tocar la lista, se busca igual.
                val typedDentist = actDentist.text?.toString()?.trim().orEmpty()
                val dentistId = selectedDentistId
                    ?: dentists.firstOrNull {
                        DentistOptionAdapter.optionLabel(this, it) == typedDentist
                    }?.id
                val typedTreatment = actTreatment.text?.toString()?.trim().orEmpty()
                val treatmentId = selectedTreatmentId
                    ?: treatments.firstOrNull { treatmentOptionLabel(it) == typedTreatment }?.id
                val selectedTreatment = treatments.firstOrNull { it.id == treatmentId }
                val startAt = selectedStartAt

                when {
                    startAt == null -> {
                        toastShort(R.string.appointments_error_datetime)
                    }
                    dentistId == null -> {
                        toastShort(
                            if (dentists.isEmpty()) R.string.appointments_no_dentists
                            else R.string.appointments_error_dentist
                        )
                    }
                    treatmentId == null -> {
                        toastShort(R.string.appointments_error_treatment)
                    }
                    selectedTreatment?.priceCents == null && selectedDepositPercent != 0 -> {
                        toastLong(getString(R.string.appointments_deposit_missing))
                    }
                    else -> {
                        val customAmount = if (selectedDepositPercent == -1) {
                            MoneyFormats.parseUsd(etCustomDeposit.text?.toString().orEmpty())
                        } else null
                        if (selectedDepositPercent == -1 &&
                            (customAmount == null || customAmount > (selectedTreatment?.priceCents ?: 0L))) {
                            toastShort(R.string.appointments_deposit_custom_invalid)
                            return@setOnClickListener
                        }
                        dialog.dismiss()
                        viewModel.createAppointment(
                            treatmentId,
                            startAt,
                            dentistId,
                            selectedDepositPercent,
                            customAmount
                        )
                    }
                }
            }
        }

        // Mantiene la lista de horas sincronizada con la agenda del dentista.
        val availabilityJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { it.availability }
                    .distinctUntilChanged()
                    .collect { value ->
                        val availability = value ?: return@collect
                        val matches = availability.dentistId == selectedDentistId &&
                            availability.treatmentId == selectedTreatmentId &&
                            availability.dayStartAt == selectedDayStart
                        if (!matches) return@collect

                        availableTimes = availability.times
                        val enabled = !availability.isLoading
                        tilTime.isEnabled = enabled
                        actTime.isEnabled = enabled
                        actTime.setAdapter(
                            ArrayAdapter(
                                this@AppointmentsActivity,
                                android.R.layout.simple_list_item_1,
                                availability.times.map { DateFormats.time(it) }
                            )
                        )
                        tvAvailabilityHint.setText(
                            when {
                                availability.isLoading ->
                                    R.string.appointments_availability_loading
                                availability.times.isEmpty() ->
                                    R.string.appointments_no_times
                                else -> R.string.appointments_availability_times
                            }
                        )
                        if (selectedStartAt != null &&
                            !availability.times.contains(selectedStartAt)
                        ) {
                            selectedStartAt = null
                            actTime.setText("", false)
                        }
                    }
            }
        }

        dialog.setOnDismissListener {
            availabilityJob.cancel()
            viewModel.clearAvailability()
        }

        dialog.show()
    }

    // ---------- Cancelacion ----------

    private fun confirmCancel(appointment: Appointment) {
        AlertDialog.Builder(this)
            .setTitle(R.string.appointments_cancel_title)
            .setMessage(R.string.appointments_cancel_message)
            .setPositiveButton(R.string.appointments_cancel_confirm) { _, _ ->
                viewModel.cancelAppointment(appointment)
            }
            .setNegativeButton(R.string.exit_cancel, null)
            .show()
    }

    // ---------- Eventos ----------

    private fun handleEvent(event: AppointmentsEvent) {
        when (event) {
            is AppointmentsEvent.Message -> toastShort(event.messageRes)
            is AppointmentsEvent.Error -> {
                if (!event.cause.isUserCancelled()) {
                    toastLong(FirestoreErrors.message(this, event.cause))
                }
            }
        }
    }

    // ---------- Utilidades ----------

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }.timeInMillis

    /** Medianoche de hoy: el selector de fecha no permite dias anteriores. */
    private fun startOfToday(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

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
