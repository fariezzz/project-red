package com.projectred.scrollguard.detector

import com.projectred.scrollguard.model.TargetApp
import com.projectred.scrollguard.model.UiNodeSnapshot
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DetectionEngineTest {

    private var currentTime = 10000L
    private var isMasterEnabled = true
    private var isSnoozeActive = false
    private var isScheduleActive = true
    private val appEnabledMap = mutableMapOf<TargetApp, Boolean>()
    private var cooldownMs = 1000L
    private var isFallbackHomeEnabled = true

    private lateinit var engine: DetectionEngine

    @Before
    fun setUp() {
        currentTime = 10000L
        isMasterEnabled = true
        isSnoozeActive = false
        isScheduleActive = true
        appEnabledMap.clear()
        TargetApp.values().forEach { appEnabledMap[it] = true }
        cooldownMs = 1000L
        isFallbackHomeEnabled = true

        engine = DetectionEngine(
            preferenceProvider = { app -> appEnabledMap[app] ?: true },
            masterEnabledProvider = { isMasterEnabled },
            snoozeActiveProvider = { isSnoozeActive },
            scheduleActiveProvider = { isScheduleActive },
            cooldownProvider = { cooldownMs },
            fallbackHomeProvider = { isFallbackHomeEnabled },
            timeProvider = { currentTime }
        )
    }

    private fun createInstagramReelsSnapshot(): UiNodeSnapshot {
        return UiNodeSnapshot(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            children = listOf(
                UiNodeSnapshot(viewIdResourceName = "com.instagram.android:id/clips_viewer_like_button")
            )
        )
    }

    @Test
    fun `test non-target application is ignored`() {
        val root = UiNodeSnapshot(text = "Chat WhatsApp")
        val result = engine.process("com.whatsapp", root)

        assertTrue(result is EngineEvaluationResult.Ignored)
    }

    @Test
    fun `test master switch disabled prevents blocking`() {
        isMasterEnabled = false
        val root = createInstagramReelsSnapshot()
        val result = engine.process("com.instagram.android", root)

        assertTrue("Jika master switch mati, evaluasi harus Disabled", result is EngineEvaluationResult.Disabled)
    }

    @Test
    fun `test app specific switch disabled prevents blocking for that app only`() {
        appEnabledMap[TargetApp.INSTAGRAM] = false
        val root = createInstagramReelsSnapshot()
        val result = engine.process("com.instagram.android", root)

        assertTrue("Instagram harus dinonaktifkan", result is EngineEvaluationResult.Disabled)

        // Verifikasi aplikasi lain tetap aktif
        val ytRoot = UiNodeSnapshot(viewIdResourceName = "com.google.android.youtube:id/reel_recycler")
        val ytResult = engine.process("com.google.android.youtube", ytRoot)
        assertTrue("YouTube harus tetap diblokir", ytResult is EngineEvaluationResult.Blocked)
    }

    @Test
    fun `test debounce cooldown prevents rapid duplicate block actions`() {
        val root = createInstagramReelsSnapshot()

        // Panggilan pertama pada t = 10000 -> Blocked
        val firstResult = engine.process("com.instagram.android", root)
        assertTrue("Panggilan pertama harus Blocked", firstResult is EngineEvaluationResult.Blocked)

        // Panggilan kedua 200ms kemudian pada t = 10200 -> InCooldown
        currentTime += 200L
        val secondResult = engine.process("com.instagram.android", root)
        assertTrue("Panggilan dalam periode cooldown harus InCooldown", secondResult is EngineEvaluationResult.InCooldown)

        // Panggilan ketiga 500ms kemudian pada t = 10700 -> InCooldown
        currentTime += 500L
        val thirdResult = engine.process("com.instagram.android", root)
        assertTrue("Panggilan masih dalam cooldown harus InCooldown", thirdResult is EngineEvaluationResult.InCooldown)

        // Panggilan keempat setelah cooldown 1000ms lewat (t = 11100) -> Blocked
        currentTime += 400L
        val fourthResult = engine.process("com.instagram.android", root)
        assertTrue("Panggilan setelah cooldown selesai harus Blocked", fourthResult is EngineEvaluationResult.Blocked)
    }

    @Test
    fun `test stuck loop triggers fallback to HOME action`() {
        val root = createInstagramReelsSnapshot()

        // Blokir 1
        val r1 = engine.process("com.instagram.android", root)
        assertTrue(r1 is EngineEvaluationResult.Blocked)

        // Blokir 2 (setelah cooldown 1100ms)
        currentTime += 1100L
        val r2 = engine.process("com.instagram.android", root)
        assertTrue(r2 is EngineEvaluationResult.Blocked)

        // Blokir 3 (setelah cooldown 1100ms)
        currentTime += 1100L
        val r3 = engine.process("com.instagram.android", root)
        assertTrue(r3 is EngineEvaluationResult.Blocked)

        // Blokir 4 berturut-turut dalam 5 detik -> Terpicu HOME fallback
        currentTime += 1100L
        val r4 = engine.process("com.instagram.android", root)
        assertTrue("Blokir berturut-turut harus memicu fallback ke HOME", r4 is EngineEvaluationResult.BlockedWithHomeFallback)
    }

    @Test
    fun `test unknown UI on target app is conservatively allowed without repeated back action`() {
        val unknownRoot = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                UiNodeSnapshot(text = "Izinkan akses kontak?")
            )
        )

        val result = engine.process("com.instagram.android", unknownRoot)
        assertTrue("UI tidak dikenal harus diizinkan (konservatif)", result is EngineEvaluationResult.Allowed)
    }

    @Test
    fun `test snooze active prevents blocking and resumes after snooze ends`() {
        val root = createInstagramReelsSnapshot()

        // 1. Kondisi normal -> Terblokir
        val normalResult = engine.process("com.instagram.android", root)
        assertTrue("Normal harus terblokir", normalResult is EngineEvaluationResult.Blocked)

        // 2. Aktifkan Snooze -> Harus Disabled (dijeda)
        isSnoozeActive = true
        currentTime += 2000L
        val snoozedResult = engine.process("com.instagram.android", root)
        assertTrue("Saat snooze aktif evaluasi harus Disabled", snoozedResult is EngineEvaluationResult.Disabled)

        // 3. Matikan Snooze -> Kembali terblokir
        isSnoozeActive = false
        currentTime += 2000L
        val resumedResult = engine.process("com.instagram.android", root)
        assertTrue("Setelah jeda selesai harus kembali Blocked", resumedResult is EngineEvaluationResult.Blocked)
    }

    @Test
    fun `test outside schedule prevents blocking`() {
        val root = createInstagramReelsSnapshot()

        // Di dalam jadwal aktif -> Blocked
        isScheduleActive = true
        val inSchedule = engine.process("com.instagram.android", root)
        assertTrue("Di dalam jadwal harus Blocked", inSchedule is EngineEvaluationResult.Blocked)

        // Di luar jadwal aktif -> Disabled
        isScheduleActive = false
        currentTime += 2000L
        val outOfSchedule = engine.process("com.instagram.android", root)
        assertTrue("Di luar jadwal harus Disabled", outOfSchedule is EngineEvaluationResult.Disabled)
    }
}
