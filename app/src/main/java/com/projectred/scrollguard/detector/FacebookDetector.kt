package com.projectred.scrollguard.detector

import com.projectred.scrollguard.model.Confidence
import com.projectred.scrollguard.model.DetectionResult
import com.projectred.scrollguard.model.RecommendedAction
import com.projectred.scrollguard.model.TargetApp
import com.projectred.scrollguard.model.UiNodeSnapshot

/**
 * Detektor untuk Facebook Reels (com.facebook.katana, com.facebook.wakizashi, com.facebook.lite).
 * Menargetkan pemutar Reels layar penuh dan tab menu Reels,
 * sembari menjaga linimasa postingan, halaman profil (saat stalking/melihat profil orang lain),
 * detail postingan biasa (post permalink), galeri foto/gambar, grup, halaman, dan obrolan Messenger tetap dapat diakses tanpa false-positive.
 */
class FacebookDetector : AppDetector {

    override val targetApp: TargetApp = TargetApp.FACEBOOK

    override fun evaluate(root: UiNodeSnapshot, windowTitle: CharSequence?): DetectionResult {
        // 1. FILTER PRIORITAS TERTINGGI: Messenger / Obrolan Facebook
        if (isMessengerOrChatScreen(root)) {
            return DetectionResult.allowed(
                reason = "Layar obrolan Messenger/Facebook terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        // 2. FILTER UTAMA PENCEGAH FALSE-POSITIVE: Lindungi Detail Postingan (Post Detail / Permalink)
        // Menangani saat pengguna mengeklik atau membuka detail postingan foto, album/kolase gambar, maupun teks biasa.
        // Contoh: Header bertuliskan "Postingan [Nama]", terdapat tombol panah kembali (<), tombol tutup (✕), dll.
        if (isPostDetailScreen(root, windowTitle)) {
            return DetectionResult.allowed(
                reason = "Halaman detail postingan Facebook terdeteksi - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        // 3. FILTER UTAMA PENCEGAH FALSE-POSITIVE: Lindungi Linimasa, Profil Pengguna, Grup, & Postingan Biasa
        // Menangani kasus di mana pengguna sedang melihat/stalking profil orang lain atau membaca feed grup,
        // di mana terdapat video preview biasa (memiliki tombol play ▶, info disematkan, tab profil 'Semua', dll.).
        if (isFeedOrProfileContext(root)) {
            return DetectionResult.allowed(
                reason = "Linimasa postingan atau profil pengguna terdeteksi (preview video biasa) - pemblokiran diabaikan",
                targetApp = targetApp
            )
        }

        // 4. DETEKSI AKTIVITAS / JENDELA KHUSUS REELS VIEWER
        val titleStr = windowTitle?.toString()?.lowercase() ?: ""
        if (titleStr.contains("fbshorts") || titleStr.contains("fb_shorts") ||
            titleStr.contains("reelsviewer") || titleStr.contains("reels_viewer") ||
            titleStr.contains("shortformvideo") || titleStr.contains("short_form_video")
        ) {
            return DetectionResult.blocked(
                ruleName = "FB_SHORTS_WINDOW_ACTIVITY",
                reason = "Aktivitas pemutar Facebook Reels layar penuh terdeteksi ($titleStr)",
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 5. DETEKSI VIEW ID DEDIKASI PEMUTAR REELS LAYAR PENUH
        val viewerRule = checkDedicatedReelsViewer(root)
        if (viewerRule != null) {
            return DetectionResult.blocked(
                ruleName = viewerRule.first,
                reason = viewerRule.second,
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 6. DETEKSI INPUT BAR FLOATING "TAMBAHKAN KOMENTAR" KHAS REELS VIEWER
        // Eksklusif pada pemutar Reels layar penuh (bukan di dalam postingan profil atau feed).
        if (hasAddCommentInput(root)) {
            return DetectionResult.blocked(
                ruleName = "FB_REELS_ADD_COMMENT_BAR",
                reason = "Bilah komentar mengambang khas pemutar Reels terdeteksi",
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 7. DETEKSI PROMPT VIDEO REELS SELANJUTNYA ("SELANJUTNYA: ...")
        if (hasNextReelPrompt(root)) {
            return DetectionResult.blocked(
                ruleName = "FB_REELS_NEXT_PROMPT",
                reason = "Kartu prompt video reels selanjutnya terdeteksi",
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 8. DETEKSI TATA LETAK MODAL REELS (TOMBOL KEMBALI + KONTROL REELS EKSKLUSIF)
        val modalViewerRule = checkModalReelsViewerLayout(root)
        if (modalViewerRule != null) {
            return DetectionResult.blocked(
                ruleName = modalViewerRule.first,
                reason = modalViewerRule.second,
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 9. DETEKSI PENANDA TEKS / CONTENT DESCRIPTION EKSPLISIT REELS (LITHO-SAFE)
        val explicitMarker = checkExplicitReelsMarker(root)
        if (explicitMarker != null) {
            return DetectionResult.blocked(
                ruleName = explicitMarker.first,
                reason = explicitMarker.second,
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 10. DETEKSI KOMBINASI KONTROL KHAS REELS VIEWER (UFI bar, remix, audio asli)
        if (hasFacebookReelsViewerControls(root)) {
            return DetectionResult.blocked(
                ruleName = "FB_REELS_CONTROLS_COMBINATION",
                reason = "Kombinasi kontrol pemutar Facebook Reels layar penuh terdeteksi",
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.NAVIGATE_BACK
            )
        }

        // 11. Periksa apakah tab Reels / Video Pendek pada bilah navigasi sedang dipilih
        if (isFacebookReelsTabSelected(root)) {
            return DetectionResult.blocked(
                ruleName = "FB_REELS_TAB_SELECTED",
                reason = "Tab Reels/Video Pendek terpilih aktif pada Facebook",
                confidence = Confidence.HIGH,
                targetApp = targetApp,
                recommendedAction = RecommendedAction.SWITCH_TO_HOME_TAB
            )
        }

        return DetectionResult.allowed(
            reason = "Bukan tampilan Facebook Reels layar penuh (beranda, grup, atau profil)",
            targetApp = targetApp
        )
    }

    private fun isMessengerOrChatScreen(root: UiNodeSnapshot): Boolean {
        val hasChatId = root.any { node ->
            val id = node.viewIdResourceName?.lowercase() ?: ""
            id.contains("orca") ||
                id.contains("composer_edit_text") ||
                id.contains("thread_view") ||
                id.contains("messages_list") ||
                id.contains("messenger_thread") ||
                id.contains("chat_head")
        }

        if (hasChatId) return true

        val hasChatPlaceholder = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.lowercase() ?: ""
            t.startsWith("tulis pesan") ||
                t.startsWith("kirim pesan") ||
                t.startsWith("write a message") ||
                t.startsWith("message...")
        }

        return hasChatPlaceholder || (root.hasEditableField() && root.hasIdContaining("composer"))
    }

    /**
     * Memeriksa apakah tampilan saat ini merupakan Halaman Detail Postingan (Post Detail / Permalink).
     * Contoh:
     * - Header bertuliskan "Postingan [Nama Halaman/Orang]" (misal: "Postingan Paper Rex", "Postingan Fariez").
     * - Terdapat input bar komentar postingan: "Komentari sebagai...", "Tulis komentar...", dll.
     * - Terdapat tombol Tutup (✕) di header bersama dengan timestamp postingan (misal "3 j", "45 mnt", "29 Jun").
     * - Kolase foto dengan badge jumlah gambar tambahan (misal "+8", "+5", "Foto").
     */
    private fun isPostDetailScreen(root: UiNodeSnapshot, windowTitle: CharSequence? = null): Boolean {
        // 1. Periksa judul jendela atau header bertuliskan "Postingan [Nama]" (misal: "Postingan Paper Rex")
        val titleStr = windowTitle?.toString()?.trim()?.lowercase() ?: ""
        if (titleStr.startsWith("postingan ") || titleStr.endsWith("'s post") || titleStr.contains("post detail")) {
            return true
        }

        val hasPostDetailHeader = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase() ?: ""
            t.startsWith("postingan ") || t.endsWith("'s post")
        }
        if (hasPostDetailHeader) return true

        // 2. Terdapat input bar komentar postingan: "Komentari sebagai [Nama]" atau "Tulis komentar..."
        val hasPostCommentInput = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase() ?: ""
            t.startsWith("komentari sebagai") || t.startsWith("comment as") ||
                t.startsWith("tulis komentar") || t.startsWith("write a comment")
        }
        if (hasPostCommentInput) return true

        // 3. Terdapat tombol Tutup (✕) di header bersama dengan timestamp postingan (misal "3 j", "45 mnt", "29 Jun")
        val hasCloseButton = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""
            t == "✕" || t == "×" || t.equals("tutup", ignoreCase = true) || t.equals("close", ignoreCase = true) ||
                id.contains("close") || id.contains("dismiss")
        }
        val hasPostTimestamp = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim() ?: ""
            t.matches(Regex("^[0-9]+\\s*(j|mnt|dtk|h|m|s|d|hr|min|sec)$", RegexOption.IGNORE_CASE)) ||
                t.matches(Regex("^[0-9]+\\s+(Jan|Feb|Mar|Apr|Mei|Jun|Jul|Agu|Sep|Okt|Nov|Des).*$", RegexOption.IGNORE_CASE)) ||
                t.contains(" · 🌐") || t.contains(" · 👥") || t.contains(" · 🔒") ||
                t.contains(" · publik") || t.contains(" · teman") ||
                t.startsWith("disematkan") || t.startsWith("pinned")
        }
        if (hasCloseButton && hasPostTimestamp) return true

        // 4. Kolase foto dengan badge sisa gambar (misal: "+8", "+3", "+5")
        val hasPhotoAlbumBadge = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim() ?: ""
            t.matches(Regex("^\\+[0-9]+$"))
        }
        if (hasPhotoAlbumBadge) return true

        return false
    }

    /**
     * Memeriksa apakah tampilan saat ini merupakan linimasa atau halaman postingan biasa
     * (misal: Beranda, Linimasa Profil Teman saat stalking, Halaman Orang, atau Grup).
     * Jika ini adalah postingan biasa yang memuat video preview (ada tombol play ▶, info disematkan, tab profil 'Semua', dll.),
     * tampilan ini adalah feed biasa dan TIDAK boleh diblokir.
     */
    private fun isFeedOrProfileContext(root: UiNodeSnapshot): Boolean {
        // Jangan lindungi jika pemutar Reels aktif di depan dengan viewId dedikasi
        if (root.hasIdContaining("fb_shorts_viewer_container") ||
            root.hasIdContaining("reels_viewer_root") ||
            root.hasIdContaining("fb_shorts_pager")
        ) {
            return false
        }

        // 1. Terdapat tombol Play video di tengah (khas preview video pada postingan feed/profil)
        // Pemutar Reels layar penuh tidak pernah memiliki tombol play di tengah karena video langsung berputar otomatis fullscreen.
        val hasCenterPlayButton = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""
            t == "▶" || t == "►" || t == "play" || t == "putar" ||
                t.contains("putar video") || t.contains("play video") ||
                t.contains("tonton video") || t.contains("watch video") ||
                id.contains("play_button") || id.contains("video_play") ||
                id.contains("play_icon") || id.contains("thumbnail_play")
        }
        if (hasCenterPlayButton) return true

        // 2. Tab profil pengguna (misal: tab 'Semua', 'Foto', 'Tentang' di bawah nama pengguna)
        val hasProfileTabs = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase() ?: ""
            t == "semua" || t == "foto" || t == "photos" || t == "tentang" || t == "about"
        }
        if (hasProfileTabs) return true

        // 3. Tombol aksi profil pengguna (Kirim Pesan, Tambahkan Teman, Edit Profil, dll.)
        val hasProfileAction = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase() ?: ""
            t == "kirim pesan" || t == "pesan" || t == "message" ||
                t == "tambahkan teman" || t == "add friend" || t == "edit profil" ||
                t == "batalkan permintaan" || t == "teman" || t == "friends"
        }
        if (hasProfileAction) return true

        // 4. Penanda postingan linimasa (kata 'Disematkan' / 'Pinned', atau metadata tanggal postingan)
        val hasPostMetadata = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase() ?: ""
            t.startsWith("disematkan") || t.startsWith("pinned") ||
                t.contains(" · 🌐") || t.contains(" · 👥") || t.contains(" · 🔒") ||
                t.contains(" · publik") || t.contains(" · teman") ||
                t.matches(Regex("^[0-9]+\\s*(j|mnt|dtk|h|m|s|d|hr|min|sec)$", RegexOption.IGNORE_CASE)) ||
                t.matches(Regex("^[0-9]+\\s+(Jan|Feb|Mar|Apr|Mei|Jun|Jul|Agu|Sep|Okt|Nov|Des).*$", RegexOption.IGNORE_CASE))
        }
        if (hasPostMetadata) return true

        // 5. Header grup atau halaman
        val hasGroupOrPageHeader = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase() ?: ""
            t == "gabung ke grup" || t == "join group" || t == "anggota grup" ||
                t == "kirim pesan ke halaman" || t == "halaman ·" || t == "komunitas"
        }
        if (hasGroupOrPageHeader) return true

        // 6. Tab Beranda aktif di bilah navigasi bawah tanpa adanya penampil Reels aktif
        val isHomeTabSelected = root.any { node ->
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            (desc.contains("beranda") || desc.contains("home")) && node.isSelected
        }
        val hasNewsFeedLayout = root.hasIdContaining("newsfeed") || root.hasIdContaining("feed_stream")
        if (isHomeTabSelected && !hasTopBackButton(root)) return true
        if (hasNewsFeedLayout && !hasTopBackButton(root)) return true

        // 7. Penanda feed rekomendasi (misal: "Orang yang mungkin Anda kenal")
        val hasFeedRecommendation = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase() ?: ""
            t.contains("orang yang mungkin anda kenal") || t.contains("people you may know")
        }
        if (hasFeedRecommendation) return true

        return false
    }

    private fun isFacebookReelsTabSelected(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val isReelsTab = (node.viewIdResourceName?.contains("fb_shorts_tab", ignoreCase = true) == true) ||
                (node.contentDescription?.contains("Reels", ignoreCase = true) == true) ||
                (node.text?.equals("Reels", ignoreCase = true) == true)

            isReelsTab && node.isSelected
        }
    }

    private fun checkDedicatedReelsViewer(root: UiNodeSnapshot): Pair<String, String>? {
        return when {
            root.hasIdContaining("fb_shorts_viewer_container") ->
                "FB_SHORTS_VIEWER" to "Kontainer FB Shorts Viewer terdeteksi"
            root.hasIdContaining("short_form_video_player") ->
                "FB_SHORT_FORM_PLAYER" to "Pemutar video pendek FB terdeteksi"
            root.hasIdContaining("reels_viewer_root") ->
                "FB_REELS_VIEWER_ROOT" to "Root pemutar Facebook Reels terdeteksi"
            root.hasIdContaining("fb_shorts_container") ->
                "FB_SHORTS_CONTAINER" to "Kontainer video Reels Facebook aktif"
            root.hasIdContaining("reels_video_player") ->
                "FB_REELS_VIDEO_PLAYER" to "Pemutar video Reels Facebook aktif"
            root.hasIdContaining("fb_shorts_unified_player") ->
                "FB_SHORTS_UNIFIED_PLAYER" to "Unified player video pendek Facebook aktif"
            root.hasIdContaining("fb_shorts_video_layout") ->
                "FB_SHORTS_VIDEO_LAYOUT" to "Layout video FB Shorts terdeteksi"
            root.hasIdContaining("fb_shorts_native_fragment") ->
                "FB_SHORTS_NATIVE_FRAGMENT" to "Fragment native FB Shorts terdeteksi"
            root.hasIdContaining("fb_shorts_unified_fragment") ->
                "FB_SHORTS_UNIFIED_FRAGMENT" to "Fragment unified FB Shorts terdeteksi"
            root.hasIdContaining("reels_fullscreen_viewer") ->
                "FB_REELS_FULLSCREEN" to "Penampil Reels layar penuh Facebook terdeteksi"
            root.hasIdContaining("fb_shorts_pager") || root.hasIdContaining("fb_shorts_view_pager") ->
                "FB_SHORTS_PAGER" to "Pager gulir video Reels terdeteksi"
            else -> null
        }
    }

    private fun checkExplicitReelsMarker(root: UiNodeSnapshot): Pair<String, String>? {
        var detectedRule: Pair<String, String>? = null

        root.any { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""

            when {
                desc.contains("reel dari ") || desc.contains("reel by ") -> {
                    detectedRule = "FB_REEL_CREATOR_LABEL" to "Penanda video Reel aktif ($desc)"
                    true
                }
                desc.contains("video reels") || desc.contains("reels video") -> {
                    detectedRule = "FB_REELS_VIDEO_LABEL" to "Label video Reels terdeteksi"
                    true
                }
                desc.contains("tonton reels lainnya") || text.contains("tonton reels lainnya") ||
                    desc.contains("watch more reels") || text.contains("watch more reels") -> {
                    detectedRule = "FB_REELS_WATCH_MORE" to "Penanda tonton reels lainnya terdeteksi"
                    true
                }
                desc.contains("reels berikutnya") || text.contains("reels berikutnya") ||
                    desc.contains("next reel") || text.contains("next reel") -> {
                    detectedRule = "FB_REELS_NEXT_MARKER" to "Penanda reels berikutnya terdeteksi"
                    true
                }
                desc.contains("remix reel") || text.contains("remix reel") ||
                    desc.contains("remix this reel") || text.contains("remix this reel") ||
                    desc.contains("remix reel ini") || text.contains("remix reel ini") -> {
                    detectedRule = "FB_REELS_REMIX_ACTION" to "Tindakan remix reel terdeteksi"
                    true
                }
                desc.contains("gunakan audio ini") || text.contains("gunakan audio ini") ||
                    desc.contains("use this audio") || text.contains("use this audio") ||
                    desc.contains("gunakan suara ini") || text.contains("gunakan suara ini") -> {
                    detectedRule = "FB_REELS_AUDIO_PIVOT" to "Tindakan pivot audio Reels terdeteksi"
                    true
                }
                desc.contains("gunakan template") || text.contains("gunakan template") ||
                    desc.contains("use template") || text.contains("use template") -> {
                    detectedRule = "FB_REELS_TEMPLATE_ACTION" to "Tindakan template Reels terdeteksi"
                    true
                }
                else -> false
            }
        }

        return detectedRule
    }

    private fun hasAddCommentInput(root: UiNodeSnapshot): Boolean {
        // Pastikan bukan input bar detail postingan
        val hasPostCommentInput = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase() ?: ""
            t.startsWith("komentari sebagai") || t.startsWith("comment as") ||
                t.startsWith("tulis komentar") || t.startsWith("write a comment")
        }
        if (hasPostCommentInput) return false

        return root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.lowercase()?.trim() ?: ""
            t == "tambahkan komentar" || t == "add a comment" ||
                t.startsWith("tambahkan komentar...") || t.startsWith("add a comment...")
        }
    }

    private fun hasNextReelPrompt(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.lowercase()?.trim() ?: ""
            t.startsWith("selanjutnya:") || t.startsWith("next:") ||
                t.contains("geser ke atas untuk reels") || t.contains("swipe up for reels")
        }
    }

    private fun hasTopBackButton(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase()?.trim() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""
            desc == "kembali" || desc == "back" || desc == "tutup" || desc == "close" ||
                desc.contains("navigasi ke atas") || desc.contains("navigate up") ||
                desc == "<" || desc == "←" ||
                id.contains("back") || id.contains("up_button") || id.contains("close_button")
        }
    }

    private fun hasFollowButton(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim() ?: ""
            t.equals("Ikuti", ignoreCase = true) || t.equals("Follow", ignoreCase = true)
        }
    }

    private fun hasBookmarkOrSaveButton(root: UiNodeSnapshot): Boolean {
        return root.any { node ->
            val desc = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""
            (desc.contains("simpan") || desc.contains("save") || desc.contains("bookmark") ||
                id.contains("save") || id.contains("bookmark")) && node.isClickable
        }
    }

    private fun hasReelsUfiRail(root: UiNodeSnapshot): Boolean {
        var count = 0
        if (root.any { node ->
            val d = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            d.contains("suka") || d.contains("like") || d.contains("tanggapi")
        }) count++

        if (root.any { node ->
            val d = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            d.contains("komentar") || d.contains("comment")
        }) count++

        if (root.any { node ->
            val d = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            d.contains("bagikan") || d.contains("share")
        }) count++

        return count >= 2
    }

    private fun checkModalReelsViewerLayout(root: UiNodeSnapshot): Pair<String, String>? {
        val hasBack = hasTopBackButton(root)
        if (!hasBack) return null

        val hasReelsHeading = root.any { node ->
            val t = (node.text ?: node.contentDescription)?.toString()?.trim() ?: ""
            t.equals("Reels", ignoreCase = true) || t.equals("Reel", ignoreCase = true)
        }

        val hasFollow = hasFollowButton(root)
        val hasUfi = hasReelsUfiRail(root)
        val hasBookmark = hasBookmarkOrSaveButton(root)

        // Trek audio Reels berformat bullet (misal "audio asli" atau "Kreator · Judul")
        // PENTING: Hanya cocok jika mengandung bullet spasi (" · ") dan diakhiri judul audio,
        // BUKAN metadata tanggal postingan seperti "45 mnt · 🌐" atau "29 Jun · 👥".
        val hasAudio = root.any { node ->
            val d = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            (d.contains("audio asli") || d.contains("original audio") || d.contains("suara asli") ||
                d.startsWith("audio:") || d.startsWith("musik:")) ||
                (d.contains(" · ") && !d.contains(" · 🌐") && !d.contains(" · 👥") && !d.contains(" · 🔒") &&
                    !d.matches(Regex("^[0-9]+\\s*(j|mnt|dtk|h|m|s|d|hr|min|sec)\\s*·.*$", RegexOption.IGNORE_CASE)))
        }

        return when {
            // Header 'Reels' berpasangan dengan tombol Ikuti atau UFI (pada layar modal fullscreen tanpa tab profil / play button)
            hasReelsHeading && (hasFollow || hasUfi) ->
                "FB_MODAL_REELS_HEADER" to "Header penampil Reels terdeteksi dengan tombol kembali"
            // Rel aksi vertikal dengan bookmark ribbon khas pemutar video pendek
            hasBookmark && (hasFollow || hasUfi) ->
                "FB_MODAL_REELS_BOOKMARK_UFI" to "Bilah kontrol reaksi vertikal penampil Reels terdeteksi"
            // Trek audio pemutar Reels dengan tombol Ikuti dan UFI
            hasAudio && hasFollow && hasUfi ->
                "FB_MODAL_REELS_AUDIO_UFI" to "Trek audio pemutar Reels terdeteksi dengan tombol Ikuti"
            // Tombol bookmark vertikal dengan trek audio
            hasBookmark && hasAudio ->
                "FB_MODAL_REELS_AUDIO_BOOKMARK" to "Trek audio dan bookmark pemutar Reels terdeteksi"
            else -> null
        }
    }

    private fun hasFacebookReelsViewerControls(root: UiNodeSnapshot): Boolean {
        var score = 0

        if (root.any { node ->
            val d = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            d.contains("remix reel") || d.contains("remix this reel") || d.contains("remix reel ini")
        }) {
            score += 2
        }

        if (root.any { node ->
            val d = (node.contentDescription ?: node.text)?.toString()?.lowercase() ?: ""
            d.contains("audio asli") || d.contains("original audio") || d.contains("suara asli")
        }) {
            score += 2
        }

        if (root.hasIdContaining("reels_ufi") ||
            root.hasIdContaining("fb_shorts_ufi") ||
            root.hasIdContaining("fb_shorts_action_bar")
        ) {
            score += 2
        }

        return score >= 2
    }
}
