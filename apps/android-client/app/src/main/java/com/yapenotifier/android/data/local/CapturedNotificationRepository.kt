package com.yapenotifier.android.data.local

import com.yapenotifier.android.data.local.dao.CapturedNotificationDao
import com.yapenotifier.android.data.local.entity.CapturedNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

/**
 * Repository for managing captured notifications in debug mode.
 */
class CapturedNotificationRepository(private val dao: CapturedNotificationDao) {
    fun getLatestNotifications(): Flow<List<CapturedNotification>> {
        return dao.getLatestNotifications()
    }

    suspend fun getLatestNotificationsSync(limit: Int = 50): List<CapturedNotification> {
        return dao.getLatestNotificationsSync(limit)
    }

    suspend fun insertNotification(notification: CapturedNotification) {
        dao.insert(notification)
        // Keep only the latest 50
        dao.keepOnlyLatest50()
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}

