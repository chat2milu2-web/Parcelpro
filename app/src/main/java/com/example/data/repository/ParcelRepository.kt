package com.example.data.repository

import com.example.data.dao.BatchDao
import com.example.data.dao.ExportHistoryDao
import com.example.data.dao.ParcelDao
import com.example.data.entities.BatchEntity
import com.example.data.entities.ExportHistoryEntity
import com.example.data.entities.ParcelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ParcelRepository(
    private val batchDao: BatchDao,
    private val parcelDao: ParcelDao,
    private val exportHistoryDao: ExportHistoryDao
) {
    val allBatches: Flow<List<BatchEntity>> = batchDao.getAllBatches()
    val activeBatch: Flow<BatchEntity?> = batchDao.getActiveBatch()

    val totalBatchesCount: Flow<Int> = batchDao.getBatchCount()
    val completedBatchesCount: Flow<Int> = batchDao.getCompletedBatchCount()
    val exportedBatchesCount: Flow<Int> = batchDao.getExportedBatchCount()

    fun getTodayParcelCount(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return parcelDao.getTodayParcelCount(calendar.timeInMillis)
    }

    fun getParcelsByBatch(batchId: Long): Flow<List<ParcelEntity>> {
        return parcelDao.getParcelsByBatch(batchId)
    }

    suspend fun getParcelsByBatchSync(batchId: Long): List<ParcelEntity> {
        return parcelDao.getParcelsByBatchSync(batchId)
    }

    fun getParcelCountByBatch(batchId: Long): Flow<Int> {
        return parcelDao.getParcelCountByBatch(batchId)
    }

    fun getPincodeCompletedCount(batchId: Long): Flow<Int> = parcelDao.getPincodeCompletedCount(batchId)
    fun getAddresseeCompletedCount(batchId: Long): Flow<Int> = parcelDao.getAddresseeCompletedCount(batchId)
    fun getMeasurementsCompletedCount(batchId: Long): Flow<Int> = parcelDao.getMeasurementsCompletedCount(batchId)
    fun getFullyCompletedCount(batchId: Long): Flow<Int> = parcelDao.getFullyCompletedCount(batchId)

    suspend fun getActiveBatchSync(): BatchEntity? {
        return batchDao.getActiveBatchSync()
    }

    suspend fun getBatchById(id: Long): BatchEntity? {
        return batchDao.getBatchById(id)
    }

    suspend fun generateUniqueBatchCode(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val todayPrefix = "YYB-$dateStr"
        val batchesToday = batchDao.getBatchesByDate(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        val countToday = batchesToday.size + 1
        return String.format(Locale.US, "%s-%03d", todayPrefix, countToday)
    }

    suspend fun createBatch(description: String, customBatchCode: String? = null): Long {
        val batchCode = if (!customBatchCode.isNullOrBlank()) customBatchCode.trim() else generateUniqueBatchCode()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val newBatch = BatchEntity(
            batchCode = batchCode,
            batchDate = dateStr,
            description = if (description.isBlank()) "Parcel Data Entry Batch" else description,
            status = BatchEntity.STATUS_ACTIVE
        )
        return batchDao.insertBatch(newBatch)
    }

    suspend fun setBatchActive(batchId: Long) {
        val active = batchDao.getActiveBatchSync()
        if (active != null && active.id != batchId) {
            batchDao.updateBatchStatus(active.id, BatchEntity.STATUS_COMPLETED)
        }
        batchDao.updateBatchStatus(batchId, BatchEntity.STATUS_ACTIVE)
    }

    suspend fun completeBatch(batchId: Long) {
        batchDao.updateBatchStatus(batchId, BatchEntity.STATUS_COMPLETED)
    }

    suspend fun deleteBatch(batchId: Long) {
        val batch = batchDao.getBatchById(batchId)
        if (batch != null) {
            batchDao.deleteBatch(batch)
        }
    }

    suspend fun findDuplicateBarcodeInBatch(batchId: Long, barcode: String): ParcelEntity? {
        return parcelDao.findDuplicateBarcodeInBatch(batchId, barcode.trim())
    }

    suspend fun findDuplicateBarcodeAnyBatch(barcode: String): ParcelEntity? {
        return parcelDao.findDuplicateBarcodeAnyBatch(barcode.trim())
    }

    fun getTodayTotalWeight(): Flow<Double?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return parcelDao.getTodayTotalWeight(calendar.timeInMillis)
    }

    suspend fun getNextSequenceNumber(batchId: Long): Int {
        val max = parcelDao.getMaxSequenceNumberInBatch(batchId) ?: 0
        return max + 1
    }

    suspend fun insertParcel(parcel: ParcelEntity): Long {
        val seq = if (parcel.sequenceNumber <= 0) getNextSequenceNumber(parcel.batchId) else parcel.sequenceNumber
        val upperAddressee = parcel.addresseeName.trim().uppercase()
        val isFull = parcel.barcode.isNotBlank() &&
                parcel.destinationPincode.isNotBlank() &&
                upperAddressee.isNotBlank() &&
                parcel.physicalWeight > 0.0

        val parcelToInsert = parcel.copy(
            addresseeName = upperAddressee,
            sequenceNumber = seq,
            pincodeCompleted = parcel.destinationPincode.isNotBlank(),
            addresseeCompleted = upperAddressee.isNotBlank(),
            measurementsCompleted = parcel.physicalWeight > 0.0,
            isFullyCompleted = isFull
        )
        return parcelDao.insertParcel(parcelToInsert)
    }

    suspend fun addSecondaryBarcode(batchId: Long, barcode: String): Long {
        val existing = findDuplicateBarcodeInBatch(batchId, barcode)
        if (existing != null) return -1L

        val nextSeq = getNextSequenceNumber(batchId)
        val parcel = ParcelEntity(
            batchId = batchId,
            barcode = barcode.trim(),
            sequenceNumber = nextSeq,
            entryMode = ParcelEntity.MODE_SECONDARY
        )
        return parcelDao.insertParcel(parcel)
    }

    suspend fun updatePincodeForParcel(parcelId: Long, pincode: String, requiredLength: Int = 6) {
        val existing = parcelDao.getParcelById(parcelId) ?: return
        val isDone = pincode.length == requiredLength
        val isFull = isDone && existing.addresseeCompleted && existing.measurementsCompleted
        val updated = existing.copy(
            destinationPincode = pincode,
            pincodeCompleted = isDone,
            isFullyCompleted = isFull,
            updatedAt = System.currentTimeMillis()
        )
        parcelDao.updateParcel(updated)
    }

    suspend fun updateAddresseeForParcel(parcelId: Long, addressee: String) {
        val existing = parcelDao.getParcelById(parcelId) ?: return
        val upper = addressee.trim().uppercase()
        val isDone = upper.isNotBlank()
        val isFull = existing.pincodeCompleted && isDone && existing.measurementsCompleted
        val updated = existing.copy(
            addresseeName = upper,
            addresseeCompleted = isDone,
            isFullyCompleted = isFull,
            updatedAt = System.currentTimeMillis()
        )
        parcelDao.updateParcel(updated)
    }

    suspend fun updateMeasurementsForParcel(
        parcelId: Long,
        weight: Double,
        length: Double,
        breadth: Double,
        height: Double
    ) {
        val existing = parcelDao.getParcelById(parcelId) ?: return
        val isDone = weight > 0.0
        val isFull = existing.pincodeCompleted && existing.addresseeCompleted && isDone
        val updated = existing.copy(
            physicalWeight = weight,
            length = length,
            breadth = breadth,
            height = height,
            measurementsCompleted = isDone,
            isFullyCompleted = isFull,
            updatedAt = System.currentTimeMillis()
        )
        parcelDao.updateParcel(updated)
    }

    suspend fun updateParcel(parcel: ParcelEntity) {
        val upperAddressee = parcel.addresseeName.trim().uppercase()
        val isFull = parcel.barcode.isNotBlank() &&
                parcel.destinationPincode.isNotBlank() &&
                upperAddressee.isNotBlank() &&
                parcel.physicalWeight > 0.0

        val updated = parcel.copy(
            addresseeName = upperAddressee,
            pincodeCompleted = parcel.destinationPincode.isNotBlank(),
            addresseeCompleted = upperAddressee.isNotBlank(),
            measurementsCompleted = parcel.physicalWeight > 0.0,
            isFullyCompleted = isFull
        )
        parcelDao.updateParcel(updated)
    }

    suspend fun deleteParcel(parcel: ParcelEntity) {
        parcelDao.deleteParcel(parcel)
    }

    suspend fun deleteParcelById(id: Long) {
        parcelDao.deleteParcelById(id)
    }

    suspend fun recordExport(batchId: Long, fileName: String, recordCount: Int) {
        exportHistoryDao.insertExportHistory(
            ExportHistoryEntity(
                batchId = batchId,
                fileName = fileName,
                exportedRecordCount = recordCount
            )
        )
        batchDao.updateBatchStatus(batchId, BatchEntity.STATUS_EXPORTED)
    }

    fun getExportHistory(batchId: Long): Flow<List<ExportHistoryEntity>> {
        return exportHistoryDao.getHistoryByBatch(batchId)
    }

    suspend fun getAllBatchesSync(): List<BatchEntity> {
        return batchDao.getAllBatches().first()
    }

    suspend fun getAllParcelsSync(): List<ParcelEntity> {
        return parcelDao.getAllParcelsSync()
    }

    suspend fun getAllExportHistorySync(): List<ExportHistoryEntity> {
        return exportHistoryDao.getAllHistorySync()
    }

    suspend fun restoreDatabaseData(
        batches: List<BatchEntity>,
        parcels: List<ParcelEntity>,
        exportHistories: List<ExportHistoryEntity>
    ) {
        batches.forEach { batchDao.insertBatch(it) }
        parcels.forEach { parcelDao.insertParcel(it) }
        exportHistories.forEach { exportHistoryDao.insertExportHistory(it) }
    }
}
