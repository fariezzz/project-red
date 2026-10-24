package com.projectred.scrollguard.data

import com.projectred.scrollguard.model.DiagnosticEntry
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

object DiagnosticLogManager {

    private const val MAX_LOG_SIZE = 100
    private val logList = Collections.synchronizedList(ArrayList<DiagnosticEntry>(MAX_LOG_SIZE))
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun record(entry: DiagnosticEntry) {
        synchronized(logList) {
            if (logList.size >= MAX_LOG_SIZE) {
                logList.removeAt(0)
            }
            logList.add(entry)
        }
        notifyListeners()
    }

    fun getEntries(): List<DiagnosticEntry> {
        synchronized(logList) {
            return ArrayList(logList).reversed()
        }
    }

    fun clear() {
        synchronized(logList) {
            logList.clear()
        }
        notifyListeners()
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        for (listener in listeners) {
            try {
                listener()
            } catch (_: Exception) {
            }
        }
    }
}
