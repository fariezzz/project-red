package com.projectred.scrollguard.detector

import com.projectred.scrollguard.model.Confidence
import com.projectred.scrollguard.model.DetectionResult
import com.projectred.scrollguard.model.RecommendedAction
import com.projectred.scrollguard.model.TargetApp
import com.projectred.scrollguard.model.UiNodeSnapshot

/**
 * Detektor untuk Instagram Reels (com.instagram.android).
 * Memblokir halaman pemutaran Reels dan tab Reels terpilih,
 * namun secara ketat melindungi Direct Messages (DM), Cerita (Stories), dan Feed Beranda umum.
 */
class InstagramDetector : AppDetector {

    override val targetApp: TargetApp = TargetApp.INSTAGRAM

    override fun evaluate(root: UiNodeSnapshot, windowTitle: CharSequence?): DetectionResult {
        // 1. FILTER PRIORITAS TERTINGGI: Lindungi Direct Messages (Chat / DM)
        if (isInstagramChatScreen(root)) {
            return DetectionResult.allowed(
                reason = "Layar obrolan Instagram DM terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        // 2. FILTER PRIORITAS: Lindungi Cerita Instagram (Stories)
        // Stories internal Instagram sering menggunakan prefiks 'reel_', tetapi BUKAN Reels video pendek
        if (isStoryViewer(root)) {
            return DetectionResult.allowed(
                reason = "Layar Cerita Instagram (Stories) terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        // 3. DETEKSI UTAMA: Penampil Reels Layar Penuh Berdedikasi (Clips Viewer)
        // PENTING: Dievaluasi SEBELUM filter Beranda agar jika pengguna mengetuk kartu Reels
        // yang disematkan di feed Beranda atau membuka dari jelajah, penampil layar penuh tetap diblokir dengan NAVIGATE_BACK.
        val fullscreenViewerRule = checkFullscreenClipsViewer(root)
        if (fullscreenViewerRule != null) {
            return DetectionResult.blocked(
                ruleName = fullscreenViewerRule.first,
                reason = fullscreenViewerRule.second,
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 4. Periksa apakah tab navigasi "Reels" pada bilah navigasi utama sedang terpilih
        if (isReelsTabSelected(root)) {
            return DetectionResult.blocked(
                ruleName = "IG_REELS_TAB_SELECTED",
                reason = "Tab Reels pada bilah navigasi utama sedang aktif",
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.SWITCH_TO_HOME_TAB
            )
        }

        // 5. FILTER PRIORITAS: Lindungi Feed Beranda Utama (Home Feed)
        // Jika tab Beranda aktif dan bukan penampil layar penuh, feed beranda diizinkan.
        if (isHomeFeedActive(root)) {
            return DetectionResult.allowed(
                reason = "Layar Beranda Utama Instagram (Home Feed) terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        return DetectionResult.allowed(
            reason = "Bukan tampilan Reels layar penuh (beranda, profil, atau jelajah)",
            targetApp = targetApp
        )
    }

    /**
     * Memeriksa apakah layar saat ini merupakan obrolan DM.
     */
    private fun isInstagramChatScreen(root: UiNodeSnapshot): Boolean {
        val hasChatId = root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("row_thread_composer") ||
                id.contains("message_composer") ||
                id.contains("direct_thread") ||
                id.contains("direct_text_message") ||
                id.contains("message_list") ||
                id.contains("thread_title") ||
                id.contains("voice_clip_button") ||
                id.contains("camera_button_composer")
        }

        if (hasChatId) return true

        val hasMessagePlaceholder = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.lowercase() ?: ""
            t.startsWith("kirim pesan") ||
                t.startsWith("message...") ||
                t.startsWith("kirim balasan") ||
                t.contains("tulis pesan")
        }

        val hasComposerInput = root.hasEditableField() && (hasMessagePlaceholder || root.hasIdContaining("composer"))
        return hasMessagePlaceholder || hasComposerInput
    }

    /**
     * Memeriksa apakah tampilan saat ini adalah Cerita Instagram (Stories).
     * Stories memiliki segmen progress bar di bagian paling atas atau tombol kirim pesan story.
     */
    private fun isStoryViewer(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("reel_viewer_progress_bar") ||
                id.contains("toolbar_stories") ||
                id.contains("story_progress_bar") ||
                id.contains("viewer_media_view")
        }
    }

    /**
     * Memeriksa apakah tab navigasi "Reels" di bagian bawah layar berstatus isSelected = true.
     */
    private fun isReelsTabSelected(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val isReelsNode = (node.contentDescription?.contains("Reels", ignoreCase = true) == true) ||
                (node.text?.contains("Reels", ignoreCase = true) == true) ||
                (node.viewIdResourceName?.contains("tab_reels", ignoreCase = true) == true) ||
                (node.viewIdResourceName?.contains("clips_tab", ignoreCase = true) == true)

            isReelsNode && node.isSelected
        }
    }

    /**
     * Memeriksa apakah tab Beranda (Home) sedang aktif terpilih.
     */
    private fun isHomeFeedActive(root: UiNodeSnapshot): Boolean {
        // Tab Beranda terpilih di bawah
        val isHomeTabSelected = root.any { node ->
            val isHome = (node.contentDescription?.contains("Beranda", ignoreCase = true) == true) ||
                (node.contentDescription?.contains("Home", ignoreCase = true) == true) ||
                (node.viewIdResourceName?.contains("tab_home", ignoreCase = true) == true) ||
                (node.viewIdResourceName?.contains("feed_tab", ignoreCase = true) == true)
            isHome && node.isSelected
        }

        // Tanda khas feed utama
        val hasMainFeedElements = root.hasIdContaining("feed_recycler_view") ||
            root.hasIdContaining("main_feed") ||
            root.hasIdContaining("action_bar_search_button")

        return isHomeTabSelected || hasMainFeedElements
    }

    /**
     * Memeriksa penampil Reels layar penuh (Clips Viewer) saat dibuka dari tautan atau jelajah.
     * Harus memiliki kontainer clips_viewer_view_pager DAN kontrol khas Reels layar penuh.
     */
    private fun checkFullscreenClipsViewer(root: UiNodeSnapshot): Pair<String, String>? {
        val hasClipsPager = root.hasIdContaining("clips_viewer_view_pager") ||
            (root.hasIdContaining("clips_swipe_refresh_layout") && !root.hasIdContaining("feed_recycler_view"))

        if (!hasClipsPager) return null

        // Harus memiliki kontrol interaksi khas Reels vertikal
        val hasViewerControls = root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""

            id.contains("clips_viewer_like_button") ||
                id.contains("clips_viewer_audio_track") ||
                id.contains("clips_author_button") ||
                id.contains("clips_action_button") ||
                id.contains("clips_ufi") ||
                desc.contains("audio asli") ||
                desc.contains("original audio") ||
                desc.contains("suara asli") ||
                desc.contains("remix")
        }

        return if (hasViewerControls) {
            "IG_CLIPS_FULLSCREEN_VIEWER" to "Penampil Reels layar penuh terdeteksi"
        } else {
            null
        }
    }
}
