package com.yapenotifier.android.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_notifications")
data class CapturedNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    var status: String = "PENDING" // PENDING, SENT, FAILED
)