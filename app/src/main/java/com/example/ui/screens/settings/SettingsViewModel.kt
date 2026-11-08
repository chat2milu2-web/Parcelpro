package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.ExcelColumnConfig
import com.example.data.models.ExcelTemplateConfig
import com.example.data.preferences.AppPreferencesRepository
import com.example.util.AppUpdateManager
import com.example.util.AppUpdateUiState
import com.example.util.UpdateMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepository = AppPreferencesRepository(application)

    private val _updateState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    val updateState: StateFlow<AppUpdateUiState> = _updateState.asStateFlow()

    private var activeDownloadJob: Job? = null

    val installedVersionName: String = AppUpdateManager.installedVersionName
    val installedVersionCode: Int = AppUpdateManager.installedVersionCode

    fun checkForUpdates() {
        val currentState = _updateState.value
        if (currentState is AppUpdateUiState.Checking ||
            currentState is AppUpdateUiState.Downloading ||
            currentState is AppUpdateUiState.Verifying ||
            activeDownloadJob?.isActive == true
        ) {
            return
        }

        viewModelScope.launch {
            _updateState.value = AppUpdateUiState.Checking
            val result = AppUpdateManager.fetchLatestUpdateMetadata()
            result.fold(
                onSuccess = { metadata ->
                    if (AppUpdateManager.isUpdateAvailable(metadata.versionCode, installedVersionCode)) {
                        _updateState.value = AppUpdateUiState.UpdateAvailable(metadata)
                    } else {
                        _updateState.value = AppUpdateUiState.UpToDate(
                            currentVersionName = installedVersionName,
                            currentVersionCode = installedVersionCode
                        )
                    }
                },
                onFailure = { error ->
                    _updateState.value = AppUpdateUiState.Error(
                        message = error.localizedMessage ?: "Unable to check for updates. Please verify your internet connection."
                    )
                }
            )
        }
    }

    fun startUpdateDownload(metadata: UpdateMetadata) {
        val currentState = _updateState.value
        if (currentState is AppUpdateUiState.Downloading ||
            currentState is AppUpdateUiState.Verifying ||
            activeDownloadJob?.isActive == true
        ) {
            return
        }

        activeDownloadJob = viewModelScope.launch {
            try {
                _updateState.value = AppUpdateUiState.Downloading(metadata, 0)
                val appContext = getApplication<Application>().applicationContext
                val downloadResult = AppUpdateManager.downloadAndVerifyApk(
                    context = appContext,
                    metadata = metadata,
                    onProgress = { progress ->
                        _updateState.value = AppUpdateUiState.Downloading(metadata, progress)
                    },
                    onVerifying = {
                        _updateState.value = AppUpdateUiState.Verifying(metadata)
                    }
                )

                downloadResult.fold(
                    onSuccess = { apkFile ->
                        proceedWithVerifiedApk(metadata, apkFile)
                    },
                    onFailure = { error ->
                        _updateState.value = AppUpdateUiState.Error(
                            message = error.localizedMessage ?: "Download or SHA-256 verification failed.",
                            metadata = metadata
                        )
                    }
                )
            } catch (e: CancellationException) {
                _updateState.value = AppUpdateUiState.UpdateAvailable(metadata)
                throw e
            } finally {
                activeDownloadJob = null
            }
        }
    }

    fun cancelUpdateDownload() {
        val currentState = _updateState.value
        val metadata = when (currentState) {
            is AppUpdateUiState.Downloading -> currentState.metadata
            is AppUpdateUiState.Verifying -> currentState.metadata
            else -> null
        }
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        if (metadata != null) {
            _updateState.value = AppUpdateUiState.UpdateAvailable(metadata)
        } else {
            _updateState.value = AppUpdateUiState.Idle
        }
    }

    private fun proceedWithVerifiedApk(metadata: UpdateMetadata, apkFile: java.io.File) {
        val appContext = getApplication<Application>().applicationContext
        if (AppUpdateManager.canRequestPackageInstalls(appContext)) {
            _updateState.value = AppUpdateUiState.ReadyToInstall(metadata, apkFile)
            AppUpdateManager.launchApkInstaller(appContext, apkFile).onFailure { error ->
                _updateState.value = AppUpdateUiState.Error(
                    message = error.localizedMessage ?: "Could not open package installer.",
                    metadata = metadata
                )
            }
        } else {
            _updateState.value = AppUpdateUiState.PermissionRequired(metadata, apkFile)
        }
    }

    fun openInstallPermissionSettings() {
        val appContext = getApplication<Application>().applicationContext
        AppUpdateManager.openManageUnknownAppSourcesSettings(appContext).onFailure { error ->
            val currentState = _updateState.value
            val metadata = (currentState as? AppUpdateUiState.PermissionRequired)?.metadata
            _updateState.value = AppUpdateUiState.Error(
                message = error.localizedMessage ?: "Unable to open Android Unknown App Sources settings.",
                metadata = metadata
            )
        }
    }

    fun onResumeCheckInstallPermission() {
        val currentState = _updateState.value
        if (currentState is AppUpdateUiState.PermissionRequired) {
            val appContext = getApplication<Application>().applicationContext
            if (AppUpdateManager.canRequestPackageInstalls(appContext)) {
                proceedWithVerifiedApk(currentState.metadata, currentState.apkFile)
            }
        }
    }

    fun installDownloadedUpdate(metadata: UpdateMetadata, apkFile: java.io.File) {
        proceedWithVerifiedApk(metadata, apkFile)
    }

    val themeMode: StateFlow<String> = prefsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

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

    val hapticFeedback: StateFlow<Boolean> = prefsRepository.hapticFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val scanSound: StateFlow<Boolean> = prefsRepository.scanSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultScanMode: StateFlow<String> = prefsRepository.defaultScanMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SINGLE")

    val autoOpenCamera: StateFlow<Boolean> = prefsRepository.autoOpenCameraEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val excelConfig: StateFlow<ExcelTemplateConfig> = prefsRepository.excelTemplateConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExcelTemplateConfig())

    val dimensionPresets: StateFlow<List<com.example.data.models.DimensionPreset>> = prefsRepository.dimensionPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.models.DimensionPreset.defaultPresets())

    fun addDimensionPreset(name: String, length: Double, breadth: Double, height: Double) {
        viewModelScope.launch {
            if (name.isNotBlank() && length > 0 && breadth > 0 && height > 0) {
                val preset = com.example.data.models.DimensionPreset(
                    name = name.trim(),
                    length = length,
                    breadth = breadth,
                    height = height
                )
                prefsRepository.addDimensionPreset(preset)
            }
        }
    }

    fun deleteDimensionPreset(id: String) {
        viewModelScope.launch {
            prefsRepository.deleteDimensionPreset(id)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            prefsRepository.updateThemeMode(mode)
        }
    }

    fun updatePincodeLength(length: Int) {
        viewModelScope.launch {
            prefsRepository.updatePincodeLength(length)
        }
    }

    fun updateWeightUnit(unit: String) {
        viewModelScope.launch {
            prefsRepository.updateWeightUnit(unit)
        }
    }

    fun updateDimensionUnit(unit: String) {
        viewModelScope.launch {
            prefsRepository.updateDimensionUnit(unit)
        }
    }

    fun updateMaxLength(maxVal: Double) {
        viewModelScope.launch {
            prefsRepository.updateMaxLength(maxVal)
        }
    }

    fun updateMaxBreadth(maxVal: Double) {
        viewModelScope.launch {
            prefsRepository.updateMaxBreadth(maxVal)
        }
    }

    fun updateMaxHeight(maxVal: Double) {
        viewModelScope.launch {
            prefsRepository.updateMaxHeight(maxVal)
        }
    }

    fun updateVolumetricDivisor(divisor: Double) {
        viewModelScope.launch {
            prefsRepository.updateVolumetricDivisor(divisor)
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.updateHapticFeedback(enabled)
        }
    }

    fun updateScanSound(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.updateScanSound(enabled)
        }
    }

    fun updateDefaultScanMode(mode: String) {
        viewModelScope.launch {
            prefsRepository.updateDefaultScanMode(mode)
        }
    }

    fun updateAutoOpenCamera(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.updateAutoOpenCamera(enabled)
        }
    }

    fun updateExcelColumn(key: String, newHeader: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = excelConfig.value.columns.toMutableList()
            val index = current.indexOfFirst { it.key == key }
            if (index != -1) {
                current[index] = current[index].copy(
                    customHeader = newHeader,
                    enabled = enabled
                )
                prefsRepository.updateExcelTemplateConfig(ExcelTemplateConfig(current))
            }
        }
    }

    fun moveExcelColumn(index: Int, directionUp: Boolean) {
        viewModelScope.launch {
            val list = excelConfig.value.columns.toMutableList()
            val targetIndex = if (directionUp) index - 1 else index + 1
            if (targetIndex in 0 until list.size) {
                val temp = list[index]
                list[index] = list[targetIndex]
                list[targetIndex] = temp

                // re-index order
                val reordered = list.mapIndexed { i, item -> item.copy(order = i) }
                prefsRepository.updateExcelTemplateConfig(ExcelTemplateConfig(reordered))
            }
        }
    }

    fun resetExcelTemplateToDefault() {
        viewModelScope.launch {
            prefsRepository.updateExcelTemplateConfig(ExcelTemplateConfig())
        }
    }
}
