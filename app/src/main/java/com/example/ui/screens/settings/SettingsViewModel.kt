package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.ExcelColumnConfig
import com.example.data.models.ExcelTemplateConfig
import com.example.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepository = AppPreferencesRepository(application)

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
