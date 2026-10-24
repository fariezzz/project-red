package com.projectred.scrollguard.detector

import com.projectred.scrollguard.model.Confidence
import com.projectred.scrollguard.model.RecommendedAction
import com.projectred.scrollguard.model.TargetApp
import com.projectred.scrollguard.model.UiNodeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FacebookDetectorTest {

    private lateinit var detector: FacebookDetector

    @Before
    fun setUp() {
        detector = FacebookDetector()
    }

    @Test
    fun `test target app is Facebook`() {
        assertEquals(TargetApp.FACEBOOK, detector.targetApp)
    }

    @Test
    fun `test Facebook Reels viewer container is detected and blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.facebook.katana:id/fb_shorts_viewer_container",
            children = listOf(
                UiNodeSnapshot(
                    viewIdResourceName = "com.facebook.katana:id/fb_shorts_container"
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("FB Shorts Viewer harus diblokir", result.isBlockedSurface)
        assertEquals(Confidence.HIGH, result.confidence)
        assertEquals("FB_SHORTS_VIEWER", result.ruleName)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }

    @Test
    fun `test Facebook Reels tab selected is blocked with switch to home`() {
        val root = UiNodeSnapshot(
            className = "android.widget.LinearLayout",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = false
                ),
                UiNodeSnapshot(
                    contentDescription = "Reels",
                    viewIdResourceName = "com.facebook.katana:id/fb_shorts_tab",
                    isSelected = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Tab FB Reels terpilih harus diblokir", result.isBlockedSurface)
        assertEquals("FB_REELS_TAB_SELECTED", result.ruleName)
        assertEquals(RecommendedAction.SWITCH_TO_HOME_TAB, result.recommendedAction)
    }

    @Test
    fun `test Facebook Reels controls combination is detected`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Remix Reel ini",
                    text = "Remix Reel"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.facebook.katana:id/reels_ufi_container"
                ),
                UiNodeSnapshot(
                    text = "audio asli"
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Kombinasi kontrol Reels FB harus diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Facebook Messenger or chat thread is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.facebook.orca:id/thread_view_messages_list_view",
            children = listOf(
                UiNodeSnapshot(
                    text = "Tulis pesan...",
                    viewIdResourceName = "com.facebook.katana:id/composer_edit_text",
                    isEditable = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Obrolan Messenger tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Facebook general news feed is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.facebook.katana:id/newsfeed_fragment",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    text = "Apa yang Anda pikirkan?",
                    isClickable = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Beranda utama Facebook tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Facebook news feed with reels carousel visible is NOT blocked`() {
        // Mensimulasikan tampilan Kabar Beranda Facebook seperti pada tangkapan layar pengguna:
        // Ada tray/carousel Reels di feed, tab Beranda aktif, tetapi pemutar belum dibuka.
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.facebook.katana:id/newsfeed_fragment",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    text = "Reels",
                    viewIdResourceName = "com.facebook.katana:id/reels_tray_header"
                ),
                UiNodeSnapshot(
                    text = "$100000000000000 Nigerian food order"
                ),
                UiNodeSnapshot(
                    text = "Kisuke Salman"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Melihat carousel Reels di News Feed tidak boleh memblokir aplikasi", result.isBlockedSurface)
    }

    @Test
    fun `test Facebook entering reels from news feed carousel is blocked with NAVIGATE_BACK`() {
        // Mensimulasikan saat pengguna mengetuk kartu Reels di carousel Beranda:
        // Komponen newsfeed dan tab Beranda masih ada di latar belakang, tetapi pemutar Reels aktif di depan.
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true,
                    viewIdResourceName = "com.facebook.katana:id/newsfeed_fragment"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.facebook.katana:id/fb_shorts_viewer_container",
                    children = listOf(
                        UiNodeSnapshot(
                            viewIdResourceName = "com.facebook.katana:id/reels_ufi"
                        ),
                        UiNodeSnapshot(
                            text = "audio asli"
                        )
                    )
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Memasuki Reels dari carousel Beranda Facebook harus diblokir", result.isBlockedSurface)
        assertEquals("FB_SHORTS_VIEWER", result.ruleName)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }

    @Test
    fun `test Facebook entering reels via modal layout with null viewIds (Litho) is blocked with NAVIGATE_BACK`() {
        // Mensimulasikan pemutar Facebook Reels berbasis Litho (di mana viewIdResourceName bernilai null):
        // Memiliki tombol Kembali, judul Reels, dan tombol reaksi samping vertikal.
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true // Tab Beranda di latar belakang
                ),
                UiNodeSnapshot(
                    contentDescription = "Kembali",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Reels"
                ),
                UiNodeSnapshot(
                    text = "Ikuti",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    contentDescription = "Suka"
                ),
                UiNodeSnapshot(
                    contentDescription = "Komentar"
                ),
                UiNodeSnapshot(
                    contentDescription = "Bagikan"
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Modal Reels Litho tanpa viewId harus diblokir", result.isBlockedSurface)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }

    @Test
    fun `test Facebook explicit reel creator label blocks with NAVIGATE_BACK`() {
        // Mensimulasikan label aksesibilitas standar Facebook "Reel dari [Nama]" di dalam pemutar (dengan tombol kembali)
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Kembali",
                    text = "<",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    contentDescription = "Reel dari Kisuke Salman",
                    isClickable = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Label Reel dari kreator harus diblokir", result.isBlockedSurface)
        assertEquals("FB_REEL_CREATOR_LABEL", result.ruleName)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }

    @Test
    fun `test Facebook activity window title detects reels viewer`() {
        val root = UiNodeSnapshot(text = "Loading")
        val result = detector.evaluate(root, "com.facebook.fbshorts.viewer.ui.FbShortsViewerActivity")

        assertTrue("Aktivitas FbShortsViewerActivity harus diblokir", result.isBlockedSurface)
        assertEquals("FB_SHORTS_WINDOW_ACTIVITY", result.ruleName)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }

    @Test
    fun `test Facebook wakizashi OEM package is mapped to Facebook`() {
        assertEquals(TargetApp.FACEBOOK, TargetApp.fromPackageName("com.facebook.wakizashi"))
    }

    @Test
    fun `test Facebook first reel without reels heading and with licensed song is blocked with NAVIGATE_BACK`() {
        // Mensimulasikan PERSIS tangkapan layar Reel pertama pengguna (Rules of Survival / Cass TV):
        // Tidak ada kata 'Reels', lagu komersil ('n · Night Changes One'),
        // tetapi ada panah kembali '<', tombol 'Ikuti', tombol bookmark, reaksi Like/Comment/Share,
        // dan bilah floating 'Tambahkan komentar'.
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true // Tab Beranda di background
                ),
                UiNodeSnapshot(
                    contentDescription = "Kembali",
                    text = "<",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Cass TV"
                ),
                UiNodeSnapshot(
                    text = "Ikuti",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "n · Night Changes  One"
                ),
                UiNodeSnapshot(
                    text = "best game is back?"
                ),
                UiNodeSnapshot(
                    contentDescription = "Suka"
                ),
                UiNodeSnapshot(
                    contentDescription = "Komentar"
                ),
                UiNodeSnapshot(
                    contentDescription = "Bagikan"
                ),
                UiNodeSnapshot(
                    contentDescription = "Simpan",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Tambahkan komentar",
                    isClickable = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Reel pertama pengguna harus langsung terdeteksi dan diblokir", result.isBlockedSurface)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }

    @Test
    fun `test Facebook second reel with next prompt is blocked with NAVIGATE_BACK`() {
        // Mensimulasikan tangkapan layar Reel kedua pengguna (Aulus MLBB / SELANJUTNYA prompt):
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    contentDescription = "Kembali",
                    text = "<",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "CC Immune"
                ),
                UiNodeSnapshot(
                    text = "Ikuti",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "SELANJUTNYA: Cass TV · 0.16"
                ),
                UiNodeSnapshot(
                    contentDescription = "Suka"
                ),
                UiNodeSnapshot(
                    contentDescription = "Komentar"
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Reel dengan prompt selanjutnya harus diblokir", result.isBlockedSurface)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }

    @Test
    fun `test Facebook other user profile is NOT blocked`() {
        // Mensimulasikan saat pengguna membuka profil orang lain:
        // Ada tombol Kembali (<), nama pengguna, tombol 'Ikuti', tombol 'Kirim Pesan',
        // tab 'Tentang', tab 'Foto', serta postingan di timeline profil dengan tombol Suka/Komentar.
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Kembali",
                    text = "<",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Budi Santoso"
                ),
                UiNodeSnapshot(
                    text = "Ikuti",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Kirim Pesan",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Tentang"
                ),
                UiNodeSnapshot(
                    text = "Foto"
                ),
                UiNodeSnapshot(
                    text = "Tinggal di Bandung, Jawa Barat"
                ),
                UiNodeSnapshot(
                    contentDescription = "Suka"
                ),
                UiNodeSnapshot(
                    contentDescription = "Komentar"
                ),
                UiNodeSnapshot(
                    contentDescription = "Bagikan"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Halaman profil orang lain tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Facebook user profile with reels tab and add friend is NOT blocked`() {
        // Mensimulasikan profil pengguna yang memiliki tab 'Reels', tombol 'Tambahkan Teman', dll.
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Kembali",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Siti Rahma"
                ),
                UiNodeSnapshot(
                    text = "Tambahkan Teman",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Pesan",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Reels" // Tab Reels di profil orang
                ),
                UiNodeSnapshot(
                    text = "1.5 rb Pengikut"
                ),
                UiNodeSnapshot(
                    contentDescription = "Suka"
                ),
                UiNodeSnapshot(
                    contentDescription = "Komentar"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Profil pengguna dengan tab Reels tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Facebook scrolling past embedded video in group feed is NOT blocked`() {
        // Mensimulasikan saat pengguna scrolling linimasa grup Facebook:
        // Ada tombol kembali (<), header grup, postingan linimasa dengan timestamp '3 j · 🌐',
        // dan video yang hanya numpang lewat di feed (bukan penampil fullscreen).
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Kembali",
                    text = "<",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Komunitas Gamer Indonesia"
                ),
                UiNodeSnapshot(
                    text = "Anggota grup"
                ),
                UiNodeSnapshot(
                    text = "Ahmad Dani"
                ),
                UiNodeSnapshot(
                    text = "3 j · 🌐" // Timestamp postingan feed
                ),
                UiNodeSnapshot(
                    text = "Gameplay keren nih guys coba tonton"
                ),
                UiNodeSnapshot(
                    text = "Tambahkan komentar..." // Inline comment box di postingan feed
                ),
                UiNodeSnapshot(
                    contentDescription = "Suka"
                ),
                UiNodeSnapshot(
                    contentDescription = "Komentar"
                ),
                UiNodeSnapshot(
                    contentDescription = "Bagikan"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Video yang numpang lewat saat scrolling feed grup tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Facebook scrolling past embedded video in page feed is NOT blocked`() {
        // Mensimulasikan saat pengguna scrolling linimasa halaman/fanpage Facebook:
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Kembali",
                    text = "<",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "RevivalTV"
                ),
                UiNodeSnapshot(
                    text = "Kirim pesan ke halaman"
                ),
                UiNodeSnapshot(
                    text = "1 j · 🌐"
                ),
                UiNodeSnapshot(
                    text = "Highlight pertandingan final hari ini"
                ),
                UiNodeSnapshot(
                    contentDescription = "Suka"
                ),
                UiNodeSnapshot(
                    contentDescription = "Komentar"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Video yang numpang lewat saat scrolling feed halaman tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Facebook profile video preview while stalking is NOT blocked`() {
        // Mensimulasikan PERSIS tangkapan layar pengguna saat stalking/melihat profil Aditya:
        // Ada tombol kembali (<), tab profil 'Semua', 'Reels', 'Foto', postingan video dengan tombol play '▶',
        // label 'Disematkan · 29 Jun · ♫ Pirapus • EVERYNITE · 👥', tombol Suka 3, Komentar 1.
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Kembali",
                    text = "<",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Semua",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    text = "Reels",
                    isSelected = false
                ),
                UiNodeSnapshot(
                    text = "Foto",
                    isSelected = false
                ),
                UiNodeSnapshot(
                    text = "Aditya"
                ),
                UiNodeSnapshot(
                    text = "Disematkan · 29 Jun · ♫ Pirapus • EVERYNITE · 👥"
                ),
                UiNodeSnapshot(
                    text = "▶", // Tombol Play video biasa di tengah layar
                    contentDescription = "Putar video",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "EVERYNITE ♫ PIRAPUS"
                ),
                UiNodeSnapshot(
                    contentDescription = "Suka",
                    text = "3"
                ),
                UiNodeSnapshot(
                    contentDescription = "Komentar",
                    text = "1"
                ),
                UiNodeSnapshot(
                    text = "M Syahily Abdullah"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Video preview saat scrolling profil orang lain TIDAK boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Facebook post detail with embedded reel is NOT blocked`() {
        // Mensimulasikan PERSIS tangkapan layar detail postingan pengguna (Fariez - VS Code 2 malam):
        // Header memiliki tombol tutup (✕), nama 'Fariez', post caption 'Jangan buka vscode di jam 2 malam',
        // timestamp '3 j', video preview embed '24 Jam Belajar Ngoding' oleh 'Rafa Alfarino' (contentDescription 'Video reels'),
        // tombol play '▶', reaksi 'Suka 1', 'Komentar 1', 'Bagikan 1', dan input 'Komentari sebagai Fariez'.
        val root = UiNodeSnapshot(
            className = "com.facebook.litho.ComponentHost",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Tutup",
                    text = "✕",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    text = "Fariez"
                ),
                UiNodeSnapshot(
                    text = "3 j"
                ),
                UiNodeSnapshot(
                    text = "Jangan buka vscode di jam 2 malam"
                ),
                UiNodeSnapshot(
                    text = "24 Jam Belajar Ngoding",
                    contentDescription = "Video reels" // Label reel pada kartu embed di dalam postingan
                ),
                UiNodeSnapshot(
                    text = "Rafa Alfarino"
                ),
                UiNodeSnapshot(
                    contentDescription = "Putar video",
                    isClickable = true
                ),
                UiNodeSnapshot(
                    contentDescription = "Suka",
                    text = "1"
                ),
                UiNodeSnapshot(
                    contentDescription = "Komentar",
                    text = "1"
                ),
                UiNodeSnapshot(
                    contentDescription = "Bagikan",
                    text = "1"
                ),
                UiNodeSnapshot(
                    text = "Komentari sebagai Fariez",
                    isClickable = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Halaman detail postingan Facebook TIDAK boleh diblokir", result.isBlockedSurface)
    }
}
