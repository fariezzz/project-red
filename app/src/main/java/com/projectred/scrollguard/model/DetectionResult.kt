package com.projectred.scrollguard.model

enum class Confidence {
    HIGH,
    MEDIUM,
    LOW,
    NONE
}

enum class RecommendedAction {
    SWITCH_TO_HOME_TAB,      // Beralih ke tab Beranda jika berada pada tab Reels/Shorts
    SWITCH_TO_INBOX_TAB,     // Beralih ke tab Kotak Masuk (khusus TikTok)
    NAVIGATE_BACK            // Tekan tombol kembali (untuk pemutar fullscreen modal)
}

data class DetectionResult(
    val isBlockedSurface: Boolean,
    val confidence: Confidence,
    val ruleName: String,
    val reason: String,
    val targetApp: TargetApp? = null,
    val recommendedAction: RecommendedAction = RecommendedAction.NAVIGATE_BACK
) {
    companion object {
        fun allowed(reason: String, targetApp: TargetApp? = null): DetectionResult {
            return DetectionResult(
                isBlockedSurface = false,
                confidence = Confidence.NONE,
                ruleName = "ALLOW_SURFACE",
                reason = reason,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        fun blocked(
            ruleName: String,
            reason: String,
            confidence: Confidence = Confidence.HIGH,
            targetApp: TargetApp? = null,
            recommendedAction: RecommendedAction = RecommendedAction.NAVIGATE_BACK
        ): DetectionResult {
            return DetectionResult(
                isBlockedSurface = true,
                confidence = confidence,
                ruleName = ruleName,
                reason = reason,
                targetApp = targetApp,
                recommendedAction = recommendedAction
            )
        }
    }
}
