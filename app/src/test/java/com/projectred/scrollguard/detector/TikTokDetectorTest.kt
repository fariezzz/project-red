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

class TikTokDetectorTest {

    private lateinit var detector: TikTokDetector

    @Before
    fun setUp() {
        detector = TikTokDetector()
    }

    @Test
    fun `test target app is TikTok`() {
        assertEquals(TargetApp.TIKTOK, detector.targetApp)
    }

    @Test
    fun `test TikTok Home feed with For You header and side controls is blocked with switch to inbox`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                UiNodeSnapshot(
                    text = "Untuk Anda",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    contentDescription = "suara asli - Kreator Musik",
                    viewIdResourceName = "com.zhiliaoapp.musically:id/music_cover"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.zhiliaoapp.musically:id/like_layout"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.zhiliaoapp.musically:id/comment_layout"
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Feed Untuk Anda TikTok harus diblokir", result.isBlockedSurface)
        assertEquals(Confidence.HIGH, result.confidence)
        assertEquals(RecommendedAction.SWITCH_TO_INBOX_TAB, result.recommendedAction)
    }

    @Test
    fun `test TikTok vertical view pager container is blocked`() {
        val root = UiNodeSnapshot(
            className = "androidx.viewpager.widget.ViewPager",
            viewIdResourceName = "com.zhiliaoapp.musically:id/vertical_view_pager",
            children = listOf(
                UiNodeSnapshot(
                    viewIdResourceName = "com.zhiliaoapp.musically:id/music_cover"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.zhiliaoapp.musically:id/like_layout"
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("ViewPager feed vertikal TikTok harus diblokir", result.isBlockedSurface)
        assertEquals("TIKTOK_VERTICAL_VIEW_PAGER", result.ruleName)
    }

    @Test
    fun `test TikTok Inbox or Direct Message chat is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.RelativeLayout",
            viewIdResourceName = "com.zhiliaoapp.musically:id/im_chat_room",
            children = listOf(
                UiNodeSnapshot(
                    text = "Budi Santoso",
                    viewIdResourceName = "com.zhiliaoapp.musically:id/chat_title"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.zhiliaoapp.musically:id/conversation_list"
                ),
                UiNodeSnapshot(
                    text = "Kirim pesan...",
                    viewIdResourceName = "com.zhiliaoapp.musically:id/message_edit_text",
                    isEditable = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Obrolan DM TikTok tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test TikTok Inbox bottom navigation tab selected is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.LinearLayout",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = false
                ),
                UiNodeSnapshot(
                    contentDescription = "Kotak Masuk",
                    isSelected = true // Tab Inbox aktif
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Halaman Kotak Masuk TikTok tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test TikTok Friends tab is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.LinearLayout",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Teman",
                    isSelected = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Tab Teman TikTok tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test TikTok User Profile page is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.zhiliaoapp.musically:id/profile_header",
            children = listOf(
                UiNodeSnapshot(
                    text = "Edit Profil",
                    viewIdResourceName = "com.zhiliaoapp.musically:id/edit_profile"
                ),
                UiNodeSnapshot(
                    text = "1.2k Mengikuti",
                    viewIdResourceName = "com.zhiliaoapp.musically:id/following_count"
                ),
                UiNodeSnapshot(
                    contentDescription = "Profil",
                    isSelected = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Halaman profil pengguna TikTok tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test TikTok search page is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                UiNodeSnapshot(
                    text = "Cari di TikTok",
                    viewIdResourceName = "com.zhiliaoapp.musically:id/search_input",
                    isEditable = true
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Halaman pencarian TikTok tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test TikTok entering video playback from search or profile is blocked with NAVIGATE_BACK`() {
        // Mensimulasikan saat pengguna membuka video dari hasil pencarian:
        // Input pencarian masih ada di latar belakang, namun video aktif diputar dengan kontrol samping.
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                UiNodeSnapshot(
                    viewIdResourceName = "com.zhiliaoapp.musically:id/search_input"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.zhiliaoapp.musically:id/vertical_view_pager",
                    children = listOf(
                        UiNodeSnapshot(
                            viewIdResourceName = "com.zhiliaoapp.musically:id/music_cover"
                        ),
                        UiNodeSnapshot(
                            viewIdResourceName = "com.zhiliaoapp.musically:id/like_layout"
                        )
                    )
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Memutar video dari pencarian atau profil TikTok harus diblokir", result.isBlockedSurface)
        assertEquals("TIKTOK_VERTICAL_VIEW_PAGER", result.ruleName)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }
}
