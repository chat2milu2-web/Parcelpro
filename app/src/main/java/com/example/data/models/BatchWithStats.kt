package com.example.data.models

import com.example.data.entities.BatchEntity

data class BatchWithStats(
    val batch: BatchEntity,
    val parcelCount: Int,
    val lastExportDate: Long? = null
)
