package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batches")
data class BatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchCode: String, // e.g. YYB-20261106-001
    val batchDate: String, // YYYY-MM-DD
    val description: String,
    val status: String = STATUS_ACTIVE, // ACTIVE, COMPLETED, EXPORTED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_EXPORTED = "EXPORTED"
    }
}
