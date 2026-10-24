package com.projectred.scrollguard.detector

import com.projectred.scrollguard.model.DetectionResult
import com.projectred.scrollguard.model.TargetApp
import com.projectred.scrollguard.model.UiNodeSnapshot

/**
 * Antarmuka untuk detektor spesifik tiap aplikasi media sosial.
 */
interface AppDetector {
    val targetApp: TargetApp

    /**
     * Memeriksa apakah snapshot antarmuka saat ini merupakan permukaan video pendek.
     * Mengembalikan DetectionResult dengan indikasi diblokir, level keyakinan, dan alasan.
     */
    fun evaluate(root: UiNodeSnapshot, windowTitle: CharSequence? = null): DetectionResult
}
