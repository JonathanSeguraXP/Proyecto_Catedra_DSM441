package com.example.the_white_smile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.ui.appointments.AppointmentAdapter
import com.example.the_white_smile.ui.appointments.AppointmentsEvent
import com.example.the_white_smile.ui.appointments.AppointmentsUiState
import com.example.the_white_smile.ui.appointments.AppointmentsViewModel
import com.example.the_white_smile.util.DateFormats
import com.example.the_white_smile.util.FirestoreErrors
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Pantalla de citas.
 * Solo administra la vista: los turnos y las operaciones los maneja
 * [AppointmentsViewModel].
 */
class AppointmentsActivity : AppCompatActivity() {

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
        adapter.submitList(state.appointments)
        layoutEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        rvAppointments.visibility = if (state.isEmpty) View.GONE else View.VISIBLE

        tvLoading.setText(state.loadingMessage)
        if (state.isLoading) showLoading() else hideLoading()
    }

    // ---------- Nueva cita ----------

    private fun showNewAppointmentDialog() {
        val content = layoutInflater.inflate(R.layout.dialog_new_appointment, null)

        val actDentist = content.findViewById<AutoCompleteTextView>(R.id.actAppointmentDentist)
        val tvNoDentists = content.findViewById<TextView>(R.id.tvNoDentists)
        val actService = content.findViewById<AutoCompleteTextView>(R.id.actAppointmentService)
        val btnPickDate = content.findViewById<MaterialButton>(R.id.btnPickDate)
        val btnPickTime = content.findViewById<MaterialButton>(R.id.btnPickTime)

        // Solo se ofrecen dentistas activos, cargados por el ViewModel.
        val dentists = viewModel.uiState.value.dentists
        var selectedDentistId: String? = null

        actDentist.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                dentists.map { it.displayName }
            )
        )
        actDentist.setOnItemClickListener { _, _, position, _ ->
            selectedDentistId = dentists[position].id
        }
        actDentist.setOnClickListener { actDentist.showDropDown() }
        tvNoDentists.visibility = if (dentists.isEmpty()) View.VISIBLE else View.GONE

        actService.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                resources.getStringArray(R.array.appointment_services)
            )
        )

        // Fecha y hora elegidas por el usuario, antes de confirmar.
        var selectedDate: Triple<Int, Int, Int>? = null
        var selectedTime: Pair<Int, Int>? = null

        btnPickDate.setOnClickListener {
            val current = selectedDate?.let { dateMillis(it.first, it.second, it.third) }
                ?: System.currentTimeMillis()
            val initial = Calendar.getInstance().apply { timeInMillis = current }

            val picker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = Triple(year, month, day)
                    btnPickDate.text = DateFormats.date(dateMillis(year, month, day))
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
            )
            // No se pueden elegir dias anteriores a hoy.
            picker.datePicker.minDate = startOfToday()
            picker.show()
        }

        btnPickTime.setOnClickListener {
            val initial = selectedTime?.let { (hour, minute) ->
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
            } ?: Calendar.getInstance()

            val picker = TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedTime = hour to minute
                    btnPickTime.text = formatTime(hour, minute)
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true
            )
            picker.show()
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
                val service = actService.text?.toString().orEmpty()
                val date = selectedDate
                val time = selectedTime
                // Si el usuario escribio el nombre sin tocar la lista, se busca igual.
                val typedDentist = actDentist.text?.toString()?.trim().orEmpty()
                val dentistId = selectedDentistId
                    ?: dentists.firstOrNull { it.displayName == typedDentist }?.id

                when {
                    date == null || time == null -> {
                        toastShort(R.string.appointments_error_datetime)
                    }
                    dentistId == null -> {
                        toastShort(
                            if (dentists.isEmpty()) R.string.appointments_no_dentists
                            else R.string.appointments_error_dentist
                        )
                    }
                    service.isBlank() -> {
                        toastShort(R.string.appointments_error_service)
                    }
                    else -> {
                        dialog.dismiss()
                        viewModel.createAppointment(
                            service = service,
                            startAt = startAtMillis(date, time),
                            dentistId = dentistId
                        )
                    }
                }
            }
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

    private fun startAtMillis(date: Triple<Int, Int, Int>, time: Pair<Int, Int>): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, date.first)
            set(Calendar.MONTH, date.second)
            set(Calendar.DAY_OF_MONTH, date.third)
            set(Calendar.HOUR_OF_DAY, time.first)
            set(Calendar.MINUTE, time.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun formatTime(hour: Int, minute: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

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
