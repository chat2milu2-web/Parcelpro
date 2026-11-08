package com.example.ui.screens.entry

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entities.BatchEntity
import com.example.data.entities.ParcelEntity
import com.example.data.pincode.IndianPincodeDirectory
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.ParcelRepository
import com.example.util.SoundFeedbackHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ParcelFormState(
    val barcode: String = "",
    val pincode: String = "",
    val resolvedLocation: String? = null,
    val addressee: String = "",
    val weight: String = "",
    val length: String = "",
    val breadth: String = "",
    val height: String = "",
    val addressPhotoUri: String? = null,
    val ocrNotice: String? = null,
    val errorMessage: String? = null,
    val duplicateWarningParcel: ParcelEntity? = null,
    val crossBatchWarning: String? = null,
    val lockWeight: Boolean = false,
    val lockDimensions: Boolean = false
)

class ParcelEntryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ParcelRepository(
        database.batchDao(),
        database.parcelDao(),
        database.exportHistoryDao()
    )
    private val prefsRepository = AppPreferencesRepository(application)
    private val soundHelper = SoundFeedbackHelper(application)

    private val _formState = MutableStateFlow(ParcelFormState())
    val formState: StateFlow<ParcelFormState> = _formState.asStateFlow()

    private val _currentBatch = MutableStateFlow<BatchEntity?>(null)
    val currentBatch: StateFlow<BatchEntity?> = _currentBatch.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val batchBarcodes: StateFlow<Set<String>> = _currentBatch
        .flatMapLatest { batch ->
            if (batch != null) {
                repository.getParcelsByBatch(batch.id).map { list -> list.map { it.barcode.trim() }.toSet() }
            } else {
                flowOf(emptySet())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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

    val autoOpenCamera: StateFlow<Boolean> = prefsRepository.autoOpenCameraEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hapticFeedbackEnabled: StateFlow<Boolean> = prefsRepository.hapticFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val scanSoundEnabled: StateFlow<Boolean> = prefsRepository.scanSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultScanModeStr: StateFlow<String> = prefsRepository.defaultScanMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SINGLE")

    val dimensionPresets: StateFlow<List<com.example.data.models.DimensionPreset>> = prefsRepository.dimensionPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.models.DimensionPreset.defaultPresets())

    private val _recentSavedMessage = MutableStateFlow<String?>(null)
    val recentSavedMessage: StateFlow<String?> = _recentSavedMessage.asStateFlow()

    fun loadBatch(batchId: Long) {
        viewModelScope.launch {
            if (batchId > 0) {
                val batch = repository.getBatchById(batchId)
                if (batch != null) {
                    _currentBatch.value = batch
                    repository.setBatchActive(batchId)
                }
            } else {
                val active = repository.getActiveBatchSync()
                if (active != null) {
                    _currentBatch.value = active
                } else {
                    val newBatchId = repository.createBatch("Auto-Created Active Batch")
                    _currentBatch.value = repository.getBatchById(newBatchId)
                }
            }
        }
    }

    fun updateBarcode(value: String, onDuplicateFound: ((ParcelEntity) -> Unit)? = null) {
        val trimmed = value.trim().uppercase().take(com.example.scanner.BarcodeAccuracyVerifier.EXACT_LENGTH)
        _formState.value = _formState.value.copy(
            barcode = trimmed,
            errorMessage = null,
            crossBatchWarning = null
        )

        val batch = currentBatch.value
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                // 1. Check duplicate within current batch
                if (batch != null) {
                    val duplicateInBatch = repository.findDuplicateBarcodeInBatch(batch.id, trimmed)
                    if (duplicateInBatch != null) {
                        _formState.value = _formState.value.copy(
                            duplicateWarningParcel = duplicateInBatch,
                            errorMessage = "DUPLICATE: Barcode '$trimmed' already scanned in this batch!"
                        )
                        soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
                        onDuplicateFound?.invoke(duplicateInBatch)
                        return@launch
                    }
                }

                // 2. Check cross-batch duplicate across history
                val crossDuplicate = repository.findDuplicateBarcodeAnyBatch(trimmed)
                if (crossDuplicate != null && crossDuplicate.batchId != batch?.id) {
                    _formState.value = _formState.value.copy(
                        crossBatchWarning = "Note: Barcode '$trimmed' was already logged in Batch #${crossDuplicate.batchId}."
                    )
                } else if (trimmed.length == com.example.scanner.BarcodeAccuracyVerifier.EXACT_LENGTH &&
                    com.example.scanner.BarcodeAccuracyVerifier.STRICT_BARCODE_REGEX.matches(trimmed)
                ) {
                    soundHelper.playSuccessFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
                }
            }
        }
    }

    fun updatePincode(value: String) {
        val requiredLength = pincodeLength.value
        val filtered = value.filter { it.isDigit() }.take(requiredLength)
        val resolved = if (filtered.length == 6) {
            IndianPincodeDirectory.lookup(filtered)?.displayName
        } else {
            null
        }

        _formState.value = _formState.value.copy(
            pincode = filtered,
            resolvedLocation = resolved,
            errorMessage = null
        )
    }

    fun updateAddressee(value: String) {
        _formState.value = _formState.value.copy(
            addressee = value.uppercase(),
            errorMessage = null
        )
    }

    fun updateWeight(value: String) {
        _formState.value = _formState.value.copy(
            weight = value,
            errorMessage = null
        )
    }

    fun updateLength(value: String) {
        _formState.value = _formState.value.copy(
            length = value,
            errorMessage = null
        )
    }

    fun updateBreadth(value: String) {
        _formState.value = _formState.value.copy(
            breadth = value,
            errorMessage = null
        )
    }

    fun updateHeight(value: String) {
        _formState.value = _formState.value.copy(
            height = value,
            errorMessage = null
        )
    }

    fun onAddressPhotoScanned(name: String?, pin: String?, photoUri: Uri?) {
        val current = _formState.value
        val newName = if (!name.isNullOrBlank()) name.uppercase() else current.addressee
        val newPin = if (!pin.isNullOrBlank()) {
            pin.filter { it.isDigit() }.take(pincodeLength.value)
        } else {
            current.pincode
        }

        val resolved = if (newPin.length == 6) {
            IndianPincodeDirectory.lookup(newPin)?.displayName
        } else {
            current.resolvedLocation
        }

        val noticeParts = mutableListOf<String>()
        if (!name.isNullOrBlank()) noticeParts.add("Name: $newName")
        if (!pin.isNullOrBlank()) noticeParts.add("PIN: $newPin")

        val notice = if (noticeParts.isNotEmpty()) {
            "OCR Detected: ${noticeParts.joinToString(" • ")}"
        } else {
            "Address photo captured. Please enter details manually if missing."
        }

        soundHelper.playOcrCaptureFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)

        _formState.value = current.copy(
            addressee = newName,
            pincode = newPin,
            resolvedLocation = resolved,
            addressPhotoUri = photoUri?.toString(),
            ocrNotice = notice,
            errorMessage = null
        )
    }

    fun clearAddressPhoto() {
        _formState.value = _formState.value.copy(
            addressPhotoUri = null,
            ocrNotice = null
        )
    }

    fun toggleLockWeight(locked: Boolean) {
        _formState.value = _formState.value.copy(lockWeight = locked)
    }

    fun toggleLockDimensions(locked: Boolean) {
        _formState.value = _formState.value.copy(lockDimensions = locked)
    }

    fun clearForm() {
        val current = _formState.value
        _formState.value = ParcelFormState(
            lockWeight = current.lockWeight,
            weight = if (current.lockWeight) current.weight else "",
            lockDimensions = current.lockDimensions,
            length = if (current.lockDimensions) current.length else "",
            breadth = if (current.lockDimensions) current.breadth else "",
            height = if (current.lockDimensions) current.height else ""
        )
    }

    fun dismissDuplicateWarning() {
        _formState.value = _formState.value.copy(duplicateWarningParcel = null)
    }

    fun dismissCrossBatchWarning() {
        _formState.value = _formState.value.copy(crossBatchWarning = null)
    }

    fun dismissSuccessMessage() {
        _recentSavedMessage.value = null
    }

    fun saveParcel(
        andNavigateToHistory: Boolean = false,
        overrideDuplicate: Boolean = false,
        onSuccess: () -> Unit
    ) {
        val batch = currentBatch.value
        if (batch == null) {
            _formState.value = _formState.value.copy(errorMessage = "No active batch selected.")
            return
        }

        val state = _formState.value
        val barcode = state.barcode.trim()
        val pincode = state.pincode.trim()
        val addressee = state.addressee.trim().uppercase()
        val weightVal = state.weight.toDoubleOrNull()
        val lengthVal = state.length.toDoubleOrNull() ?: 0.0
        val breadthVal = state.breadth.toDoubleOrNull() ?: 0.0
        val heightVal = state.height.toDoubleOrNull() ?: 0.0

        // Validation
        if (barcode.isBlank()) {
            _formState.value = state.copy(errorMessage = "Barcode number is required.")
            soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
            return
        }

        if (barcode.length != com.example.scanner.BarcodeAccuracyVerifier.EXACT_LENGTH ||
            !com.example.scanner.BarcodeAccuracyVerifier.STRICT_BARCODE_REGEX.matches(barcode)
        ) {
            _formState.value = state.copy(
                errorMessage = "Invalid barcode '$barcode': Must be exactly 13 characters (2 uppercase letters + 9 digits + IN, e.g. EW112223445IN)."
            )
            soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
            return
        }

        val requiredPincodeLength = pincodeLength.value
        if (pincode.length != requiredPincodeLength) {
            _formState.value = state.copy(errorMessage = "Destination Pincode must be exactly $requiredPincodeLength digits.")
            soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
            return
        }

        if (addressee.isBlank()) {
            _formState.value = state.copy(errorMessage = "Addressee Name is required.")
            soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
            return
        }

        if (weightVal == null || weightVal <= 0.0) {
            _formState.value = state.copy(errorMessage = "Please enter a valid physical weight (> 0).")
            soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
            return
        }

        val dimUnit = dimensionUnit.value
        val maxL = maxLength.value
        if (maxL > 0.0 && lengthVal > maxL) {
            _formState.value = state.copy(errorMessage = "Length ($lengthVal $dimUnit) exceeds maximum allowed limit ($maxL $dimUnit).")
            soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
            return
        }

        val maxB = maxBreadth.value
        if (maxB > 0.0 && breadthVal > maxB) {
            _formState.value = state.copy(errorMessage = "Breadth ($breadthVal $dimUnit) exceeds maximum allowed limit ($maxB $dimUnit).")
            soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
            return
        }

        val maxH = maxHeight.value
        if (maxH > 0.0 && heightVal > maxH) {
            _formState.value = state.copy(errorMessage = "Height ($heightVal $dimUnit) exceeds maximum allowed limit ($maxH $dimUnit).")
            soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
            return
        }

        viewModelScope.launch {
            // Duplicate check within current batch
            if (!overrideDuplicate) {
                val duplicate = repository.findDuplicateBarcodeInBatch(batch.id, barcode)
                if (duplicate != null) {
                    _formState.value = state.copy(duplicateWarningParcel = duplicate)
                    soundHelper.playWarningFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
                    return@launch
                }
            }

            val parcel = ParcelEntity(
                batchId = batch.id,
                barcode = barcode,
                destinationPincode = pincode,
                addresseeName = addressee,
                physicalWeight = weightVal,
                length = lengthVal,
                breadth = breadthVal,
                height = heightVal
            )

            repository.insertParcel(parcel)
            soundHelper.playSuccessFeedback(scanSoundEnabled.value, hapticFeedbackEnabled.value)
            _recentSavedMessage.value = "Parcel $barcode saved successfully!"
            clearForm()

            onSuccess()
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundHelper.release()
    }
}
