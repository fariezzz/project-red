package com.projectred.scrollguard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projectred.scrollguard.databinding.ItemDiagnosticEntryBinding
import com.projectred.scrollguard.model.DiagnosticEntry

class DiagnosticAdapter : RecyclerView.Adapter<DiagnosticAdapter.LogViewHolder>() {

    private val entries = mutableListOf<DiagnosticEntry>()

    fun submitList(newEntries: List<DiagnosticEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemDiagnosticEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    inner class LogViewHolder(private val binding: ItemDiagnosticEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: DiagnosticEntry) {
            val appLabel = entry.targetApp?.displayName ?: entry.packageName
            binding.tvLogApp.text = "$appLabel (${entry.packageName})"
            binding.tvLogTime.text = entry.getFormattedTime()
            binding.tvLogRule.text = "Aturan: ${entry.matchedRule} [${entry.confidence}]"
            binding.tvLogAction.text = "Aksi: ${entry.actionTaken}"
        }
    }
}
