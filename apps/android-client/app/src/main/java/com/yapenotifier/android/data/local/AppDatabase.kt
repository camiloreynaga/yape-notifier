package com.yapenotifier.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yapenotifier.android.data.local.dao.CapturedNotificationDao
import com.yapenotifier.android.data.local.entity.CapturedNotification

/**
 * Room database for storing debug information.
 * Currently only stores captured notifications for inspection.
 */
@Database(
    entities = [CapturedNotification::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun capturedNotificationDao(): CapturedNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yape_notifier_debug_db"
                )
                    .fallbackToDestructiveMigration() // For debug only
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

