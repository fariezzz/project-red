package com.projectred.scrollguard.model

/**
 * Representasi snapshot UI node yang independen dari Android SDK AccessibilityNodeInfo.
 * Memungkinkan unit testing murni tanpa emulator atau mocking framework berat.
 */
data class UiNodeSnapshot(
    val text: String? = null,
    val contentDescription: String? = null,
    val viewIdResourceName: String? = null,
    val className: String? = null,
    val isSelected: Boolean = false,
    val isClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isVisibleToUser: Boolean = true,
    val children: List<UiNodeSnapshot> = emptyList()
) {

    /**
     * Mengumpulkan seluruh node dalam pohon secara pre-order traversal.
     */
    fun flatten(onlyVisible: Boolean = true): Sequence<UiNodeSnapshot> = sequence {
        if (!onlyVisible || this@UiNodeSnapshot.isVisibleToUser) {
            yield(this@UiNodeSnapshot)
            for (child in children) {
                yieldAll(child.flatten(onlyVisible))
            }
        }
    }

    /**
     * Mencari apakah ada node yang memenuhi predikat tertentu.
     * Secara default hanya memeriksa node yang terlihat oleh pengguna (isVisibleToUser == true).
     */
    fun any(onlyVisible: Boolean = true, predicate: (UiNodeSnapshot) -> Boolean): Boolean {
        if (!onlyVisible || this.isVisibleToUser) {
            if (predicate(this)) return true
        }
        return children.any { it.any(onlyVisible, predicate) }
    }

    /**
     * Memeriksa keberadaan node dengan View ID yang mengandung substring tertentu.
     */
    fun hasIdContaining(subId: String, onlyVisible: Boolean = true): Boolean {
        val lower = subId.lowercase()
        return any(onlyVisible) { it.viewIdResourceName?.lowercase()?.contains(lower) == true }
    }

    /**
     * Memeriksa keberadaan node dengan teks sama persis atau mengandung kata tertentu.
     */
    fun hasText(expected: String, ignoreCase: Boolean = true, onlyVisible: Boolean = true): Boolean {
        return any(onlyVisible) { it.text?.equals(expected, ignoreCase = ignoreCase) == true }
    }

    /**
     * Memeriksa keberadaan node dengan teks mengandung substring tertentu.
     */
    fun hasTextContaining(subText: String, ignoreCase: Boolean = true, onlyVisible: Boolean = true): Boolean {
        return any(onlyVisible) { it.text?.contains(subText, ignoreCase = ignoreCase) == true }
    }

    /**
     * Memeriksa keberadaan node dengan content description sama persis atau mengandung kata tertentu.
     */
    fun hasDescription(expected: String, ignoreCase: Boolean = true, onlyVisible: Boolean = true): Boolean {
        return any(onlyVisible) { it.contentDescription?.equals(expected, ignoreCase = ignoreCase) == true }
    }

    /**
     * Memeriksa keberadaan node dengan content description mengandung substring tertentu.
     */
    fun hasDescriptionContaining(subDesc: String, ignoreCase: Boolean = true, onlyVisible: Boolean = true): Boolean {
        return any(onlyVisible) { it.contentDescription?.contains(subDesc, ignoreCase = ignoreCase) == true }
    }

    /**
     * Memeriksa keberadaan node dengan Class Name tertentu (misalnya EditText atau ViewPager2).
     */
    fun hasClassContaining(subClass: String, onlyVisible: Boolean = true): Boolean {
        return any(onlyVisible) { it.className?.contains(subClass, ignoreCase = true) == true }
    }

    /**
     * Memeriksa apakah ada field input teks aktif atau mode obrolan/komposisi pesan.
     */
    fun hasEditableField(onlyVisible: Boolean = true): Boolean {
        return any(onlyVisible) { it.isEditable || it.className?.contains("EditText", ignoreCase = true) == true }
    }
}
