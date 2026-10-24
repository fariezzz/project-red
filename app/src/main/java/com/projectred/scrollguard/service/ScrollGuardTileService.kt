package com.projectred.scrollguard.service

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.projectred.scrollguard.R
import com.projectred.scrollguard.data.PreferenceManager

/**
 * Layanan Quick Settings Tile untuk Android Notification Shade.
 * Memungkinkan pengguna melihat status perlindungan dan menyalakan/mematikan
 * atau membatalkan jeda langsung dari panel pengaturan cepat HP.
 */
class ScrollGuardTileService : TileService() {

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val isSnooze = preferenceManager.isSnoozeActive()
        if (isSnooze) {
            // Jika sedang snooze, klik pada tile akan langsung membatalkan snooze dan mengaktifkan perlindungan
            preferenceManager.clearSnooze()
            preferenceManager.isMasterEnabled = true
        } else {
            // Toggle status perlindungan utama
            preferenceManager.isMasterEnabled = !preferenceManager.isMasterEnabled
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        val isMasterOn = preferenceManager.isMasterEnabled
        val isSnooze = preferenceManager.isSnoozeActive()

        when {
            isSnooze -> {
                tile.state = Tile.STATE_INACTIVE
                tile.icon = Icon.createWithResource(this, R.drawable.ic_shield_alert)
                tile.label = "Project Red"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val remaining = preferenceManager.getRemainingSnoozeMinutes()
                    tile.subtitle = "Jeda ($remaining mnt)"
                }
            }
            isMasterOn -> {
                tile.state = Tile.STATE_ACTIVE
                tile.icon = Icon.createWithResource(this, R.drawable.ic_shield_check)
                tile.label = "Project Red"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Aktif"
                }
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.icon = Icon.createWithResource(this, R.drawable.ic_shield_alert)
                tile.label = "Project Red"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Mati"
                }
            }
        }

        tile.updateTile()
    }

    companion object {
        fun requestTileUpdate(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, ScrollGuardTileService::class.java)
                )
            } catch (_: Exception) {
            }
        }
    }
}
