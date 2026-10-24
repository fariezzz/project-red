package com.projectred.scrollguard.model

/**
 * Daftar aplikasi target pemblokiran video pendek.
 * Berisi package identifier resmi yang didukung dan nama tampilan ramah pengguna.
 */
enum class TargetApp(
    val id: String,
    val displayName: String,
    val surfaceName: String,
    val packageNames: Set<String>,
    val defaultEnabled: Boolean = true
) {
    INSTAGRAM(
        id = "instagram",
        displayName = "Instagram",
        surfaceName = "Reels",
        packageNames = setOf(
            "com.instagram.android"
        )
    ),
    FACEBOOK(
        id = "facebook",
        displayName = "Facebook",
        surfaceName = "Reels & Video Pendek",
        packageNames = setOf(
            "com.facebook.katana",
            "com.facebook.wakizashi",
            "com.facebook.lite"
        )
    ),
    YOUTUBE(
        id = "youtube",
        displayName = "YouTube",
        surfaceName = "Shorts",
        packageNames = setOf(
            "com.google.android.youtube",
            "com.google.android.youtube.tv"
        )
    ),
    TIKTOK(
        id = "tiktok",
        displayName = "TikTok",
        surfaceName = "Feed Video Vertikal (For You)",
        packageNames = setOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.zhiliaoapp.musically.go"
        )
    );

    companion object {
        fun fromPackageName(packageName: String): TargetApp? {
            return values().firstOrNull { it.packageNames.contains(packageName) }
        }
    }
}
