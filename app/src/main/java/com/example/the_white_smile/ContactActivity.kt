package com.example.the_white_smile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.the_white_smile.util.ContactLinks
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * Pantalla de contacto del consultorio.
 * La informacion es fija, por lo que no requiere ViewModel: la Activity
 * pinta los datos y abre las aplicaciones externas correspondientes.
 */
class ContactActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        setupToolbar()
        setupListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarContact)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.btnCall).setOnClickListener {
            open(Intent.ACTION_DIAL, ContactLinks.tel(getString(R.string.contact_phone_value)))
        }

        findViewById<MaterialButton>(R.id.btnWhatsapp).setOnClickListener {
            open(
                Intent.ACTION_VIEW,
                ContactLinks.whatsapp(
                    phone = getString(R.string.contact_whatsapp_value),
                    message = getString(R.string.contact_whatsapp_message)
                )
            )
        }

        findViewById<MaterialButton>(R.id.btnEmail).setOnClickListener {
            open(
                Intent.ACTION_SENDTO,
                ContactLinks.email(
                    address = getString(R.string.contact_email_value),
                    subject = getString(R.string.contact_email_subject)
                )
            )
        }

        findViewById<MaterialButton>(R.id.btnMap).setOnClickListener {
            val query = "${getString(R.string.contact_clinic_name)}, " +
                getString(R.string.contact_address_value)
            open(Intent.ACTION_VIEW, ContactLinks.map(query))
        }
    }

    /** Abre el enlace; si no hay app compatible, avisa sin cerrar la pantalla. */
    private fun open(action: String, uri: String) {
        try {
            startActivity(Intent(action, Uri.parse(uri)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.contact_no_app, Toast.LENGTH_LONG).show()
        }
    }
}
