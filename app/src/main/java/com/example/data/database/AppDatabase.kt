package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BatchDao
import com.example.data.dao.ExportHistoryDao
import com.example.data.dao.ParcelDao
import com.example.data.entities.BatchEntity
import com.example.data.entities.ExportHistoryEntity
import com.example.data.entities.ParcelEntity

@Database(
    entities = [
        BatchEntity::class,
        ParcelEntity::class,
        ExportHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao
    abstract fun parcelDao(): ParcelDao
    abstract fun exportHistoryDao(): ExportHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE parcels ADD COLUMN sequenceNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE parcels ADD COLUMN entryMode TEXT NOT NULL DEFAULT 'PRIMARY'")
                db.execSQL("ALTER TABLE parcels ADD COLUMN pincodeCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE parcels ADD COLUMN addresseeCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE parcels ADD COLUMN measurementsCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE parcels ADD COLUMN isFullyCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_parcels_batchId_sequenceNumber` ON `parcels` (`batchId`, `sequenceNumber`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parcel_entry_pro.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
