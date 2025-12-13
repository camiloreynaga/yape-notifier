package com.yapenotifier.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yapenotifier.android.data.local.entity.CapturedNotification
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing captured notifications in debug mode.
 */
@Dao
interface CapturedNotificationDao {
    @Query("SELECT * FROM captured_notifications ORDER BY capturedAt DESC LIMIT 50")
    fun getLatestNotifications(): Flow<List<CapturedNotification>>

    @Query("SELECT * FROM captured_notifications ORDER BY capturedAt DESC LIMIT :limit")
    suspend fun getLatestNotificationsSync(limit: Int = 50): List<CapturedNotification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: CapturedNotification): Long

    @Query("DELETE FROM captured_notifications WHERE id NOT IN (SELECT id FROM captured_notifications ORDER BY capturedAt DESC LIMIT 50)")
    suspend fun keepOnlyLatest50()

    @Query("DELETE FROM captured_notifications")
    suspend fun deleteAll()
}

