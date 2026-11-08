package com.example.ui.screens.batch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entities.BatchEntity
import com.example.data.models.BatchWithStats
import com.example.data.repository.ParcelRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BatchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ParcelRepository(
        database.batchDao(),
        database.parcelDao(),
        database.exportHistoryDao()
    )

    val activeBatch: StateFlow<BatchEntity?> = repository.activeBatch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allBatches: StateFlow<List<BatchEntity>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _suggestedBatchCode = MutableStateFlow("")
    val suggestedBatchCode: StateFlow<String> = _suggestedBatchCode.asStateFlow()

    init {
        generateNewBatchCode()
    }

    fun generateNewBatchCode() {
        viewModelScope.launch {
            _suggestedBatchCode.value = repository.generateUniqueBatchCode()
        }
    }

    fun createBatch(description: String, customBatchCode: String? = null, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val batchId = repository.createBatch(description, customBatchCode)
            repository.setBatchActive(batchId)
            onCreated(batchId)
        }
    }

    fun setActiveBatch(batchId: Long) {
        viewModelScope.launch {
            repository.setBatchActive(batchId)
        }
    }

    fun completeBatch(batchId: Long) {
        viewModelScope.launch {
            repository.completeBatch(batchId)
        }
    }

    fun deleteBatch(batchId: Long) {
        viewModelScope.launch {
            repository.deleteBatch(batchId)
        }
    }
}
