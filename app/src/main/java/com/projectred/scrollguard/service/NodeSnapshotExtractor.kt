package com.projectred.scrollguard.service

import android.view.accessibility.AccessibilityNodeInfo
import com.projectred.scrollguard.model.UiNodeSnapshot

/**
 * Pengonversi aman dari AccessibilityNodeInfo Android ke UiNodeSnapshot.
 * Menerapkan batasan kedalaman, jumlah node, serta penyaringan node tak terlihat (isVisibleToUser).
 * Hal ini mencegah node latar belakang yang dicache oleh FragmentManager atau ViewPager
 * memicu false positive saat pengguna berada di halaman yang diizinkan.
 */
object NodeSnapshotExtractor {

    private const val MAX_DEPTH = 16
    private const val MAX_TOTAL_NODES = 600

    fun extract(root: AccessibilityNodeInfo?): UiNodeSnapshot? {
        if (root == null) return null
        var nodeCount = 0

        fun extractInternal(node: AccessibilityNodeInfo?, currentDepth: Int): UiNodeSnapshot? {
            if (node == null || currentDepth > MAX_DEPTH || nodeCount >= MAX_TOTAL_NODES) {
                return null
            }

            // PENTING: Abaikan subtree yang tidak terlihat di layar oleh pengguna.
            // Menghindari deteksi fragment lama yang masih tersimpan di memori (ViewPager / FragmentManager cache).
            if (currentDepth > 0 && !node.isVisibleToUser) {
                return null
            }

            nodeCount++

            return try {
                val text = node.text?.toString()
                val contentDescription = node.contentDescription?.toString()
                val viewId = node.viewIdResourceName
                val className = node.className?.toString()
                val isSelected = node.isSelected
                val isClickable = node.isClickable
                val isEditable = node.isEditable
                val isVisible = node.isVisibleToUser

                val childrenList = mutableListOf<UiNodeSnapshot>()
                val childCount = node.childCount
                for (i in 0 until childCount) {
                    if (nodeCount >= MAX_TOTAL_NODES) break
                    val child = try {
                        node.getChild(i)
                    } catch (_: Exception) {
                        null
                    }
                    if (child != null) {
                        val childSnapshot = extractInternal(child, currentDepth + 1)
                        if (childSnapshot != null) {
                            childrenList.add(childSnapshot)
                        }
                    }
                }

                UiNodeSnapshot(
                    text = text,
                    contentDescription = contentDescription,
                    viewIdResourceName = viewId,
                    className = className,
                    isSelected = isSelected,
                    isClickable = isClickable,
                    isEditable = isEditable,
                    isVisibleToUser = isVisible,
                    children = childrenList
                )
            } catch (_: Exception) {
                null
            }
        }

        return extractInternal(root, 0)
    }
}
