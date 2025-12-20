package com.yapenotifier.android.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CapturedNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: CapturedNotification): Long

    @Query("SELECT * FROM captured_notifications WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingNotifications(): List<CapturedNotification>

    @Query("UPDATE captured_notifications SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: String)

    @Query("DELETE FROM captured_notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM captured_notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): kotlinx.coroutines.flow.Flow<List<CapturedNotification>>

    @Query("UPDATE captured_notifications SET status = 'PENDING' WHERE status = 'FAILED'")
    suspend fun resetFailedNotifications()

    // Statistics queries
    @Query("SELECT * FROM captured_notifications WHERE status = 'SENT' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSentNotification(): CapturedNotification?

    @Query("SELECT COUNT(*) FROM captured_notifications WHERE status = 'SENT' AND timestamp >= (strftime('%s', 'now', 'start of day') * 1000)")
    suspend fun getSentTodayCount(): Int

    @Query("SELECT COUNT(*) FROM captured_notifications WHERE status = 'PENDING'")
    fun getPendingCountFlow(): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT COUNT(*) FROM captured_notifications WHERE status = 'FAILED'")
    fun getFailedCountFlow(): kotlinx.coroutines.flow.Flow<Int>
}
