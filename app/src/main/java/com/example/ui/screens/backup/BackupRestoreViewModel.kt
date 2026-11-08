package com.example.ui.screens.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupData
import com.example.backup.BackupManager
import com.example.data.database.AppDatabase
import com.example.data.repository.ParcelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ParcelRepository(
        database.batchDao(),
        database.parcelDao(),
        database.exportHistoryDao()
    )

    val totalBatches: StateFlow<Int> = repository.totalBatchesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalParcelsToday: StateFlow<Int> = repository.getTodayParcelCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _pendingRestoreData = MutableStateFlow<BackupData?>(null)
    val pendingRestoreData: StateFlow<BackupData?> = _pendingRestoreData.asStateFlow()

    fun generateBackupFileName(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "ParcelEntryPro_Backup_$dateStr.json"
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val batches = repository.getAllBatchesSync()
                val parcels = repository.getAllParcelsSync()
                val exportHistories = repository.getAllExportHistorySync()

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    BackupManager.exportBackupToJson(
                        batches = batches,
                        parcels = parcels,
                        exportHistories = exportHistories,
                        outputStream = outputStream
                    )
                }

                _statusMessage.value = "Backup exported successfully! (${batches.size} batches, ${parcels.size} parcels saved)."
            } catch (e: Exception) {
                _statusMessage.value = "Backup export failed: ${e.localizedMessage}"
            }
        }
    }

    fun readAndValidateRestoreFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val backupData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BackupManager.parseAndValidateBackupJson(inputStream)
                }

                if (backupData != null) {
                    _pendingRestoreData.value = backupData
                } else {
                    _statusMessage.value = "Invalid backup file."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to parse backup file: ${e.localizedMessage}"
            }
        }
    }

    fun confirmRestore() {
        val backupData = pendingRestoreData.value ?: return
        viewModelScope.launch {
            try {
                repository.restoreDatabaseData(
                    batches = backupData.batches,
                    parcels = backupData.parcels,
                    exportHistories = backupData.exportHistories
                )
                _statusMessage.value = "Database restored successfully! (${backupData.batches.size} batches, ${backupData.parcels.size} parcels restored)."
                _pendingRestoreData.value = null
            } catch (e: Exception) {
                _statusMessage.value = "Restore failed: ${e.localizedMessage}"
            }
        }
    }

    fun cancelRestore() {
        _pendingRestoreData.value = null
    }

    fun dismissStatus() {
        _statusMessage.value = null
    }
}
