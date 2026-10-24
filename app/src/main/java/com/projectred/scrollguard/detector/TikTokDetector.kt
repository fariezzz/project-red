package com.projectred.scrollguard.detector

import com.projectred.scrollguard.model.Confidence
import com.projectred.scrollguard.model.DetectionResult
import com.projectred.scrollguard.model.RecommendedAction
import com.projectred.scrollguard.model.TargetApp
import com.projectred.scrollguard.model.UiNodeSnapshot

/**
 * Detektor untuk TikTok (com.zhiliaoapp.musically, com.ss.android.ugc.trill, com.zhiliaoapp.musically.go).
 * Memblokir feed video vertikal tak terbatas (For You / Mengikuti),
 * namun secara ketat melindungi Kotak Masuk (Inbox), obrolan pribadi (DM), profil pengguna, dan pencarian.
 */
class TikTokDetector : AppDetector {

    override val targetApp: TargetApp = TargetApp.TIKTOK

    override fun evaluate(root: UiNodeSnapshot, windowTitle: CharSequence?): DetectionResult {
        // 1. FILTER PRIORITAS TERTINGGI: Kotak Masuk / Obrolan Langsung (Inbox / Direct Message)
        if (isInboxOrChatScreen(root)) {
            return DetectionResult.allowed(
                reason = "Layar Kotak Masuk/Obrolan TikTok terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        // 2. DETEKSI UTAMA: Kontainer Penampil Video Vertikal (TikTok Video Feed / Viewer)
        // PENTING: Dievaluasi sebelum Profil, Pencarian, atau Teman agar jika pengguna membuka video
        // dari halaman pencarian, teman, atau profil, pemutar video vertikal tetap terblokir dengan NAVIGATE_BACK.
        val feedRule = checkDedicatedVideoFeed(root)
        if (feedRule != null) {
            return DetectionResult.blocked(
                ruleName = feedRule.first,
                reason = feedRule.second,
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 3. Periksa apakah tab Beranda / Home aktif dengan feed video vertikal utama
        if (isHomeFeedActive(root)) {
            return DetectionResult.blocked(
                ruleName = "TIKTOK_HOME_FEED_ACTIVE",
                reason = "Feed video vertikal utama (Beranda / Untuk Anda / Mengikuti) sedang aktif",
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.SWITCH_TO_INBOX_TAB
            )
        }

        // 4. FILTER PRIORITAS: Profil Pengguna (Halaman Akun)
        if (isProfileScreen(root)) {
            return DetectionResult.allowed(
                reason = "Layar Profil Pengguna TikTok terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        // 5. FILTER PRIORITAS: Halaman Pencarian / Jelajah / Kamera Rekam
        if (isSearchOrCameraScreen(root)) {
            return DetectionResult.allowed(
                reason = "Layar Pencarian atau Perekaman TikTok terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        // 6. FILTER PRIORITAS: Tab Teman (Friends)
        if (isFriendsScreen(root)) {
            return DetectionResult.allowed(
                reason = "Layar Teman TikTok terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        return DetectionResult.allowed(
            reason = "Bukan tampilan feed video pendek TikTok yang dilarang",
            targetApp = targetApp
        )
    }

    /**
     * Memeriksa apakah layar saat ini merupakan Kotak Masuk (Inbox) atau pesan obrolan.
     */
    private fun isInboxOrChatScreen(root: UiNodeSnapshot): Boolean {
        // Tab Inbox di bilah navigasi bawah terpilih
        val isInboxTabSelected = root.any { node ->
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            val isInbox = desc.contains("kotak masuk") || desc.contains("inbox") || desc.contains("pesan")
            isInbox && node.isSelected
        }
        if (isInboxTabSelected) return true

        // Obrolan aktif (IM chat room, list pesan, komposer)
        val hasChatMarkers = root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("im_chat_room") ||
                id.contains("chat_room") ||
                id.contains("message_edit_text") ||
                id.contains("im_edit_text") ||
                id.contains("conversation_list") ||
                id.contains("btn_send_message")
        }
        if (hasChatMarkers) return true

        // Input pesan teks aktif
        val hasMessagePlaceholder = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.lowercase() ?: ""
            t.startsWith("kirim pesan") || t.startsWith("send message") || t.startsWith("tulis pesan")
        }

        return hasMessagePlaceholder || (root.hasEditableField() && root.hasIdContaining("im"))
    }

    /**
     * Memeriksa apakah layar saat ini merupakan halaman Profil pengguna.
     */
    private fun isProfileScreen(root: UiNodeSnapshot): Boolean {
        // Tab Profil terpilih di bawah
        val isProfileTabSelected = root.any { node ->
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            val isProfile = desc.contains("profil") || desc.contains("profile") || desc.contains("saya")
            isProfile && node.isSelected
        }
        if (isProfileTabSelected) return true

        // Elemen khas profil (edit profil, jumlah pengikut, mengikuti)
        return root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("edit_profile") ||
                id.contains("profile_header") ||
                id.contains("follower_count") ||
                id.contains("following_count") ||
                id.contains("bio_text")
        }
    }

    /**
     * Memeriksa apakah layar saat ini merupakan halaman Pencarian atau Kamera rekam.
     */
    private fun isSearchOrCameraScreen(root: UiNodeSnapshot): Boolean {
        if (root.hasIdContaining("camera_container") || root.hasIdContaining("record_layout")) {
            return true
        }

        val hasSearchInput = root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("search_input") || id.contains("search_edit_text") || id.contains("et_search")
        }

        return hasSearchInput
    }

    /**
     * Memeriksa apakah layar saat ini merupakan tab Teman.
     */
    private fun isFriendsScreen(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            (desc.contains("teman") || desc.contains("friends")) && node.isSelected
        }
    }

    /**
     * Memeriksa apakah feed video Beranda (Home / For You / Mengikuti) sedang aktif.
     */
    private fun isHomeFeedActive(root: UiNodeSnapshot): Boolean {
        // Tab Beranda terpilih di bawah
        val isHomeTabSelected = root.any { node ->
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            val isHome = desc.contains("beranda") || desc.contains("home") || (node.viewIdResourceName?.contains("tab_home") == true)
            isHome && node.isSelected
        }

        // Indikator tombol samping feed video (Like, Komentar, Bagikan, Piringan Musik)
        val hasFeedSideControls = hasVerticalFeedSideControls(root)

        return (isHomeTabSelected && hasFeedSideControls) || hasForYouHeaderTab(root)
    }

    /**
     * Memeriksa tab atas "Untuk Anda" (For You) atau "Mengikuti" (Following).
     */
    private fun hasForYouHeaderTab(root: UiNodeSnapshot): Boolean {
        var hasForYouOrFollowing = false
        for (node in root.flatten()) {
            val t = (node.text ?: node.contentDescription)?.toString()?.trim() ?: continue
            if (t.equals("Untuk Anda", ignoreCase = true) ||
                t.equals("For You", ignoreCase = true) ||
                t.equals("Mengikuti", ignoreCase = true) ||
                t.equals("Following", ignoreCase = true)
            ) {
                hasForYouOrFollowing = true
                break
            }
        }

        return hasForYouOrFollowing && hasVerticalFeedSideControls(root)
    }

    /**
     * Memeriksa keberadaan kontrol samping video vertikal khas TikTok.
     */
    private fun hasVerticalFeedSideControls(root: UiNodeSnapshot): Boolean {
        var score = 0

        // Piringan musik berputar / suara asli
        if (root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            id.contains("music_cover") || id.contains("record_icon") || desc.contains("suara asli") || desc.contains("original sound")
        }) {
            score += 2
        }

        // Tombol suka / favorit vertikal
        if (root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("like_layout") || id.contains("iv_like") || id.contains("favorite_button")
        }) {
            score += 1
        }

        // Tombol komentar vertikal
        if (root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("comment_layout") || id.contains("iv_comment")
        }) {
            score += 1
        }

        return score >= 2
    }

    /**
     * Memeriksa ID kontainer feed video vertikal.
     */
    private fun checkDedicatedVideoFeed(root: UiNodeSnapshot): Pair<String, String>? {
        val hasSideControls = hasVerticalFeedSideControls(root)
        return when {
            root.hasIdContaining("vertical_view_pager") && hasSideControls ->
                "TIKTOK_VERTICAL_VIEW_PAGER" to "ViewPager vertikal feed video TikTok aktif"
            root.hasIdContaining("feed_fragment") && hasSideControls ->
                "TIKTOK_FEED_FRAGMENT" to "Fragment feed video TikTok aktif"
            hasSideControls && (root.hasIdContaining("video_player") || root.hasIdContaining("player_view")) ->
                "TIKTOK_VIDEO_PLAYER" to "Pemutar video vertikal TikTok aktif"
            else -> null
        }
    }
}
