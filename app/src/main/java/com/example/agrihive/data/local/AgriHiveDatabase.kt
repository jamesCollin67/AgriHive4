package com.example.agrihive.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ApiaryEntity::class, ReportEntity::class], version = 2, exportSchema = false)
abstract class AgriHiveDatabase : RoomDatabase() {
    abstract fun apiaryDao(): ApiaryDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AgriHiveDatabase? = null

        /**
         * Migration from version 1 → 2.
         * Add any new columns here instead of wiping user data.
         * Example: ALTER TABLE apiaries ADD COLUMN alertsCount INTEGER NOT NULL DEFAULT 0
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safe no-op if schema didn't change structurally between v1 and v2
            }
        }

        fun getDatabase(context: Context): AgriHiveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AgriHiveDatabase::class.java,
                    "agrihive_database"
                )
                .addMigrations(MIGRATION_1_2)
                // Only drop and recreate tables on downgrade (not on upgrade)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
