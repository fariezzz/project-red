package com.projectred.scrollguard.detector

import com.projectred.scrollguard.model.Confidence
import com.projectred.scrollguard.model.DetectionResult
import com.projectred.scrollguard.model.RecommendedAction
import com.projectred.scrollguard.model.TargetApp
import com.projectred.scrollguard.model.UiNodeSnapshot

/**
 * Detektor untuk YouTube Shorts (com.google.android.youtube).
 * Memblokir pemutar YouTube Shorts dan tab Shorts pada bilah navigasi,
 * namun secara ketat melindungi pemutaran video YouTube biasa, pencarian, dan beranda.
 */
class YouTubeDetector : AppDetector {

    override val targetApp: TargetApp = TargetApp.YOUTUBE

    override fun evaluate(root: UiNodeSnapshot, windowTitle: CharSequence?): DetectionResult {
        // 1. FILTER PERLINDUNGAN PEMUTAR VIDEO BIASA (Standard Long-form Player)
        if (isStandardVideoPlayer(root)) {
            return DetectionResult.allowed(
                reason = "Pemutaran video reguler YouTube terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        // 2. DETEKSI UTAMA: Penampil YouTube Shorts Berdedikasi (Layar Penuh / Modal)
        // PENTING: Dievaluasi SEBELUM filter Beranda/Langganan agar jika pengguna membuka Shorts
        // melalui rak/shelf horizontal di feed Beranda, pemutar Shorts tetap terblokir dengan NAVIGATE_BACK.
        val shortsRule = checkDedicatedShortsPlayer(root)
        if (shortsRule != null) {
            return DetectionResult.blocked(
                ruleName = shortsRule.first,
                reason = shortsRule.second,
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 3. DETEKSI KOMBINASI KONTROL KHAS SHORTS (remix, pivot musik, tombol interaksi Shorts)
        if (hasShortsSpecificControls(root)) {
            return DetectionResult.blocked(
                ruleName = "YT_SHORTS_CONTROLS_COMBINATION",
                reason = "Kombinasi kontrol khas Shorts (remix, pivot suara, layout vertikal) terdeteksi",
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 4. Periksa apakah tab Shorts pada bilah navigasi bawah (pivot_bar) terpilih
        if (isShortsPivotTabSelected(root)) {
            return DetectionResult.blocked(
                ruleName = "YT_SHORTS_TAB_SELECTED",
                reason = "Tab Shorts pada bilah navigasi bawah sedang aktif",
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.SWITCH_TO_HOME_TAB
            )
        }

        // 5. FILTER PERLINDUNGAN: Beranda atau Tab Langganan Aktif (Feed biasa aman)
        // Jika tab Beranda atau Langganan aktif dan bukan pemutar Shorts, feed diizinkan.
        // Rak horizontal Shorts yang ada di feed Beranda tidak memblokir feed beranda.
        if (isHomeOrSubscriptionsTabActive(root)) {
            return DetectionResult.allowed(
                reason = "Layar Beranda/Langganan YouTube aktif - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        return DetectionResult.allowed(
            reason = "Bukan tampilan YouTube Shorts (beranda, pencarian, atau langganan)",
            targetApp = targetApp
        )
    }

    /**
     * Memeriksa apakah layar saat ini menampilkan pemutar video biasa (panjang).
     */
    private fun isStandardVideoPlayer(root: UiNodeSnapshot): Boolean {
        if (root.hasIdContaining("reel_recycler") ||
            root.hasIdContaining("shorts_player") ||
            root.hasIdContaining("reel_player_fragment") ||
            root.hasIdContaining("reel_watch_fragment") ||
            root.hasIdContaining("shorts_container") ||
            hasShortsSpecificControls(root)
        ) {
            return false
        }

        val hasTimeBar = root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("time_bar") ||
                id.contains("inline_time_bar") ||
                id.contains("scrub_bar") ||
                id.contains("progress_bar_seek")
        }

        val hasStandardPlayerMetadata = root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("watch_metadata") ||
                id.contains("watch_panel") ||
                id.contains("related_videos") ||
                id.contains("playlist_header")
        }

        return hasTimeBar || hasStandardPlayerMetadata
    }

    /**
     * Memeriksa apakah tab Shorts pada pivot bar dipilih (isSelected = true).
     */
    private fun isShortsPivotTabSelected(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val isShorts = (node.contentDescription?.contains("Shorts", ignoreCase = true) == true) ||
                (node.text?.equals("Shorts", ignoreCase = true) == true) ||
                (node.viewIdResourceName?.contains("shorts_tab", ignoreCase = true) == true)

            isShorts && node.isSelected
        }
    }

    /**
     * Memeriksa apakah tab Beranda atau Langganan aktif.
     */
    private fun isHomeOrSubscriptionsTabActive(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            val isNonShortsTab = desc.contains("beranda") || desc.contains("home") ||
                desc.contains("langganan") || desc.contains("subscriptions") ||
                desc.contains("koleksi") || desc.contains("you")

            isNonShortsTab && node.isSelected
        }
    }

    /**
     * Memeriksa ID komponen pemutar Shorts YouTube.
     */
    private fun checkDedicatedShortsPlayer(root: UiNodeSnapshot): Pair<String, String>? {
        return when {
            root.hasIdContaining("reel_recycler") ->
                "YT_REEL_RECYCLER" to "Daftar gulir vertikal Reel/Shorts aktif"
            root.hasIdContaining("reel_player_fragment_container") ->
                "YT_REEL_FRAGMENT" to "Fragment pemutar Reel/Shorts terdeteksi"
            root.hasIdContaining("shorts_player") ->
                "YT_SHORTS_PLAYER" to "Kontainer Shorts Player aktif"
            root.hasIdContaining("reel_watch_fragment") ->
                "YT_REEL_WATCH" to "Layar tontonan Reel/Shorts aktif"
            root.hasIdContaining("reel_player_page") ->
                "YT_REEL_PLAYER_PAGE" to "Halaman pemutar Reel/Shorts aktif"
            root.hasIdContaining("shorts_container") ->
                "YT_SHORTS_CONTAINER" to "Kontainer pemutar Shorts aktif"
            root.hasIdContaining("reel_video_player") ->
                "YT_REEL_VIDEO_PLAYER" to "Pemutar video Shorts aktif"
            else -> null
        }
    }

    /**
     * Memeriksa kontrol samping khas YouTube Shorts (remix sound, pivot lagu, dsb).
     */
    private fun hasShortsSpecificControls(root: UiNodeSnapshot): Boolean {
        var score = 0

        if (root.any { node ->
            val d = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            d.contains("remix") || d.contains("gunakan suara ini") || d.contains("use this sound")
        }) {
            score += 2
        }

        if (root.hasIdContaining("reel_sound_pivot_button") ||
            root.hasIdContaining("sound_button") ||
            root.hasIdContaining("sound_pivot_button") ||
            root.hasIdContaining("music_pivot")
        ) {
            score += 2
        }

        if (root.hasIdContaining("reel_like_button") ||
            root.hasIdContaining("reel_dislike_button") ||
            root.hasIdContaining("reel_comment_button") ||
            root.hasIdContaining("reel_share_button")
        ) {
            score += 2
        }

        if (root.hasIdContaining("reel_player_overlay") ||
            root.hasIdContaining("shorts_overlay") ||
            root.hasIdContaining("reel_right_side_bar")
        ) {
            score += 2
        }

        return score >= 3
    }
}
