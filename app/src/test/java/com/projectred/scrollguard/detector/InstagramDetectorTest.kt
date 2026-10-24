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

class InstagramDetectorTest {

    private lateinit var detector: InstagramDetector

    @Before
    fun setUp() {
        detector = InstagramDetector()
    }

    @Test
    fun `test target app is Instagram`() {
        assertEquals(TargetApp.INSTAGRAM, detector.targetApp)
    }

    @Test
    fun `test Instagram fullscreen clips viewer is detected and blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            children = listOf(
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/clips_viewer_like_button"
                ),
                UiNodeSnapshot(
                    text = "audio asli - Artis Keren"
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Fullscreen clips viewer harus diblokir", result.isBlockedSurface)
        assertEquals(Confidence.HIGH, result.confidence)
        assertEquals("IG_CLIPS_FULLSCREEN_VIEWER", result.ruleName)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }

    @Test
    fun `test Instagram Reels bottom navigation tab selected is blocked with switch to home`() {
        val root = UiNodeSnapshot(
            className = "android.widget.LinearLayout",
            viewIdResourceName = "com.instagram.android:id/tab_bar",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = false
                ),
                UiNodeSnapshot(
                    contentDescription = "Reels",
                    viewIdResourceName = "com.instagram.android:id/tab_reels",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    contentDescription = "Profil",
                    isSelected = false
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Tab Reels aktif harus diblokir", result.isBlockedSurface)
        assertEquals("IG_REELS_TAB_SELECTED", result.ruleName)
        assertEquals(RecommendedAction.SWITCH_TO_HOME_TAB, result.recommendedAction)
    }

    @Test
    fun `test Instagram Stories is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.instagram.android:id/reel_viewer_root",
            children = listOf(
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/reel_viewer_progress_bar"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/toolbar_stories"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Cerita Instagram (Stories) tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Instagram Home feed with embedded suggested clips card is NOT blocked`() {
        // Layar Beranda utama di mana tab Beranda aktif dan terdapat kartu reels yang disematkan
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.instagram.android:id/feed_recycler_view",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    contentDescription = "Reels",
                    viewIdResourceName = "com.instagram.android:id/tab_reels",
                    isSelected = false
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/clips_video_container"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Beranda Instagram dengan kartu Reels terselip tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test Instagram DM chat thread is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.RelativeLayout",
            viewIdResourceName = "com.instagram.android:id/direct_thread",
            children = listOf(
                UiNodeSnapshot(
                    text = "Fariez",
                    viewIdResourceName = "com.instagram.android:id/thread_title"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/message_list",
                    children = listOf(
                        UiNodeSnapshot(
                            text = "Halo, apa kabar?",
                            viewIdResourceName = "com.instagram.android:id/direct_text_message_text_view"
                        )
                    )
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/row_thread_composer",
                    children = listOf(
                        UiNodeSnapshot(
                            text = "Kirim pesan...",
                            isEditable = true,
                            className = "android.widget.EditText"
                        )
                    )
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Layar chat DM tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test message containing the word 'reels' in DM chat is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.RelativeLayout",
            viewIdResourceName = "com.instagram.android:id/direct_thread",
            children = listOf(
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/message_list",
                    children = listOf(
                        UiNodeSnapshot(
                            text = "Coba cek reels ini bagus banget kemarin aku lihat",
                            viewIdResourceName = "com.instagram.android:id/direct_text_message_text_view"
                        )
                    )
                ),
                UiNodeSnapshot(
                    text = "Kirim pesan...",
                    isEditable = true,
                    className = "android.widget.EditText"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Pesan yang menyebut kata 'reels' tidak boleh memicu blokir", result.isBlockedSurface)
    }

    @Test
    fun `test invisible cached reels view does NOT trigger false positive`() {
        // Node clips_viewer yang tersimpan di memori tetapi isVisibleToUser == false
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true,
                    isVisibleToUser = true
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
                    isVisibleToUser = false, // Halaman latar belakang tidak terlihat
                    children = listOf(
                        UiNodeSnapshot(
                            viewIdResourceName = "com.instagram.android:id/clips_viewer_like_button",
                            isVisibleToUser = false
                        )
                    )
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Node tersembunyi tidak boleh memicu pemblokiran", result.isBlockedSurface)
    }

    @Test
    fun `test Instagram entering reels from home feed is blocked with NAVIGATE_BACK even when home tab is selected`() {
        // Mensimulasikan saat pengguna mengetuk kartu Reels yang terselip di Beranda:
        // Tab Beranda dan feed_recycler_view masih ada di latar belakang, tetapi clips_viewer aktif di depan.
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true,
                    viewIdResourceName = "com.instagram.android:id/tab_home"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/feed_recycler_view"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
                    children = listOf(
                        UiNodeSnapshot(
                            viewIdResourceName = "com.instagram.android:id/clips_viewer_like_button"
                        ),
                        UiNodeSnapshot(
                            text = "audio asli"
                        )
                    )
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Memasuki Reels dari feed Beranda Instagram harus diblokir", result.isBlockedSurface)
        assertEquals("IG_CLIPS_FULLSCREEN_VIEWER", result.ruleName)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }
}
