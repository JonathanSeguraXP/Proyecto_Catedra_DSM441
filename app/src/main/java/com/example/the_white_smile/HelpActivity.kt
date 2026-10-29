package com.example.the_white_smile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.ui.help.HelpAdapter
import com.example.the_white_smile.ui.help.HelpUiState
import com.example.the_white_smile.ui.help.HelpViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Pantalla de ayuda con preguntas frecuentes desplegables.
 * Solo administra la vista: las preguntas y la respuesta abierta las
 * maneja [HelpViewModel].
 */
class HelpActivity : AppCompatActivity() {

    private val viewModel: HelpViewModel by viewModels {
        HelpViewModel.factory()
    }

    private lateinit var rvFaq: RecyclerView
    private lateinit var adapter: HelpAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        setupToolbar()
        setupList()
        setupListeners()
        observeViewModel()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarHelp)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupList() {
        rvFaq = findViewById(R.id.rvFaq)
        adapter = HelpAdapter { faq -> viewModel.toggle(faq.id) }
        rvFaq.adapter = adapter
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.btnHelpContact).setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: HelpUiState) {
        adapter.submitList(state.entries)
    }
}
