package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.BatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches ORDER BY id DESC")
    fun getAllBatches(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches WHERE id = :id")
    suspend fun getBatchById(id: Long): BatchEntity?

    @Query("SELECT * FROM batches WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun getActiveBatch(): Flow<BatchEntity?>

    @Query("SELECT * FROM batches WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    suspend fun getActiveBatchSync(): BatchEntity?

    @Query("SELECT COUNT(*) FROM batches")
    fun getBatchCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM batches WHERE status = 'COMPLETED'")
    fun getCompletedBatchCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM batches WHERE status = 'EXPORTED'")
    fun getExportedBatchCount(): Flow<Int>

    @Query("SELECT * FROM batches WHERE batchDate = :date")
    suspend fun getBatchesByDate(date: String): List<BatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity): Long

    @Update
    suspend fun updateBatch(batch: BatchEntity)

    @Delete
    suspend fun deleteBatch(batch: BatchEntity)

    @Query("UPDATE batches SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBatchStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())
}
