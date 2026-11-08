package com.example.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parcels",
    foreignKeys = [
        ForeignKey(
            entity = BatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["batchId", "barcode"]),
        Index(value = ["batchId", "sequenceNumber"])
    ]
)
data class ParcelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: Long,
    val barcode: String,
    val destinationPincode: String = "",
    val addresseeName: String = "",
    val physicalWeight: Double = 0.0,
    val length: Double = 0.0,
    val breadth: Double = 0.0,
    val height: Double = 0.0,
    val sequenceNumber: Int = 0,
    val entryMode: String = MODE_PRIMARY,
    val pincodeCompleted: Boolean = false,
    val addresseeCompleted: Boolean = false,
    val measurementsCompleted: Boolean = false,
    val isFullyCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MODE_PRIMARY = "PRIMARY"
        const val MODE_SECONDARY = "SECONDARY"
    }
}
