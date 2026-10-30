package com.example.the_white_smile.ui.appointments

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.the_white_smile.R
import com.example.the_white_smile.domain.Dentist
import com.example.the_white_smile.util.ProfilePhotoCodec

/** Muestra nombre, especialidad y miniatura al elegir dentista para una cita. */
class DentistOptionAdapter(
    context: Context,
    private val dentists: List<Dentist>
) : ArrayAdapter<String>(
    context,
    android.R.layout.simple_list_item_1,
    dentists.map { optionLabel(context, it) }
) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        bindView(position, convertView, parent)

    private fun bindView(position: Int, recycled: View?, parent: ViewGroup): View {
        val view = recycled ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dentist_option, parent, false)
        val dentist = dentists[position]
        view.findViewById<TextView>(R.id.tvDentistOptionName).text = dentist.displayName
        view.findViewById<TextView>(R.id.tvDentistOptionSpecialty).text =
            dentist.specialty.ifBlank { context.getString(R.string.dentist_specialty_general) }
        val image = view.findViewById<ImageView>(R.id.ivDentistOptionPhoto)
        val bitmap = ProfilePhotoCodec.decode(dentist.profilePhotoBase64)
        if (bitmap == null) image.setImageResource(R.mipmap.ic_launcher)
        else image.setImageBitmap(bitmap)
        return view
    }

    companion object {
        fun optionLabel(context: Context, dentist: Dentist): String = context.getString(
            R.string.appointments_dentist_option,
            dentist.displayName,
            dentist.specialty.ifBlank { context.getString(R.string.dentist_specialty_general) }
        )
    }
}
