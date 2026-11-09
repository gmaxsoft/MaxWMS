package pl.maxwms.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE parts ADD COLUMN pendingChange TEXT NOT NULL DEFAULT 'NONE'",
        )
    }
}
