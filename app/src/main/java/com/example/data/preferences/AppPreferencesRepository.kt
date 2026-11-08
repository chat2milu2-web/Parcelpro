package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.models.ExcelColumnConfig
import com.example.data.models.ExcelTemplateConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parcel_settings")

class AppPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_PINCODE_LENGTH = intPreferencesKey("pincode_length")
        val KEY_WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val KEY_DIMENSION_UNIT = stringPreferencesKey("dimension_unit")
        val KEY_MAX_LENGTH = doublePreferencesKey("max_length")
        val KEY_MAX_BREADTH = doublePreferencesKey("max_breadth")
        val KEY_MAX_HEIGHT = doublePreferencesKey("max_height")
        val KEY_VOLUMETRIC_DIVISOR = doublePreferencesKey("volumetric_divisor")
        val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val KEY_SCAN_SOUND = booleanPreferencesKey("scan_sound")
        val KEY_DEFAULT_SCAN_MODE = stringPreferencesKey("default_scan_mode")
        val KEY_AUTO_OPEN_CAMERA = booleanPreferencesKey("auto_open_camera")
        val KEY_EXCEL_CONFIG_JSON = stringPreferencesKey("excel_config_json")
        val KEY_FIRST_TIME_OPEN = booleanPreferencesKey("is_first_time_open")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val KEY_DIMENSION_PRESETS_JSON = stringPreferencesKey("dimension_presets_json")
    }

    val dimensionPresets: Flow<List<com.example.data.models.DimensionPreset>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_DIMENSION_PRESETS_JSON]
        if (jsonStr.isNullOrEmpty()) {
            com.example.data.models.DimensionPreset.defaultPresets()
        } else {
            parseDimensionPresetsJson(jsonStr)
        }
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val isFirstTimeOpen: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_TIME_OPEN] ?: true
    }

    val pincodeLength: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_PINCODE_LENGTH] ?: 6
    }

    val weightUnit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEIGHT_UNIT] ?: "g"
    }

    val dimensionUnit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DIMENSION_UNIT] ?: "cm"
    }

    val maxLength: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_MAX_LENGTH] ?: 0.0 // 0.0 means no limit
    }

    val maxBreadth: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_MAX_BREADTH] ?: 0.0
    }

    val maxHeight: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_MAX_HEIGHT] ?: 0.0
    }

    val volumetricDivisor: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOLUMETRIC_DIVISOR] ?: 5000.0
    }

    val hapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTIC_FEEDBACK] ?: true
    }

    val scanSoundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCAN_SOUND] ?: true
    }

    val defaultScanMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SCAN_MODE] ?: "SINGLE"
    }

    val autoOpenCameraEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_OPEN_CAMERA] ?: false
    }

    val excelTemplateConfig: Flow<ExcelTemplateConfig> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_EXCEL_CONFIG_JSON]
        if (jsonStr.isNullOrEmpty()) {
            ExcelTemplateConfig()
        } else {
            parseTemplateConfigJson(jsonStr)
        }
    }

    suspend fun updatePincodeLength(length: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PINCODE_LENGTH] = length
        }
    }

    suspend fun updateWeightUnit(unit: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WEIGHT_UNIT] = unit
        }
    }

    suspend fun updateDimensionUnit(unit: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DIMENSION_UNIT] = unit
        }
    }

    suspend fun updateMaxLength(maxVal: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_LENGTH] = maxVal
        }
    }

    suspend fun updateMaxBreadth(maxVal: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_BREADTH] = maxVal
        }
    }

    suspend fun updateMaxHeight(maxVal: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_HEIGHT] = maxVal
        }
    }

    suspend fun updateVolumetricDivisor(divisor: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VOLUMETRIC_DIVISOR] = if (divisor > 0.0) divisor else 5000.0
        }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun updateScanSound(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCAN_SOUND] = enabled
        }
    }

    suspend fun updateDefaultScanMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_SCAN_MODE] = mode
        }
    }

    suspend fun updateAutoOpenCamera(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_OPEN_CAMERA] = enabled
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun toggleTheme(currentIsDark: Boolean) {
        val nextMode = if (currentIsDark) "LIGHT" else "DARK"
        updateThemeMode(nextMode)
    }

    suspend fun setFirstTimeOpenCompleted() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_TIME_OPEN] = false
        }
    }

    suspend fun updateExcelTemplateConfig(config: ExcelTemplateConfig) {
        val jsonStr = serializeTemplateConfigJson(config)
        context.dataStore.edit { prefs ->
            prefs[KEY_EXCEL_CONFIG_JSON] = jsonStr
        }
    }

    private fun serializeTemplateConfigJson(config: ExcelTemplateConfig): String {
        val jsonArray = JSONArray()
        config.columns.forEach { col ->
            val obj = JSONObject()
            obj.put("key", col.key)
            obj.put("defaultHeader", col.defaultHeader)
            obj.put("customHeader", col.customHeader)
            obj.put("enabled", col.enabled)
            obj.put("order", col.order)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun parseTemplateConfigJson(jsonStr: String): ExcelTemplateConfig {
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ExcelColumnConfig>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ExcelColumnConfig(
                        key = obj.getString("key"),
                        defaultHeader = obj.getString("defaultHeader"),
                        customHeader = obj.getString("customHeader"),
                        enabled = obj.optBoolean("enabled", true),
                        order = obj.optInt("order", i)
                    )
                )
            }
            ExcelTemplateConfig(list.sortedBy { it.order })
        } catch (e: Exception) {
            ExcelTemplateConfig()
        }
    }

    suspend fun updateDimensionPresets(presets: List<com.example.data.models.DimensionPreset>) {
        val jsonStr = serializeDimensionPresetsJson(presets)
        context.dataStore.edit { prefs ->
            prefs[KEY_DIMENSION_PRESETS_JSON] = jsonStr
        }
    }

    suspend fun addDimensionPreset(preset: com.example.data.models.DimensionPreset) {
        val current = dimensionPresets.firstOrNull() ?: com.example.data.models.DimensionPreset.defaultPresets()
        val updated = current + preset
        updateDimensionPresets(updated)
    }

    suspend fun deleteDimensionPreset(presetId: String) {
        val current = dimensionPresets.firstOrNull() ?: com.example.data.models.DimensionPreset.defaultPresets()
        val updated = current.filterNot { it.id == presetId }
        updateDimensionPresets(updated)
    }

    private fun serializeDimensionPresetsJson(list: List<com.example.data.models.DimensionPreset>): String {
        val array = JSONArray()
        list.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("length", p.length)
            obj.put("breadth", p.breadth)
            obj.put("height", p.height)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseDimensionPresetsJson(jsonStr: String): List<com.example.data.models.DimensionPreset> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<com.example.data.models.DimensionPreset>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.data.models.DimensionPreset(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        length = obj.getDouble("length"),
                        breadth = obj.getDouble("breadth"),
                        height = obj.getDouble("height")
                    )
                )
            }
            list.ifEmpty { com.example.data.models.DimensionPreset.defaultPresets() }
        } catch (e: Exception) {
            com.example.data.models.DimensionPreset.defaultPresets()
        }
    }
}
