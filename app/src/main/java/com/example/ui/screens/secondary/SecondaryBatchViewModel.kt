package com.example.ui.screens.secondary

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

class SecondaryBatchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ParcelRepository(
        database.batchDao(),
        database.parcelDao(),
        database.exportHistoryDao()
    )
    private val prefsRepository = AppPreferencesRepository(application)

    private val _batchId = MutableStateFlow<Long>(-1L)
    val batchId: StateFlow<Long> = _batchId.asStateFlow()

    private val _currentBatch = MutableStateFlow<BatchEntity?>(null)
    val currentBatch: StateFlow<BatchEntity?> = _currentBatch.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val parcels: StateFlow<List<ParcelEntity>> = _batchId
        .flatMapLatest { id ->
            if (id > 0) repository.getParcelsByBatch(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pincodeCompletedCount: StateFlow<Int> = _batchId
        .flatMapLatest { id ->
            if (id > 0) repository.getPincodeCompletedCount(id) else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val addresseeCompletedCount: StateFlow<Int> = _batchId
        .flatMapLatest { id ->
            if (id > 0) repository.getAddresseeCompletedCount(id) else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val measurementsCompletedCount: StateFlow<Int> = _batchId
        .flatMapLatest { id ->
            if (id > 0) repository.getMeasurementsCompletedCount(id) else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val fullyCompletedCount: StateFlow<Int> = _batchId
        .flatMapLatest { id ->
            if (id > 0) repository.getFullyCompletedCount(id) else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pincodeLength: StateFlow<Int> = prefsRepository.pincodeLength
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val weightUnit: StateFlow<String> = prefsRepository.weightUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "g")

    val dimensionUnit: StateFlow<String> = prefsRepository.dimensionUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "cm")

    val maxLength: StateFlow<Double> = prefsRepository.maxLength
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val maxBreadth: StateFlow<Double> = prefsRepository.maxBreadth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val maxHeight: StateFlow<Double> = prefsRepository.maxHeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val volumetricDivisor: StateFlow<Double> = prefsRepository.volumetricDivisor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5000.0)

    val hapticFeedbackEnabled: StateFlow<Boolean> = prefsRepository.hapticFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val scanSoundEnabled: StateFlow<Boolean> = prefsRepository.scanSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _currentParcelIndex = MutableStateFlow(0)
    val currentParcelIndex: StateFlow<Int> = _currentParcelIndex.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun loadBatch(bId: Long) {
        viewModelScope.launch {
            if (bId > 0) {
                _batchId.value = bId
                _currentBatch.value = repository.getBatchById(bId)
            } else {
                val active = repository.getActiveBatchSync()
                if (active != null) {
                    _batchId.value = active.id
                    _currentBatch.value = active
                } else {
                    val newId = repository.createBatch("Secondary Mode Batch")
                    _batchId.value = newId
                    _currentBatch.value = repository.getBatchById(newId)
                }
            }
        }
    }

    fun setParcelIndex(index: Int) {
        val max = (parcels.value.size - 1).coerceAtLeast(0)
        _currentParcelIndex.value = index.coerceIn(0, max)
    }

    fun nextParcel() {
        val max = (parcels.value.size - 1).coerceAtLeast(0)
        if (_currentParcelIndex.value < max) {
            _currentParcelIndex.value += 1
        }
    }

    fun previousParcel() {
        if (_currentParcelIndex.value > 0) {
            _currentParcelIndex.value -= 1
        }
    }

    fun addBarcode(barcode: String, onComplete: (Boolean) -> Unit) {
        val bId = batchId.value
        if (bId <= 0) return
        val trimmed = barcode.trim().uppercase()
        if (trimmed.length != com.example.scanner.BarcodeAccuracyVerifier.EXACT_LENGTH ||
            !com.example.scanner.BarcodeAccuracyVerifier.STRICT_BARCODE_REGEX.matches(trimmed)
        ) {
            _statusMessage.value = "Invalid barcode '$trimmed': Expected 13 chars format (e.g. EW112223445IN)."
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val result = repository.addSecondaryBarcode(bId, trimmed)
            if (result == -1L) {
                _statusMessage.value = "Duplicate barcode '$trimmed' exists in this batch!"
                onComplete(false)
            } else {
                _statusMessage.value = "Verified Barcode: $trimmed"
                onComplete(true)
            }
        }
    }

    fun savePincode(parcelId: Long, pincode: String) {
        viewModelScope.launch {
            val reqLen = pincodeLength.value
            repository.updatePincodeForParcel(parcelId, pincode, reqLen)
            nextParcel()
        }
    }

    fun saveAddressee(parcelId: Long, addressee: String) {
        viewModelScope.launch {
            repository.updateAddresseeForParcel(parcelId, addressee.uppercase())
            nextParcel()
        }
    }

    fun saveOcrAddressInfo(parcelId: Long, addressee: String?, pincode: String?) {
        viewModelScope.launch {
            if (!addressee.isNullOrBlank()) {
                repository.updateAddresseeForParcel(parcelId, addressee.uppercase())
            }
            if (!pincode.isNullOrBlank()) {
                val reqLen = pincodeLength.value
                val filtered = pincode.filter { it.isDigit() }.take(reqLen)
                if (filtered.length == reqLen) {
                    repository.updatePincodeForParcel(parcelId, filtered, reqLen)
                }
            }
            _statusMessage.value = "Updated details from Address Photo!"
        }
    }

    fun saveMeasurements(
        parcelId: Long,
        weight: Double,
        length: Double,
        breadth: Double,
        height: Double
    ): Boolean {
        val dimUnit = dimensionUnit.value
        val maxL = maxLength.value
        if (maxL > 0.0 && length > maxL) {
            _statusMessage.value = "Length ($length $dimUnit) exceeds max limit of $maxL $dimUnit."
            return false
        }
        val maxB = maxBreadth.value
        if (maxB > 0.0 && breadth > maxB) {
            _statusMessage.value = "Breadth ($breadth $dimUnit) exceeds max limit of $maxB $dimUnit."
            return false
        }
        val maxH = maxHeight.value
        if (maxH > 0.0 && height > maxH) {
            _statusMessage.value = "Height ($height $dimUnit) exceeds max limit of $maxH $dimUnit."
            return false
        }

        viewModelScope.launch {
            repository.updateMeasurementsForParcel(parcelId, weight, length, breadth, height)
            nextParcel()
        }
        return true
    }

    fun updateParcelFull(parcel: ParcelEntity) {
        viewModelScope.launch {
            repository.updateParcel(parcel)
        }
    }

    fun deleteParcel(parcelId: Long) {
        viewModelScope.launch {
            repository.deleteParcelById(parcelId)
            val max = (parcels.value.size - 2).coerceAtLeast(0)
            _currentParcelIndex.value = _currentParcelIndex.value.coerceIn(0, max)
        }
    }

    fun dismissStatus() {
        _statusMessage.value = null
    }
}
