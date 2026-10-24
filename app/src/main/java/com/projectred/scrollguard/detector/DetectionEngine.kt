package com.projectred.scrollguard.detector

import com.projectred.scrollguard.data.PreferenceManager
import com.projectred.scrollguard.model.DetectionResult
import com.projectred.scrollguard.model.RecommendedAction
import com.projectred.scrollguard.model.TargetApp
import com.projectred.scrollguard.model.UiNodeSnapshot
import java.util.concurrent.ConcurrentHashMap

sealed class EngineEvaluationResult {
    data class Blocked(val result: DetectionResult) : EngineEvaluationResult()
    data class BlockedWithHomeFallback(val result: DetectionResult) : EngineEvaluationResult()
    data class InCooldown(val result: DetectionResult, val message: String) : EngineEvaluationResult()
    data class Allowed(val result: DetectionResult) : EngineEvaluationResult()
    data class Disabled(val message: String) : EngineEvaluationResult()
    data class Ignored(val message: String) : EngineEvaluationResult()
}

/**
 * Mesin utama pengatur deteksi pemblokiran video pendek.
 * Mengoordinasikan detektor tiap aplikasi, memeriksa preferensi pengguna,
 * dan mengelola cooldown/debounce untuk mencegah looping tindakan berulang.
 */
class DetectionEngine(
    private val preferenceProvider: (TargetApp) -> Boolean,
    private val masterEnabledProvider: () -> Boolean,
    private val snoozeActiveProvider: () -> Boolean = { false },
    private val scheduleActiveProvider: () -> Boolean = { true },
    private val cooldownProvider: () -> Long = { 1000L },
    private val fallbackHomeProvider: () -> Boolean = { true },
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {

    constructor(
        preferenceManager: PreferenceManager,
        scheduleManager: com.projectred.scrollguard.data.ScheduleManager? = null,
        timeProvider: () -> Long = { System.currentTimeMillis() }
    ) : this(
        preferenceProvider = { app -> preferenceManager.isAppEnabled(app) },
        masterEnabledProvider = { preferenceManager.isMasterEnabled },
        snoozeActiveProvider = { preferenceManager.isSnoozeActive(timeProvider()) },
        scheduleActiveProvider = { scheduleManager?.isWithinActiveSchedule() ?: true },
        cooldownProvider = { preferenceManager.cooldownMs },
        fallbackHomeProvider = { preferenceManager.isFallbackHomeEnabled },
        timeProvider = timeProvider
    )

    private val detectors: Map<TargetApp, AppDetector> = listOf(
        InstagramDetector(),
        FacebookDetector(),
        YouTubeDetector(),
        TikTokDetector()
    ).associateBy { it.targetApp }

    // Mencatat waktu aksi pemblokiran terakhir per package
    private val lastBlockTimestamps = ConcurrentHashMap<String, Long>()

    // Mencatat jumlah pemblokiran berturut-turut untuk deteksi siklus macet (stuck loop)
    private val consecutiveBlocks = ConcurrentHashMap<String, Int>()

    /**
     * Memproses snapshot antarmuka dari aplikasi yang sedang aktif.
     */
    fun process(
        packageName: String,
        root: UiNodeSnapshot,
        windowTitle: CharSequence? = null
    ): EngineEvaluationResult {
        // 1. Identifikasi aplikasi target
        val targetApp = TargetApp.fromPackageName(packageName)
            ?: return EngineEvaluationResult.Ignored("Aplikasi $packageName bukan merupakan target pemblokiran")

        // 2. Periksa sakelar utama (Master Switch)
        if (!masterEnabledProvider()) {
            return EngineEvaluationResult.Disabled("Perlindungan Project Red sedang dimatikan")
        }

        // 3. Periksa status jeda sementara (Quick Snooze)
        if (snoozeActiveProvider()) {
            return EngineEvaluationResult.Disabled("Perlindungan sedang dijeda sementara")
        }

        // 4. Periksa jadwal aktif (Focus Hours / Bedtime Mode)
        if (!scheduleActiveProvider()) {
            return EngineEvaluationResult.Disabled("Di luar jadwal waktu perlindungan aktif")
        }

        // 5. Periksa sakelar per aplikasi
        if (!preferenceProvider(targetApp)) {
            return EngineEvaluationResult.Disabled("Perlindungan untuk ${targetApp.displayName} dinonaktifkan oleh pengguna")
        }

        // 6. Dapatkan detektor yang sesuai
        val detector = detectors[targetApp]
            ?: return EngineEvaluationResult.Ignored("Detektor untuk ${targetApp.displayName} tidak tersedia")

        // 5. Evaluasi aturan deteksi
        val result = detector.evaluate(root, windowTitle)

        val currentTime = timeProvider()
        val lastBlockTime = lastBlockTimestamps[packageName] ?: 0L
        val cooldownMs = cooldownProvider()

        if (result.isBlockedSurface) {
            // Jika jeda dari blokir sebelumnya sudah lama (> 5 detik), reset hitungan loop berturut-turut
            if (currentTime - lastBlockTime > 5000L) {
                consecutiveBlocks[packageName] = 0
            }

            // 6. Evaluasi mekanisme Cooldown / Debounce
            if (currentTime - lastBlockTime < cooldownMs) {
                return EngineEvaluationResult.InCooldown(
                    result = result,
                    message = "Aksi dilewati karena masih dalam masa cooldown (${cooldownMs}ms)"
                )
            }

            // Catat waktu eksekusi blokir
            lastBlockTimestamps[packageName] = currentTime

            val currentConsecutive = (consecutiveBlocks[packageName] ?: 0) + 1
            consecutiveBlocks[packageName] = currentConsecutive

            // Jika pengguna benar-benar terjebak dalam siklus BACK modal tanpa henti (>= 4x dalam 5 detik)
            // dan tidak ada aksi alih tab yang tersedia, picu HOME fallback
            if (currentConsecutive >= 4 && fallbackHomeProvider() && result.recommendedAction == RecommendedAction.NAVIGATE_BACK) {
                consecutiveBlocks[packageName] = 0
                return EngineEvaluationResult.BlockedWithHomeFallback(result)
            }

            return EngineEvaluationResult.Blocked(result)
        } else {
            // Jika layar diizinkan, reset hitungan pemblokiran berturut-turut
            consecutiveBlocks[packageName] = 0
            return EngineEvaluationResult.Allowed(result)
        }
    }

    /**
     * Mengatur ulang riwayat cooldown dan siklus blokir (berguna saat pengujian atau penggantian activity).
     */
    fun resetCooldowns() {
        lastBlockTimestamps.clear()
        consecutiveBlocks.clear()
    }
}
