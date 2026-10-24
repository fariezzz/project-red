package com.projectred.scrollguard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.projectred.scrollguard.R
import com.projectred.scrollguard.data.DiagnosticLogManager
import com.projectred.scrollguard.data.PreferenceManager
import com.projectred.scrollguard.data.ScheduleManager
import com.projectred.scrollguard.data.StatsManager
import com.projectred.scrollguard.detector.DetectionEngine
import com.projectred.scrollguard.detector.EngineEvaluationResult
import com.projectred.scrollguard.model.DiagnosticEntry
import com.projectred.scrollguard.model.RecommendedAction
import com.projectred.scrollguard.model.TargetApp

/**
 * Layanan Aksesibilitas Android untuk Project Red.
 * Bertanggung jawab memantau window event pada aplikasi target,
 * mendeteksi permukaan video pendek, dan mengeksekusi aksi navigasi yang tepat
 * (beralih ke tab aman seperti Beranda/Kotak Masuk, atau navigasi kembali).
 */
class ScrollGuardAccessibilityService : AccessibilityService() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var statsManager: StatsManager
    private lateinit var scheduleManager: ScheduleManager
    private lateinit var detectionEngine: DetectionEngine
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastToastTime = 0L

    companion object {
        const val CHANNEL_ID = "scrollguard_block_notifications"
        const val NOTIFICATION_ID = 2470

        @Volatile
        var isServiceRunning: Boolean = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferenceManager = PreferenceManager(this)
        statsManager = StatsManager(this)
        scheduleManager = ScheduleManager(this)
        detectionEngine = DetectionEngine(preferenceManager, scheduleManager)
        isServiceRunning = true

        initNotificationChannel()
        OngoingNotificationHelper.update(this)

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val targetApp = TargetApp.fromPackageName(packageName) ?: return

        // Periksa apakah service atau aplikasi target diaktifkan oleh pengguna
        if (!preferenceManager.isMasterEnabled || !preferenceManager.isAppEnabled(targetApp)) {
            return
        }

        // Dapatkan root node jendela aktif dan validasi nama paketnya
        val rootNode = rootInActiveWindow ?: return
        val rootPackage = rootNode.packageName?.toString()
        if (rootPackage != packageName) {
            return
        }

        // Ekstraksi struktur UI aktif dengan penyaringan node yang terlihat di layar
        val snapshot = NodeSnapshotExtractor.extract(rootNode) ?: return

        // Proses melalui mesin deteksi
        val evaluation = detectionEngine.process(packageName, snapshot, event.className)

        when (evaluation) {
            is EngineEvaluationResult.Blocked -> {
                val result = evaluation.result
                statsManager.recordBlock(result.targetApp)
                OngoingNotificationHelper.update(this)
                executeSmartRemediation(rootNode, result.recommendedAction, packageName, result)
            }

            is EngineEvaluationResult.BlockedWithHomeFallback -> {
                val result = evaluation.result
                statsManager.recordBlock(result.targetApp)
                OngoingNotificationHelper.update(this)
                DiagnosticLogManager.record(
                    DiagnosticEntry(
                        packageName = packageName,
                        targetApp = result.targetApp,
                        matchedRule = "${result.ruleName}_HOME_FALLBACK",
                        actionTaken = "GLOBAL_ACTION_HOME",
                        confidence = result.confidence
                    )
                )

                showFeedbackToast(result.targetApp?.displayName, "Dialihkan ke Beranda perangkat untuk mencegah siklus macet")
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

            is EngineEvaluationResult.InCooldown -> {
                // Diabaikan karena debounce / cooldown aktif
            }

            is EngineEvaluationResult.Allowed -> {
                // Halaman diizinkan (misal DM / video reguler)
            }

            is EngineEvaluationResult.Disabled,
            is EngineEvaluationResult.Ignored -> {
                // Tidak ada tindakan
            }
        }
    }

    /**
     * Menjalankan aksi perbaikan cerdas sesuai dengan jenis antarmuka yang terblokir.
     * Jika pengguna berada pada tab video pendek (misal Reels/Shorts tab), alihkan ke tab Beranda.
     * Jika berada pada video feed TikTok, alihkan ke Kotak Masuk.
     * Jika pemutar modal layar penuh, lakukan aksi BACK.
     */
    private fun executeSmartRemediation(
        rootNode: AccessibilityNodeInfo,
        action: RecommendedAction,
        packageName: String,
        result: com.projectred.scrollguard.model.DetectionResult
    ) {
        when (action) {
            RecommendedAction.SWITCH_TO_HOME_TAB -> {
                val clicked = findAndClickTab(
                    rootNode,
                    keywords = listOf("beranda", "home"),
                    idSubstrings = listOf("tab_home", "feed_tab", "pivot_bar")
                )

                if (clicked) {
                    DiagnosticLogManager.record(
                        DiagnosticEntry(
                            packageName = packageName,
                            targetApp = result.targetApp,
                            matchedRule = result.ruleName,
                            actionTaken = "SWITCH_TO_HOME_TAB",
                            confidence = result.confidence
                        )
                    )
                    showFeedbackToast(result.targetApp?.displayName, "Dialihkan ke Beranda")
                } else {
                    DiagnosticLogManager.record(
                        DiagnosticEntry(
                            packageName = packageName,
                            targetApp = result.targetApp,
                            matchedRule = result.ruleName,
                            actionTaken = "GLOBAL_ACTION_BACK",
                            confidence = result.confidence
                        )
                    )
                    showFeedbackToast(result.targetApp?.displayName, result.targetApp?.surfaceName)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }

            RecommendedAction.SWITCH_TO_INBOX_TAB -> {
                val clicked = findAndClickTab(
                    rootNode,
                    keywords = listOf("kotak masuk", "inbox", "profil", "profile", "saya"),
                    idSubstrings = listOf("tab_inbox", "tab_profile", "im_tab")
                )

                if (clicked) {
                    DiagnosticLogManager.record(
                        DiagnosticEntry(
                            packageName = packageName,
                            targetApp = result.targetApp,
                            matchedRule = result.ruleName,
                            actionTaken = "SWITCH_TO_INBOX_TAB",
                            confidence = result.confidence
                        )
                    )
                    showFeedbackToast(result.targetApp?.displayName, "Dialihkan ke Kotak Masuk")
                } else {
                    DiagnosticLogManager.record(
                        DiagnosticEntry(
                            packageName = packageName,
                            targetApp = result.targetApp,
                            matchedRule = result.ruleName,
                            actionTaken = "GLOBAL_ACTION_BACK",
                            confidence = result.confidence
                        )
                    )
                    showFeedbackToast(result.targetApp?.displayName, result.targetApp?.surfaceName)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }

            RecommendedAction.NAVIGATE_BACK -> {
                DiagnosticLogManager.record(
                    DiagnosticEntry(
                        packageName = packageName,
                        targetApp = result.targetApp,
                        matchedRule = result.ruleName,
                        actionTaken = "GLOBAL_ACTION_BACK",
                        confidence = result.confidence
                    )
                )
                showFeedbackToast(result.targetApp?.displayName, result.targetApp?.surfaceName)
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    /**
     * Mencari tab target pada pohon accessibility secara rekursif dan mengeklik elemen tersebut.
     */
    private fun findAndClickTab(
        node: AccessibilityNodeInfo?,
        keywords: List<String>,
        idSubstrings: List<String>,
        depth: Int = 0
    ): Boolean {
        if (node == null || depth > 10) return false
        if (!node.isVisibleToUser) return false

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val matchesKeyword = keywords.any { kw ->
            text == kw || desc == kw || text.contains(kw) || desc.contains(kw)
        }
        val matchesId = idSubstrings.any { sub -> viewId.contains(sub) }

        if (matchesKeyword || matchesId) {
            // Jika node ini dapat diklik secara langsung
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            // Jika node anak tidak clickable, coba klik parent-nya (hingga 2 tingkat ke atas)
            var parent = node.parent
            var upLevels = 0
            while (parent != null && upLevels < 2) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
                upLevels++
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val clicked = findAndClickTab(child, keywords, idSubstrings, depth + 1)
            if (clicked) return true
        }

        return false
    }

    override fun onInterrupt() {
        isServiceRunning = false
        OngoingNotificationHelper.cancel(this)
    }

    override fun onDestroy() {
        isServiceRunning = false
        OngoingNotificationHelper.cancel(this)
        super.onDestroy()
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notifikasi Pemblokiran",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Menampilkan pemberitahuan saat video pendek berhasil dicegah"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showFeedbackToast(appName: String?, surfaceOrMessage: String?) {
        triggerHapticFeedback()

        if (!preferenceManager.isToastFeedbackEnabled) return

        val now = System.currentTimeMillis()
        if (now - lastToastTime < 2000L) return
        lastToastTime = now

        val label = appName ?: "Aplikasi"
        val detail = surfaceOrMessage ?: "Video pendek"

        // 1. Tampilkan notifikasi sistem Android (status bar / heads-up banner)
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_shield_check)
                    .setContentTitle("Project Red: $detail")
                    .setContentText("Pemutaran $detail pada $label berhasil dicegah")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setAutoCancel(true)
                    .setTimeoutAfter(3500L)
                    .build()

                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
        }

        // 2. Tampilkan Toast menggunakan context layanan aksesibilitas
        mainHandler.post {
            try {
                Toast.makeText(
                    this,
                    "Project Red: $detail ($label) dicegah",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
            }
        }
    }

    private fun triggerHapticFeedback() {
        if (!preferenceManager.isHapticFeedbackEnabled) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(40L, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                vibrator?.vibrate(40L)
            }
        } catch (_: Exception) {
        }
    }
}
