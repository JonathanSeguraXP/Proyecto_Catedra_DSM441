package com.example.the_white_smile.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.the_white_smile.R
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Gráfico de consultas completadas por mes durante los últimos seis meses. */
class ConsultationChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.on_surface_variant)
        textSize = 12f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
    }
    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.outline)
        strokeWidth = resources.displayMetrics.density
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.on_surface)
        textSize = 12f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val monthFormat = SimpleDateFormat("MMM", Locale("es", "SV"))
    private var appointments: List<Appointment> = emptyList()

    fun setAppointments(items: List<Appointment>) {
        appointments = items.filter { it.status == AppointmentStatus.COMPLETED }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val now = Calendar.getInstance()
        val months = (5 downTo 0).map { offset ->
            (now.clone() as Calendar).apply {
                add(Calendar.MONTH, -offset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        val counts = months.map { month ->
            appointments.count { appointment ->
                val date = Calendar.getInstance().apply { timeInMillis = appointment.startAt }
                date.get(Calendar.YEAR) == month.get(Calendar.YEAR) &&
                    date.get(Calendar.MONTH) == month.get(Calendar.MONTH)
            }
        }
        val left = 18f * density
        val right = width - 18f * density
        val top = 16f * density
        val baseline = height - 28f * density
        canvas.drawLine(left, baseline, right, baseline, baselinePaint)
        val maxCount = maxOf(1, counts.maxOrNull() ?: 0)
        val columnWidth = (right - left) / months.size
        val barWidth = columnWidth * 0.48f
        months.indices.forEach { index ->
            val centerX = left + columnWidth * (index + 0.5f)
            val barHeight = (baseline - top - 22f * density) * counts[index] / maxCount
            if (counts[index] > 0) {
                val rect = android.graphics.RectF(
                    centerX - barWidth / 2f,
                    baseline - barHeight,
                    centerX + barWidth / 2f,
                    baseline
                )
                canvas.drawRoundRect(rect, 8f * density, 8f * density, barPaint)
                canvas.drawText(counts[index].toString(), centerX, baseline - barHeight - 6f * density, valuePaint)
            }
            canvas.drawText(monthFormat.format(Date(months[index].timeInMillis)), centerX, height - 6f * density, labelPaint)
        }
    }
}
