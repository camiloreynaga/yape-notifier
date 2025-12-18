package com.yapenotifier.android.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration1To2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns for dual app support
        database.execSQL("ALTER TABLE captured_notifications ADD COLUMN androidUserId INTEGER")
        database.execSQL("ALTER TABLE captured_notifications ADD COLUMN androidUid INTEGER")
        database.execSQL("ALTER TABLE captured_notifications ADD COLUMN postedAt INTEGER")
    }
}


