package com.yapenotifier.android.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Persistent file logger for debugging service disconnections.
 * Logs are written to app's internal storage and can be viewed in the app.
 * Critical logs are queued for sending to the server.
 *
 * Log file location: /data/data/com.yapenotifier.android/files/service_log.txt
 */
object FileLogger {
    private const val TAG = "FileLogger"
    private const val LOG_FILE_NAME = "service_log.txt"
    private const val PENDING_LOGS_FILE = "pending_server_logs.txt"
    private const val MAX_LOG_SIZE_BYTES = 500_000 // 500KB max
    private const val MAX_PENDING_LOGS = 100

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Volatile
    private var context: Context? = null

    // In-memory queue for logs pending server upload
    private val pendingServerLogs = CopyOnWriteArrayList<LogEntry>()
    private val mutex = Mutex()

    /**
     * Data class representing a log entry for server upload
     */
    data class LogEntry(
        val category: String,
        val level: String,
        val message: String,
        val details: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Categories that should be sent to server
     */
    private val CRITICAL_CATEGORIES = setOf("SERVICE", "DISCONNECT", "RECONNECT", "ERROR")

    /**
     * Initialize the logger with application context.
     * Call this in Application.onCreate()
     */
    fun init(appContext: Context) {
        context = appContext.applicationContext
        // Load any pending logs from disk
        loadPendingLogs()
        log("SYSTEM", "FileLogger initialized")
    }

    /**
     * Log a message with timestamp and category.
     * Categories: SERVICE, HEALTH, DISCONNECT, RECONNECT, ERROR, SYSTEM
     */
    fun log(category: String, message: String, level: String = "info", details: String? = null) {
        val ctx = context ?: return

        try {
            val timestamp = dateFormat.format(Date())
            val logLine = "[$timestamp] [$category] $message\n"

            val file = File(ctx.filesDir, LOG_FILE_NAME)

            // Rotate log if too large
            if (file.exists() && file.length() > MAX_LOG_SIZE_BYTES) {
                rotateLog(file)
            }

            file.appendText(logLine)

            // Also log to Logcat for ADB debugging
            Log.d(TAG, "[$category] $message")

            // Queue critical logs for server upload
            if (category in CRITICAL_CATEGORIES) {
                queueForServer(LogEntry(
                    category = category,
                    level = level,
                    message = message,
                    details = details
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    /**
     * Queue a log entry for server upload
     */
    private fun queueForServer(entry: LogEntry) {
        // Limit queue size
        if (pendingServerLogs.size >= MAX_PENDING_LOGS) {
            pendingServerLogs.removeAt(0) // Remove oldest
        }
        pendingServerLogs.add(entry)

        // Persist to disk in case app is killed
        savePendingLogs()
    }

    /**
     * Get pending logs for server upload and clear them
     */
    suspend fun getPendingLogsForUpload(): List<LogEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val logs = pendingServerLogs.toList()
            pendingServerLogs.clear()
            savePendingLogs()
            logs
        }
    }

    /**
     * Mark logs as uploaded (called after successful server upload)
     */
    fun markLogsUploaded() {
        pendingServerLogs.clear()
        savePendingLogs()
    }

    /**
     * Get count of pending logs
     */
    fun getPendingLogsCount(): Int = pendingServerLogs.size

    /**
     * Save pending logs to disk
     */
    private fun savePendingLogs() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, PENDING_LOGS_FILE)
            val lines = pendingServerLogs.map { entry ->
                "${entry.timestamp}|${entry.category}|${entry.level}|${entry.message}|${entry.details ?: ""}"
            }
            file.writeText(lines.joinToString("\n"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save pending logs", e)
        }
    }

    /**
     * Load pending logs from disk
     */
    private fun loadPendingLogs() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, PENDING_LOGS_FILE)
            if (!file.exists()) return

            val lines = file.readLines()
            for (line in lines) {
                val parts = line.split("|", limit = 5)
                if (parts.size >= 4) {
                    pendingServerLogs.add(LogEntry(
                        timestamp = parts[0].toLongOrNull() ?: System.currentTimeMillis(),
                        category = parts[1],
                        level = parts[2],
                        message = parts[3],
                        details = parts.getOrNull(4)?.takeIf { it.isNotEmpty() }
                    ))
                }
            }
            Log.d(TAG, "Loaded ${pendingServerLogs.size} pending logs from disk")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load pending logs", e)
        }
    }

    /**
     * Log service lifecycle events
     */
    fun logServiceEvent(event: String, details: String? = null) {
        val msg = if (details != null) "$event - $details" else event
        log("SERVICE", msg, "info", details)
    }

    /**
     * Log disconnection events with reason (CRITICAL - always sent to server)
     */
    fun logDisconnect(reason: String, attempt: Int? = null) {
        val msg = if (attempt != null) "$reason (attempt $attempt)" else reason
        log("DISCONNECT", msg, "warning")
    }

    /**
     * Log reconnection attempts
     */
    fun logReconnect(success: Boolean, attempt: Int, delayMs: Long? = null) {
        val status = if (success) "SUCCESS" else "ATTEMPTING"
        val delay = if (delayMs != null) " after ${delayMs}ms" else ""
        val level = if (success) "info" else "warning"
        log("RECONNECT", "$status (attempt $attempt)$delay", level)
    }

    /**
     * Log health worker events (not sent to server - too frequent)
     */
    fun logHealth(serviceConnected: Boolean, pendingCount: Int?, batteryLevel: Int?) {
        val ctx = context ?: return
        try {
            val timestamp = dateFormat.format(Date())
            val message = "serviceConnected=$serviceConnected, pending=$pendingCount, battery=$batteryLevel%"
            val logLine = "[$timestamp] [HEALTH] $message\n"

            val file = File(ctx.filesDir, LOG_FILE_NAME)
            file.appendText(logLine)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write health log", e)
        }
    }

    /**
     * Log errors with stack trace (CRITICAL - always sent to server)
     */
    fun logError(source: String, error: Throwable) {
        val stackTrace = error.stackTraceToString().take(500) // Limit stack trace size
        log("ERROR", "[$source] ${error.message}", "error", stackTrace)
    }

    /**
     * Read all logs as a string (for displaying in UI)
     */
    suspend fun readLogs(): String = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext "Logger not initialized"

        try {
            val file = File(ctx.filesDir, LOG_FILE_NAME)
            if (!file.exists()) {
                return@withContext "No logs yet"
            }

            // Read last N lines
            val lines = file.readLines()
            val lastLines = lines.takeLast(200) // Last 200 lines
            lastLines.reversed().joinToString("\n") // Most recent first
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }

    /**
     * Get log file for sharing
     */
    fun getLogFile(): File? {
        val ctx = context ?: return null
        val file = File(ctx.filesDir, LOG_FILE_NAME)
        return if (file.exists()) file else null
    }

    /**
     * Clear all logs
     */
    fun clearLogs() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
            pendingServerLogs.clear()
            savePendingLogs()
            log("SYSTEM", "Logs cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }

    private fun rotateLog(file: File) {
        try {
            // Keep last 500 lines when rotating
            val lines = file.readLines()
            val keptLines = lines.takeLast(500)
            file.writeText(keptLines.joinToString("\n") + "\n")
            log("SYSTEM", "Log rotated - kept last 500 entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log", e)
        }
    }
}
