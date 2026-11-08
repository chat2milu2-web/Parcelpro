package com.example.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entities.BatchEntity
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.ParcelRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val prefsRepository = AppPreferencesRepository(application)
    private val repository = ParcelRepository(
        database.batchDao(),
        database.parcelDao(),
        database.exportHistoryDao()
    )

    val themeMode: StateFlow<String> = prefsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val todayEntriesCount: StateFlow<Int> = repository.getTodayParcelCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayTotalWeight: StateFlow<Double> = repository.getTodayTotalWeight()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weightUnit: StateFlow<String> = prefsRepository.weightUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "g")

    val activeBatch: StateFlow<BatchEntity?> = repository.activeBatch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBatchParcelCount: StateFlow<Int> = activeBatch
        .flatMapLatest { batch ->
            if (batch != null) {
                repository.getParcelCountByBatch(batch.id)
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedBatchesCount: StateFlow<Int> = repository.completedBatchesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val exportedBatchesCount: StateFlow<Int> = repository.exportedBatchesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleTheme(currentIsDark: Boolean) {
        viewModelScope.launch {
            prefsRepository.toggleTheme(currentIsDark)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefsRepository.updateThemeMode(mode)
        }
    }

    fun createQuickBatch(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val batchId = repository.createBatch("Quick Office Data Entry Batch")
            onCreated(batchId)
        }
    }
}
