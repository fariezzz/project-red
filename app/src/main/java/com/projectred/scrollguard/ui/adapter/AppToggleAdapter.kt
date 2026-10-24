package com.projectred.scrollguard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projectred.scrollguard.data.PreferenceManager
import com.projectred.scrollguard.databinding.ItemAppToggleBinding
import com.projectred.scrollguard.model.TargetApp

class AppToggleAdapter(
    private val preferenceManager: PreferenceManager,
    private val onToggleChanged: (TargetApp, Boolean) -> Unit
) : RecyclerView.Adapter<AppToggleAdapter.AppViewHolder>() {

    private val apps = TargetApp.values().toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppToggleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(private val binding: ItemAppToggleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: TargetApp) {
            binding.tvAppName.text = app.displayName
            binding.tvAppSurface.text = app.surfaceName

            // Hapus listener sementara saat mengikat nilai
            binding.switchApp.setOnCheckedChangeListener(null)
            binding.switchApp.isChecked = preferenceManager.isAppEnabled(app)

            binding.switchApp.setOnCheckedChangeListener { _, isChecked ->
                preferenceManager.setAppEnabled(app, isChecked)
                onToggleChanged(app, isChecked)
            }
        }
    }
}
