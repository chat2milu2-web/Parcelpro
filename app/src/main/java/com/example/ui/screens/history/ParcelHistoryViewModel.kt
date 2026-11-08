package com.example.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entities.BatchEntity
import com.example.data.entities.ParcelEntity
import com.example.data.preferences.AppPreferencesRepository
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

class ParcelHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ParcelRepository(
        database.batchDao(),
        database.parcelDao(),
        database.exportHistoryDao()
    )
    private val prefsRepository = AppPreferencesRepository(application)

    private val _selectedBatchId = MutableStateFlow<Long>(-1L)
    val selectedBatchId: StateFlow<Long> = _selectedBatchId.asStateFlow()

    private val _selectedBatch = MutableStateFlow<BatchEntity?>(null)
    val selectedBatch: StateFlow<BatchEntity?> = _selectedBatch.asStateFlow()

    val allBatches: StateFlow<List<BatchEntity>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawParcels: StateFlow<List<ParcelEntity>> = _selectedBatchId
        .flatMapLatest { id ->
            if (id > 0) {
                repository.getParcelsByBatch(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredParcels: StateFlow<List<ParcelEntity>> = combine(rawParcels, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.lowercase().trim()
            list.filter { p ->
                p.barcode.lowercase().contains(q) ||
                        p.destinationPincode.lowercase().contains(q) ||
                        p.addresseeName.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = rawParcels
        .combine(flowOf(0)) { list, _ -> list.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weightUnit: StateFlow<String> = prefsRepository.weightUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "g")

    val dimensionUnit: StateFlow<String> = prefsRepository.dimensionUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "cm")

    fun loadBatch(batchId: Long) {
        viewModelScope.launch {
            if (batchId > 0) {
                _selectedBatchId.value = batchId
                _selectedBatch.value = repository.getBatchById(batchId)
            } else {
                val active = repository.getActiveBatchSync()
                if (active != null) {
                    _selectedBatchId.value = active.id
                    _selectedBatch.value = active
                }
            }
        }
    }

    fun selectBatch(batchId: Long) {
        loadBatch(batchId)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateParcel(parcel: ParcelEntity) {
        viewModelScope.launch {
            repository.updateParcel(parcel)
        }
    }

    fun deleteParcel(parcelId: Long) {
        viewModelScope.launch {
            repository.deleteParcelById(parcelId)
        }
    }
}
