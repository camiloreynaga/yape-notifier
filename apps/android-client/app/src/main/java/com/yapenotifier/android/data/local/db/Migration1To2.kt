package com.yapenotifier.android.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration1To2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns for dual app support
        db.execSQL("ALTER TABLE captured_notifications ADD COLUMN androidUserId INTEGER")
        db.execSQL("ALTER TABLE captured_notifications ADD COLUMN androidUid INTEGER")
        db.execSQL("ALTER TABLE captured_notifications ADD COLUMN postedAt INTEGER")
    }
}
