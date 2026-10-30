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
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.the_white_smile.domain.UserRole
import com.example.the_white_smile.ui.menu.MenuEvent
import com.example.the_white_smile.ui.menu.MenuUiState
import com.example.the_white_smile.ui.menu.MenuViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Pantalla del menu principal.
 * Solo administra la vista: la sesion y el cierre los maneja [MenuViewModel].
 */
class MenuActivity : AppCompatActivity() {

    private val viewModel: MenuViewModel by viewModels { MenuViewModel.factory() }

    private lateinit var tvGreeting: TextView
    private lateinit var tvMenuDescription: TextView
    private lateinit var grid: GridLayout
    private var renderedRole: UserRole? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        bindViews()
        setupToolbar()
        setupBackHandler()
        observeViewModel()

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { confirmLogout() }
    }

    override fun onStart() {
        super.onStart()
        // Sin sesion activa no tiene sentido mostrar el menu.
        if (!viewModel.hasActiveSession()) {
            goToLogin()
            return
        }
        // Mantiene el saludo al dia despues de editar el perfil.
        viewModel.refresh()
    }

    // ---------- Barra superior ----------

    private fun bindViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvMenuDescription = findViewById(R.id.tvMenuDescription)
        grid = findViewById(R.id.gridMenu)
    }

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

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: MenuUiState) {
        tvGreeting.text = state.displayName
            ?.let { getString(R.string.menu_greeting, it) }
            ?: getString(R.string.menu_welcome)

        val role = state.role ?: return
        tvMenuDescription.setText(
            when (role) {
                UserRole.PATIENT -> R.string.menu_description
                UserRole.DENTIST -> R.string.menu_description_dentist
                UserRole.ADMIN -> R.string.menu_description_admin
            }
        )

        if (role == renderedRole) return
        renderedRole = role
        renderMenu(role)
    }

    private fun handleEvent(event: MenuEvent) {
        when (event) {
            MenuEvent.LoggedOut -> {
                toast(R.string.session_closed)
                goToLogin()
            }
        }
    }

    /** Construye la grilla de opciones correspondiente al rol. */
    private fun renderMenu(role: UserRole) {
        grid.removeAllViews()
        val inflater = LayoutInflater.from(this)

        menuOptionsFor(role).forEach { option ->
            val item = inflater.inflate(R.layout.item_menu_option, grid, false)
            item.findViewById<ImageView>(R.id.ivOptionIcon).setImageResource(option.icon)
            item.findViewById<TextView>(R.id.tvOptionLabel).setText(option.label)
            item.setOnClickListener {
                startActivity(Intent(this, option.destination))
            }
            grid.addView(item)
        }
    }

    private fun menuOptionsFor(role: UserRole): List<MenuOption> = when (role) {
        UserRole.PATIENT -> listOf(
            MenuOption(
                R.string.menu_option_appointments,
                android.R.drawable.ic_menu_my_calendar,
                AppointmentsActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_treatments,
                android.R.drawable.ic_menu_agenda,
                TreatmentsActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_profile,
                android.R.drawable.ic_menu_myplaces,
                ProfileActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_history,
                android.R.drawable.ic_menu_recent_history,
                HistoryActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_contact,
                android.R.drawable.ic_dialog_email,
                ContactActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_help,
                android.R.drawable.ic_menu_help,
                HelpActivity::class.java
            )
        )
        UserRole.DENTIST -> listOf(
            MenuOption(
                R.string.menu_option_agenda,
                android.R.drawable.ic_menu_my_calendar,
                DentistAgendaActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_treatments,
                android.R.drawable.ic_menu_agenda,
                TreatmentsActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_profile,
                android.R.drawable.ic_menu_myplaces,
                ProfileActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_contact,
                android.R.drawable.ic_dialog_email,
                ContactActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_help,
                android.R.drawable.ic_menu_help,
                HelpActivity::class.java
            )
        )
        UserRole.ADMIN -> listOf(
            MenuOption(
                R.string.menu_option_dentists,
                android.R.drawable.ic_menu_manage,
                DentistsActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_treatments,
                android.R.drawable.ic_menu_agenda,
                TreatmentsActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_profile,
                android.R.drawable.ic_menu_myplaces,
                ProfileActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_contact,
                android.R.drawable.ic_dialog_email,
                ContactActivity::class.java
            ),
            MenuOption(
                R.string.menu_option_help,
                android.R.drawable.ic_menu_help,
                HelpActivity::class.java
            )
        )
    }

    // ---------- Navegacion y salida ----------

    /** Pide confirmacion antes de abandonar la aplicacion. */
    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@MenuActivity)
                    .setTitle(R.string.exit_title)
                    .setMessage(R.string.exit_message)
                    .setPositiveButton(R.string.exit_confirm) { _, _ -> finishAffinity() }
                    .setNegativeButton(R.string.exit_cancel, null)
                    .show()
            }
        })
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout) { _, _ -> viewModel.logout() }
            .setNegativeButton(R.string.exit_cancel, null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private data class MenuOption(
        val label: Int,
        val icon: Int,
        val destination: Class<*>
    )
}
