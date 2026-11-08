package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.ParcelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParcelDao {
    @Query("SELECT * FROM parcels WHERE batchId = :batchId ORDER BY sequenceNumber ASC, id ASC")
    fun getParcelsByBatch(batchId: Long): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE batchId = :batchId ORDER BY sequenceNumber ASC, id ASC")
    suspend fun getParcelsByBatchSync(batchId: Long): List<ParcelEntity>

    @Query("SELECT COUNT(*) FROM parcels WHERE batchId = :batchId")
    fun getParcelCountByBatch(batchId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM parcels WHERE batchId = :batchId")
    suspend fun getParcelCountByBatchSync(batchId: Long): Int

    @Query("SELECT COUNT(*) FROM parcels WHERE createdAt >= :startOfDayTimestamp")
    fun getTodayParcelCount(startOfDayTimestamp: Long): Flow<Int>

    @Query("SELECT MAX(sequenceNumber) FROM parcels WHERE batchId = :batchId")
    suspend fun getMaxSequenceNumberInBatch(batchId: Long): Int?

    @Query("SELECT COUNT(*) FROM parcels WHERE batchId = :batchId AND pincodeCompleted = 1")
    fun getPincodeCompletedCount(batchId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM parcels WHERE batchId = :batchId AND addresseeCompleted = 1")
    fun getAddresseeCompletedCount(batchId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM parcels WHERE batchId = :batchId AND measurementsCompleted = 1")
    fun getMeasurementsCompletedCount(batchId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM parcels WHERE batchId = :batchId AND isFullyCompleted = 1")
    fun getFullyCompletedCount(batchId: Long): Flow<Int>

    @Query("SELECT * FROM parcels WHERE batchId = :batchId AND sequenceNumber = :sequenceNumber LIMIT 1")
    suspend fun getParcelBySequenceNumber(batchId: Long, sequenceNumber: Int): ParcelEntity?

    @Query("SELECT * FROM parcels WHERE batchId = :batchId AND barcode = :barcode LIMIT 1")
    suspend fun findDuplicateBarcodeInBatch(batchId: Long, barcode: String): ParcelEntity?

    @Query("SELECT * FROM parcels WHERE barcode = :barcode ORDER BY id DESC LIMIT 1")
    suspend fun findDuplicateBarcodeAnyBatch(barcode: String): ParcelEntity?

    @Query("SELECT SUM(physicalWeight) FROM parcels WHERE createdAt >= :startOfDayTimestamp AND physicalWeight > 0.0")
    fun getTodayTotalWeight(startOfDayTimestamp: Long): Flow<Double?>

    @Query("SELECT * FROM parcels WHERE id = :id")
    suspend fun getParcelById(id: Long): ParcelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParcel(parcel: ParcelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParcels(parcels: List<ParcelEntity>)

    @Update
    suspend fun updateParcel(parcel: ParcelEntity)

    @Delete
    suspend fun deleteParcel(parcel: ParcelEntity)

    @Query("DELETE FROM parcels WHERE id = :id")
    suspend fun deleteParcelById(id: Long)

    @Query("SELECT * FROM parcels ORDER BY id DESC")
    suspend fun getAllParcelsSync(): List<ParcelEntity>
}
