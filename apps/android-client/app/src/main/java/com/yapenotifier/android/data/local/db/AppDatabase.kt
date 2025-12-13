package com.yapenotifier.android.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CapturedNotification::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun capturedNotificationDao(): CapturedNotificationDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yape_notifier_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
