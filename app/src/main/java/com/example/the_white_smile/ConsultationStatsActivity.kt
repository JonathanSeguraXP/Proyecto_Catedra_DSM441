package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.core.isUserCancelled
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentStatus
import com.example.the_white_smile.domain.UserRole
import com.example.the_white_smile.ui.stats.ConsultationChartView
import com.example.the_white_smile.util.FirestoreErrors
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar

/** Resumen visual de las consultas completadas del dentista o del consultorio. */
class ConsultationStatsActivity : BaseActivity() {

    private lateinit var tvSubtitle: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvMonth: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var chart: ConsultationChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_stats)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarStats)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        tvSubtitle = findViewById(R.id.tvStatsSubtitle)
        tvTotal = findViewById(R.id.tvStatsTotal)
        tvMonth = findViewById(R.id.tvStatsMonth)
        tvEmpty = findViewById(R.id.tvStatsEmpty)
        chart = findViewById(R.id.chartConsultations)
        observeStatistics()
    }

    override fun onStart() {
        super.onStart()
        if (!ServiceLocator.authRepository.isLoggedIn()) goToLogin()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun observeStatistics() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val uid = ServiceLocator.authRepository.currentUser?.uid
                if (uid == null) {
                    goToLogin()
                    return@repeatOnLifecycle
                }
                val profile = ServiceLocator.userRepository.getProfile(uid).getOrNull()
                val appointmentsFlow = when (profile?.role) {
                    UserRole.ADMIN -> ServiceLocator.appointmentRepository.observeAllAppointments()
                    UserRole.DENTIST -> {
                        tvSubtitle.setText(R.string.stats_dentist_subtitle)
                        ServiceLocator.appointmentRepository.observeDentistAppointments(uid)
                    }
                    else -> {
                        Toast.makeText(this@ConsultationStatsActivity, R.string.stats_access_denied, Toast.LENGTH_LONG).show()
                        finish()
                        return@repeatOnLifecycle
                    }
                }
                appointmentsFlow
                    .catch { cause ->
                        if (!cause.isUserCancelled()) {
                            Toast.makeText(
                                this@ConsultationStatsActivity,
                                FirestoreErrors.message(this@ConsultationStatsActivity, cause),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        emit(emptyList())
                    }
                    .collect(::render)
            }
        }
    }

    private fun render(appointments: List<Appointment>) {
        val completed = appointments.filter { it.status == AppointmentStatus.COMPLETED }
        val now = Calendar.getInstance()
        val thisMonth = completed.count { appointment ->
            val date = Calendar.getInstance().apply { timeInMillis = appointment.startAt }
            date.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                date.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        }
        tvTotal.text = completed.size.toString()
        tvMonth.text = thisMonth.toString()
        chart.setAppointments(completed)
        tvEmpty.visibility = if (completed.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
