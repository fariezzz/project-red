package com.projectred.scrollguard.data

import android.content.SharedPreferences
import com.projectred.scrollguard.model.TargetApp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StatsManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private var currentDate = "2026-10-23"
    private lateinit var statsManager: StatsManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        currentDate = "2026-10-23"
        statsManager = StatsManager(
            prefs = fakePrefs,
            dateProvider = { currentDate }
        )
    }

    @Test
    fun `test initial stats are zero`() {
        assertEquals(0, statsManager.getTodayTotalBlocks())
        assertEquals(0, statsManager.getTodayTimeSavedMinutes())
        assertEquals(0, statsManager.getAllTimeTotalBlocks())
        assertEquals(0, statsManager.getTodayBlocksForApp(TargetApp.INSTAGRAM))
    }

    @Test
    fun `test record block updates daily total and app count`() {
        statsManager.recordBlock(TargetApp.INSTAGRAM)
        statsManager.recordBlock(TargetApp.INSTAGRAM)
        statsManager.recordBlock(TargetApp.YOUTUBE)

        assertEquals(3, statsManager.getTodayTotalBlocks())
        assertEquals(9, statsManager.getTodayTimeSavedMinutes()) // 3 * 3 menit = 9 menit
        assertEquals(2, statsManager.getTodayBlocksForApp(TargetApp.INSTAGRAM))
        assertEquals(1, statsManager.getTodayBlocksForApp(TargetApp.YOUTUBE))
        assertEquals(0, statsManager.getTodayBlocksForApp(TargetApp.TIKTOK))
        assertEquals(3, statsManager.getAllTimeTotalBlocks())
    }

    @Test
    fun `test date change resets today stats but keeps all-time stats`() {
        statsManager.recordBlock(TargetApp.FACEBOOK)
        statsManager.recordBlock(TargetApp.TIKTOK)

        assertEquals(2, statsManager.getTodayTotalBlocks())
        assertEquals(6, statsManager.getTodayTimeSavedMinutes())

        // Ganti hari berikutnya
        currentDate = "2026-10-24"

        assertEquals(0, statsManager.getTodayTotalBlocks())
        assertEquals(0, statsManager.getTodayTimeSavedMinutes())
        assertEquals(2, statsManager.getAllTimeTotalBlocks()) // All-time tetap bertahan

        // Catat blokir di hari baru
        statsManager.recordBlock(TargetApp.TIKTOK)
        assertEquals(1, statsManager.getTodayTotalBlocks())
        assertEquals(3, statsManager.getAllTimeTotalBlocks())
    }

    @Test
    fun `test clear stats resets all data`() {
        statsManager.recordBlock(TargetApp.INSTAGRAM)
        assertEquals(1, statsManager.getTodayTotalBlocks())

        statsManager.clearStats()

        assertEquals(0, statsManager.getTodayTotalBlocks())
        assertEquals(0, statsManager.getAllTimeTotalBlocks())
    }

    /**
     * Implementasi in-memory SharedPreferences murni untuk JVM testing tanpa ketergantungan framework Android.
     */
    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = data

        override fun getString(key: String?, defValue: String?): String? =
            data[key] as? String ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? MutableSet<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            data[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            data[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            data[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            data[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(data)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val backingMap: MutableMap<String, Any>) : SharedPreferences.Editor {
            private val tempMap = mutableMapOf<String, Any>()
            private var clearFlag = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null && value != null) tempMap[key] = value
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null && values != null) tempMap[key] = values
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) backingMap.remove(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearFlag = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearFlag) {
                    backingMap.clear()
                }
                backingMap.putAll(tempMap)
            }
        }
    }
}
