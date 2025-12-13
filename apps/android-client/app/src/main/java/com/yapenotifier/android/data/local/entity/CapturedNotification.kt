package com.yapenotifier.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity for storing captured notifications in debug mode.
 * Only stores minimal data for inspection purposes.
 */
@Entity(tableName = "captured_notifications")
data class CapturedNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postTime: Long,
    val extrasKeys: String?, // Comma-separated list of extra keys
    val capturedAt: Long = System.currentTimeMillis()
) {
    fun getFormattedPostTime(): String {
        return Date(postTime).toString()
    }

    fun getFormattedCapturedAt(): String {
        return Date(capturedAt).toString()
    }
}

