package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class MenuActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupGreeting()
        setupMenuGrid()
        setupBackHandler()

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { confirmLogout() }
    }

    // ---------- Barra superior ----------

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                confirmLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ---------- Contenido ----------

    /** Saluda al usuario por su nombre, o con un texto genérico si no lo tiene. */
    private fun setupGreeting() {
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val displayName = auth.currentUser?.displayName?.trim()
        tvGreeting.text = if (displayName.isNullOrEmpty()) {
            getString(R.string.menu_welcome)
        } else {
            getString(R.string.menu_greeting, displayName)
        }
    }

    /** Construye la grilla de opciones del menu. */
    private fun setupMenuGrid() {
        val grid = findViewById<GridLayout>(R.id.gridMenu)
        val inflater = LayoutInflater.from(this)

        MENU_OPTIONS.forEach { option ->
            val item = inflater.inflate(R.layout.item_menu_option, grid, false)
            item.findViewById<ImageView>(R.id.ivOptionIcon).setImageResource(option.icon)
            item.findViewById<TextView>(R.id.tvOptionLabel).setText(option.label)
            item.setOnClickListener {
                Toast.makeText(this, R.string.menu_coming_soon, Toast.LENGTH_SHORT).show()
            }
            grid.addView(item)
        }
    }

    // ---------- Navegacion y salida ----------

    /** Pide confirmacion antes de abandonar la aplicacion. */
    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@MenuActivity)
                    .setTitle(R.string.exit_title)
                    .setMessage(R.string.exit_message)
                    .setPositiveButton(R.string.exit_confirm) { _, _ ->
                        finishAffinity()
                    }
                    .setNegativeButton(R.string.exit_cancel, null)
                    .show()
            }
        })
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout) { _, _ -> logout() }
            .setNegativeButton(R.string.exit_cancel, null)
            .show()
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, R.string.session_closed, Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private data class MenuOption(val label: Int, val icon: Int)

    companion object {
        private val MENU_OPTIONS = listOf(
            MenuOption(R.string.menu_option_appointments, android.R.drawable.ic_menu_my_calendar),
            MenuOption(R.string.menu_option_treatments, android.R.drawable.ic_menu_agenda),
            MenuOption(R.string.menu_option_profile, android.R.drawable.ic_menu_myplaces),
            MenuOption(R.string.menu_option_history, android.R.drawable.ic_menu_recent_history),
            MenuOption(R.string.menu_option_contact, android.R.drawable.ic_dialog_email),
            MenuOption(R.string.menu_option_help, android.R.drawable.ic_menu_help)
        )
    }
}
