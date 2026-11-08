package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entities.ExportHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportHistoryDao {
    @Query("SELECT * FROM export_history WHERE batchId = :batchId ORDER BY exportDate DESC")
    fun getHistoryByBatch(batchId: Long): Flow<List<ExportHistoryEntity>>

    @Query("SELECT * FROM export_history ORDER BY exportDate DESC")
    fun getAllHistory(): Flow<List<ExportHistoryEntity>>

    @Query("SELECT * FROM export_history ORDER BY exportDate DESC")
    suspend fun getAllHistorySync(): List<ExportHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExportHistory(history: ExportHistoryEntity): Long
}
