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

class YouTubeDetectorTest {

    private lateinit var detector: YouTubeDetector

    @Before
    fun setUp() {
        detector = YouTubeDetector()
    }

    @Test
    fun `test target app is YouTube`() {
        assertEquals(TargetApp.YOUTUBE, detector.targetApp)
    }

    @Test
    fun `test YouTube Shorts reel recycler is detected and blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.google.android.youtube:id/reel_recycler",
            children = listOf(
                UiNodeSnapshot(
                    viewIdResourceName = "com.google.android.youtube:id/reel_player_fragment_container"
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("YouTube Shorts reel recycler harus diblokir", result.isBlockedSurface)
        assertEquals(Confidence.HIGH, result.confidence)
        assertEquals("YT_REEL_RECYCLER", result.ruleName)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }

    @Test
    fun `test YouTube Shorts bottom navigation tab selected is blocked with switch to home`() {
        val root = UiNodeSnapshot(
            className = "android.widget.LinearLayout",
            viewIdResourceName = "com.google.android.youtube:id/pivot_bar",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = false
                ),
                UiNodeSnapshot(
                    contentDescription = "Shorts",
                    viewIdResourceName = "com.google.android.youtube:id/shorts_tab",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    contentDescription = "Langganan",
                    isSelected = false
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Tab Shorts terpilih pada pivot bar harus diblokir", result.isBlockedSurface)
        assertEquals("YT_SHORTS_TAB_SELECTED", result.ruleName)
        assertEquals(RecommendedAction.SWITCH_TO_HOME_TAB, result.recommendedAction)
    }

    @Test
    fun `test YouTube Shorts controls combination is detected`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                UiNodeSnapshot(
                    text = "Remix",
                    contentDescription = "Remix audio ini"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.google.android.youtube:id/reel_sound_pivot_button"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.google.android.youtube:id/reel_like_button"
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Kombinasi kontrol Shorts harus diblokir", result.isBlockedSurface)
        assertEquals("YT_SHORTS_CONTROLS_COMBINATION", result.ruleName)
    }

    @Test
    fun `test YouTube standard video player is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.google.android.youtube:id/watch_while_layout",
            children = listOf(
                UiNodeSnapshot(
                    viewIdResourceName = "com.google.android.youtube:id/inline_time_bar"
                ),
                UiNodeSnapshot(
                    viewIdResourceName = "com.google.android.youtube:id/watch_metadata",
                    children = listOf(
                        UiNodeSnapshot(
                            text = "Tutorial Pemrograman Kotlin Lengkap"
                        ),
                        UiNodeSnapshot(
                            viewIdResourceName = "com.google.android.youtube:id/related_videos"
                        )
                    )
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Pemutar video biasa YouTube tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test YouTube home and search feed is NOT blocked`() {
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.google.android.youtube:id/pane_fragment_container",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    contentDescription = "Shorts",
                    isSelected = false
                ),
                UiNodeSnapshot(
                    text = "Telusuri YouTube"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Beranda dan pencarian YouTube tidak boleh diblokir", result.isBlockedSurface)
    }

    @Test
    fun `test YouTube home feed with horizontal shorts shelf visible is NOT blocked`() {
        // Mensimulasikan tampilan Beranda YouTube seperti pada tangkapan layar pengguna:
        // Ada rak Shorts horizontal di feed, tab Beranda aktif, tetapi pemutar belum dibuka.
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true
                ),
                UiNodeSnapshot(
                    text = "Shorts",
                    viewIdResourceName = "com.google.android.youtube:id/shelf_header"
                ),
                UiNodeSnapshot(
                    text = "that spray",
                    viewIdResourceName = "com.google.android.youtube:id/shorts_shelf_card"
                ),
                UiNodeSnapshot(
                    text = "The Only Aim Guide You Need to Rank Up in Valorant",
                    viewIdResourceName = "com.google.android.youtube:id/video_title"
                )
            )
        )

        val result = detector.evaluate(root)
        assertFalse("Melihat rak Shorts di feed Beranda tidak boleh memblokir aplikasi", result.isBlockedSurface)
    }

    @Test
    fun `test YouTube entering shorts from home feed shelf is blocked with NAVIGATE_BACK`() {
        // Mensimulasikan saat pengguna mengetuk kartu Shorts di rak Beranda:
        // Tab Beranda masih aktif/tercatat di latar belakang, namun pemutar Shorts aktif di layar depan.
        val root = UiNodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                // Tab Beranda masih berstatus isSelected = true di bilah navigasi latar belakang
                UiNodeSnapshot(
                    contentDescription = "Beranda",
                    isSelected = true
                ),
                // Pemutar Shorts aktif di depan
                UiNodeSnapshot(
                    viewIdResourceName = "com.google.android.youtube:id/reel_recycler",
                    children = listOf(
                        UiNodeSnapshot(
                            viewIdResourceName = "com.google.android.youtube:id/reel_sound_pivot_button"
                        ),
                        UiNodeSnapshot(
                            viewIdResourceName = "com.google.android.youtube:id/reel_like_button"
                        )
                    )
                )
            )
        )

        val result = detector.evaluate(root)
        assertTrue("Memasuki Shorts dari rak Beranda harus diblokir", result.isBlockedSurface)
        assertEquals("YT_REEL_RECYCLER", result.ruleName)
        assertEquals(RecommendedAction.NAVIGATE_BACK, result.recommendedAction)
    }
}
