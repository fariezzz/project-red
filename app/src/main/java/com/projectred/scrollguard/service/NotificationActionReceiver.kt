package com.projectred.scrollguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.projectred.scrollguard.data.PreferenceManager

/**
 * Receiver untuk menangani aksi tombol cepat dari Notifikasi Status Permanen
 * (On/Off Master Switch, Jeda 15 Menit, dan Lanjutkan Perlindungan).
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val preferenceManager = PreferenceManager(context)

        when (intent.action) {
            ACTION_TOGGLE_MASTER -> {
                val isSnooze = preferenceManager.isSnoozeActive()
                if (isSnooze) {
                    preferenceManager.clearSnooze()
                    preferenceManager.isMasterEnabled = true
                } else {
                    preferenceManager.isMasterEnabled = !preferenceManager.isMasterEnabled
                }
            }

            ACTION_SNOOZE_15 -> {
                preferenceManager.setSnooze(15)
            }

            ACTION_RESUME -> {
                preferenceManager.clearSnooze()
                preferenceManager.isMasterEnabled = true
            }

            ACTION_NOTIFICATION_DISMISSED -> {
                // Jika notifikasi dihapus atau di-swipe oleh pengguna, munculkan kembali secara otomatis
                if (preferenceManager.isPersistentNotificationEnabled) {
                    OngoingNotificationHelper.update(context)
                }
            }
        }

        // Perbarui tampilan status di Quick Settings Tile dan Notifikasi Permanen
        ScrollGuardTileService.requestTileUpdate(context)
        OngoingNotificationHelper.update(context)
    }

    companion object {
        const val ACTION_TOGGLE_MASTER = "com.projectred.scrollguard.ACTION_TOGGLE_MASTER"
        const val ACTION_SNOOZE_15 = "com.projectred.scrollguard.ACTION_SNOOZE_15"
        const val ACTION_RESUME = "com.projectred.scrollguard.ACTION_RESUME"
        const val ACTION_NOTIFICATION_DISMISSED = "com.projectred.scrollguard.ACTION_NOTIFICATION_DISMISSED"
    }
}
