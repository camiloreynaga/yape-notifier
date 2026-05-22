package com.yapenotifier.android.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_notifications")
data class CapturedNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val androidUserId: Int? = null, // UserHandle identifier for dual apps
    val androidUid: Int? = null, // Optional UID
    val title: String,
    val body: String,
    val postedAt: Long? = null, // Original notification timestamp
    val timestamp: Long = System.currentTimeMillis(), // When we captured it
    var status: String = "PENDING" // PENDING, SENT, FAILED
)