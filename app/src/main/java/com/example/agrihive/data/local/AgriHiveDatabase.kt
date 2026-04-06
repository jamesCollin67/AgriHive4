package com.example.agrihive.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ApiaryEntity::class, ReportEntity::class], version = 2, exportSchema = false)
abstract class AgriHiveDatabase : RoomDatabase() {
    abstract fun apiaryDao(): ApiaryDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AgriHiveDatabase? = null

        fun getDatabase(context: Context): AgriHiveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AgriHiveDatabase::class.java,
                    "agrihive_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
