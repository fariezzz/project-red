package com.projectred.scrollguard.data

import android.content.Context
import android.content.SharedPreferences
import com.projectred.scrollguard.model.TargetApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Pengelola statistik pemblokiran video pendek dan estimasi waktu yang terselamatkan.
 * 100% tersimpan lokal di perangkat pengguna melalui SharedPreferences.
 */
class StatsManager(
    private val prefs: SharedPreferences,
    private val dateProvider: () -> String = {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    )

    companion object {
        private const val PREF_NAME = "scrollguard_stats"
        private const val KEY_TOTAL_ALL_TIME = "stats_total_all_time"
        private const val KEY_PREFIX_DAILY = "stats_daily_"
        private const val MINUTES_SAVED_PER_BLOCK = 3 // Rata-rata sesi doomscrolling dicegah ~3 menit

        private val listeners = CopyOnWriteArrayList<() -> Unit>()

        fun addListener(listener: () -> Unit) {
            listeners.add(listener)
        }

        fun removeListener(listener: () -> Unit) {
            listeners.remove(listener)
        }

        private fun notifyListeners() {
            for (listener in listeners) {
                try {
                    listener()
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Mencatat satu kejadian pemblokiran untuk aplikasi target.
     */
    fun recordBlock(targetApp: TargetApp?) {
        val today = dateProvider()
        val editor = prefs.edit()

        // 1. Tambah total all-time
        val currentAllTime = prefs.getInt(KEY_TOTAL_ALL_TIME, 0)
        editor.putInt(KEY_TOTAL_ALL_TIME, currentAllTime + 1)

        // 2. Tambah total harian
        val dailyTotalKey = "${KEY_PREFIX_DAILY}${today}_total"
        val currentDailyTotal = prefs.getInt(dailyTotalKey, 0)
        editor.putInt(dailyTotalKey, currentDailyTotal + 1)

        // 3. Tambah total harian per-aplikasi jika ada
        if (targetApp != null) {
            val appDailyKey = "${KEY_PREFIX_DAILY}${today}_${targetApp.id}"
            val currentAppDaily = prefs.getInt(appDailyKey, 0)
            editor.putInt(appDailyKey, currentAppDaily + 1)
        }

        editor.apply()
        notifyListeners()
    }

    /**
     * Mendapatkan total pemblokiran hari ini.
     */
    fun getTodayTotalBlocks(): Int {
        val today = dateProvider()
        val dailyTotalKey = "${KEY_PREFIX_DAILY}${today}_total"
        return prefs.getInt(dailyTotalKey, 0)
    }

    /**
     * Mendapatkan total pemblokiran hari ini untuk aplikasi spesifik.
     */
    fun getTodayBlocksForApp(targetApp: TargetApp): Int {
        val today = dateProvider()
        val appDailyKey = "${KEY_PREFIX_DAILY}${today}_${targetApp.id}"
        return prefs.getInt(appDailyKey, 0)
    }

    /**
     * Mendapatkan estimasi menit yang terselamatkan hari ini (1 blokir ~ 3 menit).
     */
    fun getTodayTimeSavedMinutes(): Int {
        return getTodayTotalBlocks() * MINUTES_SAVED_PER_BLOCK
    }

    /**
     * Mendapatkan total pemblokiran sepanjang masa.
     */
    fun getAllTimeTotalBlocks(): Int {
        return prefs.getInt(KEY_TOTAL_ALL_TIME, 0)
    }

    /**
     * Menghapus seluruh rekaman statistik.
     */
    fun clearStats() {
        prefs.edit().clear().apply()
        notifyListeners()
    }
}
