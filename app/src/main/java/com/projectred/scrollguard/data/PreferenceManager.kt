package com.projectred.scrollguard.data

import android.content.Context
import android.content.SharedPreferences
import com.projectred.scrollguard.model.TargetApp

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isMasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MASTER_ENABLED, value).apply()

    var isToastFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TOAST, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TOAST, value).apply()

    var isPersistentNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_PERSISTENT_NOTIFICATION, true)
        set(value) = prefs.edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, value).apply()

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    var cooldownMs: Long
        get() = prefs.getLong(KEY_COOLDOWN_MS, 1000L)
        set(value) = prefs.edit().putLong(KEY_COOLDOWN_MS, value).apply()

    var isFallbackHomeEnabled: Boolean
        get() = prefs.getBoolean(KEY_FALLBACK_HOME, true)
        set(value) = prefs.edit().putBoolean(KEY_FALLBACK_HOME, value).apply()

    var snoozeUntilTimestamp: Long
        get() = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_SNOOZE_UNTIL, value).apply()

    fun isSnoozeActive(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime < snoozeUntilTimestamp
    }

    fun setSnooze(durationMinutes: Int, currentTime: Long = System.currentTimeMillis()) {
        snoozeUntilTimestamp = currentTime + (durationMinutes * 60 * 1000L)
    }

    fun clearSnooze() {
        snoozeUntilTimestamp = 0L
    }

    fun getRemainingSnoozeMinutes(currentTime: Long = System.currentTimeMillis()): Int {
        val remainingMs = snoozeUntilTimestamp - currentTime
        return if (remainingMs > 0) {
            ((remainingMs + 59999L) / 60000L).toInt()
        } else {
            0
        }
    }

    fun isAppEnabled(targetApp: TargetApp): Boolean {
        return prefs.getBoolean(KEY_APP_PREFIX + targetApp.id, targetApp.defaultEnabled)
    }

    fun setAppEnabled(targetApp: TargetApp, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_PREFIX + targetApp.id, enabled).apply()
    }

    companion object {
        private const val PREF_NAME = "scrollguard_preferences"
        private const val KEY_MASTER_ENABLED = "pref_master_enabled"
        private const val KEY_SHOW_TOAST = "pref_show_toast"
        private const val KEY_PERSISTENT_NOTIFICATION = "pref_persistent_notification"
        private const val KEY_HAPTIC_FEEDBACK = "pref_haptic_feedback"
        private const val KEY_COOLDOWN_MS = "pref_cooldown_ms"
        private const val KEY_FALLBACK_HOME = "pref_fallback_home"
        private const val KEY_SNOOZE_UNTIL = "pref_snooze_until"
        private const val KEY_APP_PREFIX = "pref_app_"
    }
}
