package com.projectred.scrollguard.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Entri log diagnostik lokal.
 * Tidak menyimpan teks percakapan, screenshot, atau isi layar pengguna.
 */
data class DiagnosticEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val targetApp: TargetApp?,
    val matchedRule: String,
    val actionTaken: String,
    val confidence: Confidence
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
