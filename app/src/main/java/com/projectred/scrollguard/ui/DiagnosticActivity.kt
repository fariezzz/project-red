package com.projectred.scrollguard.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.projectred.scrollguard.data.DiagnosticLogManager
import com.projectred.scrollguard.databinding.ActivityDiagnosticBinding
import com.projectred.scrollguard.ui.adapter.DiagnosticAdapter

class DiagnosticActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiagnosticBinding
    private val adapter = DiagnosticAdapter()

    private val logListener = {
        runOnUiThread {
            loadEntries()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvDiagnosticLogs.layoutManager = LinearLayoutManager(this)
        binding.rvDiagnosticLogs.adapter = adapter

        binding.btnClearLog.setOnClickListener {
            DiagnosticLogManager.clear()
        }

        DiagnosticLogManager.addListener(logListener)
        loadEntries()
    }

    override fun onDestroy() {
        DiagnosticLogManager.removeListener(logListener)
        super.onDestroy()
    }

    private fun loadEntries() {
        val entries = DiagnosticLogManager.getEntries()
        adapter.submitList(entries)

        if (entries.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvDiagnosticLogs.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvDiagnosticLogs.visibility = View.VISIBLE
        }
    }
}
