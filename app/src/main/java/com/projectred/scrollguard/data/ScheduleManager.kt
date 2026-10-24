package com.projectred.scrollguard.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Pengatur jadwal waktu aktif perlindungan (Focus Hours / Bedtime Mode).
 * Memungkinkan pemblokiran berjalan otomatis pada rentang jam rawan (misal: jam 22.00 - 06.00),
 * atau dibebaskan di luar rentang jam tersebut.
 */
class ScheduleManager(
    private val prefs: SharedPreferences,
    private val timeProvider: () -> Pair<Int, Int> = {
        val cal = Calendar.getInstance()
        Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    )

    var isScheduleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCHEDULE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SCHEDULE_ENABLED, value).apply()

    var startHour: Int
        get() = prefs.getInt(KEY_START_HOUR, 22) // Default jam 22:00
        set(value) = prefs.edit().putInt(KEY_START_HOUR, value).apply()

    var startMinute: Int
        get() = prefs.getInt(KEY_START_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_START_MINUTE, value).apply()

    var endHour: Int
        get() = prefs.getInt(KEY_END_HOUR, 6) // Default jam 06:00
        set(value) = prefs.edit().putInt(KEY_END_HOUR, value).apply()

    var endMinute: Int
        get() = prefs.getInt(KEY_END_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_END_MINUTE, value).apply()

    /**
     * Memeriksa apakah saat ini berada dalam rentang jadwal aktif.
     */
    fun isWithinActiveSchedule(): Boolean {
        if (!isScheduleEnabled) return true // Jika jadwal tidak diaktifkan, selalu aktif sepanjang waktu

        val (currentHour, currentMinute) = timeProvider()
        return isTimeWithinRange(currentHour, currentMinute, startHour, startMinute, endHour, endMinute)
    }

    /**
     * Memformat teks ringkas rentang jadwal (misal: "22:00 - 06:00").
     */
    fun getFormattedScheduleRange(): String {
        val startStr = String.format("%02d:%02d", startHour, startMinute)
        val endStr = String.format("%02d:%02d", endHour, endMinute)
        return "$startStr - $endStr"
    }

    /**
     * Menerapkan prasetel jam tidur (22.00 - 06.00).
     */
    fun applyBedtimePreset() {
        startHour = 22
        startMinute = 0
        endHour = 6
        endMinute = 0
        isScheduleEnabled = true
    }

    /**
     * Menerapkan prasetel jam kerja/belajar (08.00 - 17.00).
     */
    fun applyWorkHoursPreset() {
        startHour = 8
        startMinute = 0
        endHour = 17
        endMinute = 0
        isScheduleEnabled = true
    }

    companion object {
        private const val PREF_NAME = "scrollguard_schedule"
        private const val KEY_SCHEDULE_ENABLED = "pref_schedule_enabled"
        private const val KEY_START_HOUR = "pref_start_hour"
        private const val KEY_START_MINUTE = "pref_start_minute"
        private const val KEY_END_HOUR = "pref_end_hour"
        private const val KEY_END_MINUTE = "pref_end_minute"

        /**
         * Logika murni evaluasi rentang waktu (termasuk yang melewati tengah malam).
         */
        fun isTimeWithinRange(
            curH: Int, curM: Int,
            startH: Int, startM: Int,
            endH: Int, endM: Int
        ): Boolean {
            val currentTotalMinutes = curH * 60 + curM
            val startTotalMinutes = startH * 60 + startM
            val endTotalMinutes = endH * 60 + endM

            return if (startTotalMinutes == endTotalMinutes) {
                true // Rentang 24 jam penuh
            } else if (startTotalMinutes < endTotalMinutes) {
                // Rentang dalam hari yang sama (misal 08:00 - 17:00)
                currentTotalMinutes in startTotalMinutes until endTotalMinutes
            } else {
                // Rentang melewati tengah malam (misal 22:00 - 06:00)
                currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes
            }
        }
    }
}
