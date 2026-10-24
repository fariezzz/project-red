package com.projectred.scrollguard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleManagerTest {

    @Test
    fun `test daytime schedule range`() {
        // Rentang kerja 08:00 sampai 17:00
        val startH = 8
        val startM = 0
        val endH = 17
        val endM = 0

        assertFalse(ScheduleManager.isTimeWithinRange(7, 59, startH, startM, endH, endM))
        assertTrue(ScheduleManager.isTimeWithinRange(8, 0, startH, startM, endH, endM))
        assertTrue(ScheduleManager.isTimeWithinRange(12, 30, startH, startM, endH, endM))
        assertTrue(ScheduleManager.isTimeWithinRange(16, 59, startH, startM, endH, endM))
        assertFalse(ScheduleManager.isTimeWithinRange(17, 0, startH, startM, endH, endM))
        assertFalse(ScheduleManager.isTimeWithinRange(20, 0, startH, startM, endH, endM))
    }

    @Test
    fun `test overnight bedtime schedule range crossing midnight`() {
        // Rentang jam tidur 22:00 sampai 06:00 (melewati tengah malam)
        val startH = 22
        val startM = 0
        val endH = 6
        val endM = 0

        assertFalse(ScheduleManager.isTimeWithinRange(21, 59, startH, startM, endH, endM))
        assertTrue(ScheduleManager.isTimeWithinRange(22, 0, startH, startM, endH, endM))
        assertTrue(ScheduleManager.isTimeWithinRange(23, 45, startH, startM, endH, endM))
        assertTrue(ScheduleManager.isTimeWithinRange(0, 0, startH, startM, endH, endM))
        assertTrue(ScheduleManager.isTimeWithinRange(3, 15, startH, startM, endH, endM))
        assertTrue(ScheduleManager.isTimeWithinRange(5, 59, startH, startM, endH, endM))
        assertFalse(ScheduleManager.isTimeWithinRange(6, 0, startH, startM, endH, endM))
        assertFalse(ScheduleManager.isTimeWithinRange(14, 0, startH, startM, endH, endM))
    }

    @Test
    fun `test 24 hours full day schedule range`() {
        // Start == End -> 24 jam penuh
        assertTrue(ScheduleManager.isTimeWithinRange(10, 0, 8, 0, 8, 0))
    }
}
