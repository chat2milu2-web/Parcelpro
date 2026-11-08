package com.example.ui.screens.export

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entities.BatchEntity
import com.example.data.entities.ExportHistoryEntity
import com.example.data.entities.ParcelEntity
import com.example.data.models.ExcelTemplateConfig
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.ParcelRepository
import com.example.excel.ExcelExporter
import com.example.excel.TextAndCsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    EXCEL(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel (.xlsx)"),
    CSV(".csv", "text/csv", "CSV File (.csv)"),
    TEXT(".txt", "text/plain", "Text Summary (.txt)")
}

class ExcelExportViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ParcelRepository(
        database.batchDao(),
        database.parcelDao(),
        database.exportHistoryDao()
    )
    private val prefsRepository = AppPreferencesRepository(application)

    val allBatches: StateFlow<List<BatchEntity>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedBatch = MutableStateFlow<BatchEntity?>(null)
    val selectedBatch: StateFlow<BatchEntity?> = _selectedBatch.asStateFlow()

    private val _batchParcels = MutableStateFlow<List<ParcelEntity>>(emptyList())
    val batchParcels: StateFlow<List<ParcelEntity>> = _batchParcels.asStateFlow()

    private val _exportHistory = MutableStateFlow<List<ExportHistoryEntity>>(emptyList())
    val exportHistory: StateFlow<List<ExportHistoryEntity>> = _exportHistory.asStateFlow()

    val templateConfig: StateFlow<ExcelTemplateConfig> = prefsRepository.excelTemplateConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExcelTemplateConfig())

    private val _selectedFormat = MutableStateFlow(ExportFormat.EXCEL)
    val selectedFormat: StateFlow<ExportFormat> = _selectedFormat.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatedFile = MutableStateFlow<File?>(null)
    val generatedFile: StateFlow<File?> = _generatedFile.asStateFlow()

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    fun setExportFormat(format: ExportFormat) {
        _selectedFormat.value = format
        _generatedFile.value = null // reset preview/cache
    }

    fun loadBatch(batchId: Long) {
        viewModelScope.launch {
            val all = repository.getAllBatchesSync()
            var targetBatch: BatchEntity? = null

            if (batchId > 0) {
                targetBatch = repository.getBatchById(batchId)
            }
            if (targetBatch == null) {
                targetBatch = repository.getActiveBatchSync()
            }
            // If active batch has 0 records, check if another batch has records
            if (targetBatch != null) {
                val count = repository.getParcelsByBatchSync(targetBatch.id).size
                if (count == 0 && all.isNotEmpty()) {
                    val batchWithData = all.firstOrNull { repository.getParcelsByBatchSync(it.id).isNotEmpty() }
                    if (batchWithData != null) {
                        targetBatch = batchWithData
                    }
                }
            } else if (all.isNotEmpty()) {
                targetBatch = all.firstOrNull { repository.getParcelsByBatchSync(it.id).isNotEmpty() } ?: all.first()
            }

            if (targetBatch != null) {
                _selectedBatch.value = targetBatch
                _batchParcels.value = repository.getParcelsByBatchSync(targetBatch.id)
                _exportHistory.value = repository.getExportHistory(targetBatch.id).first()
                _generatedFile.value = null
            }
        }
    }

    fun selectBatch(batchId: Long) {
        viewModelScope.launch {
            val batch = repository.getBatchById(batchId)
            if (batch != null) {
                _selectedBatch.value = batch
                _batchParcels.value = repository.getParcelsByBatchSync(batch.id)
                _exportHistory.value = repository.getExportHistory(batch.id).first()
                _generatedFile.value = null
            }
        }
    }

    fun generateSuggestedFileName(batch: BatchEntity, format: ExportFormat = _selectedFormat.value): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "${batch.batchCode}_Export_$dateStr${format.extension}"
    }

    /**
     * Step 1: Pre-generate and verify the complete file locally.
     * Guaranteed non-empty and uncorrupted file ready to be saved or shared.
     */
    fun preGenerateFile(context: Context, onComplete: ((File) -> Unit)? = null) {
        val batch = selectedBatch.value ?: return
        val parcels = batchParcels.value
        val config = templateConfig.value
        val format = selectedFormat.value

        if (parcels.isEmpty()) {
            _exportStatus.value = "Cannot generate file: This batch has 0 parcel records!"
            return
        }

        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = generateSuggestedFileName(batch, format)
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val targetFile = File(exportDir, fileName)

                val bytes = when (format) {
                    ExportFormat.EXCEL -> {
                        val byteBuffer = ByteArrayOutputStream()
                        ExcelExporter.exportBatchToExcel(
                            batch = batch,
                            parcels = parcels,
                            templateConfig = config,
                            outputStream = byteBuffer
                        )
                        byteBuffer.toByteArray()
                    }
                    ExportFormat.CSV -> {
                        val csvContent = TextAndCsvExporter.generateCsv(parcels, config)
                        csvContent.toByteArray(Charsets.UTF_8)
                    }
                    ExportFormat.TEXT -> {
                        val txtContent = TextAndCsvExporter.generateFormattedText(batch, parcels, config)
                        txtContent.toByteArray(Charsets.UTF_8)
                    }
                }

                if (bytes.isEmpty()) {
                    throw IllegalStateException("Generated file content is empty.")
                }

                FileOutputStream(targetFile).use { out ->
                    out.write(bytes)
                    out.flush()
                }

                _generatedFile.value = targetFile

                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    val sizeKb = (targetFile.length() / 1024L).coerceAtLeast(1L)
                    _exportStatus.value = "Ready! ${format.label} generated successfully ($sizeKb KB, ${parcels.size} records)."
                    onComplete?.invoke(targetFile)
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    _exportStatus.value = "Generation failed: ${e.localizedMessage}"
                }
            }
        }
    }

    /**
     * Direct 1-Click Save to Local Device Storage (Downloads/Documents folder)
     */
    fun saveDirectlyToLocalStorage(context: Context) {
        val batch = selectedBatch.value ?: return
        val parcels = batchParcels.value
        val format = selectedFormat.value

        if (parcels.isEmpty()) {
            _exportStatus.value = "Cannot save: Batch has 0 parcel records!"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure file is ready
                var localFile = _generatedFile.value
                if (localFile == null || !localFile.exists() || localFile.length() == 0L) {
                    val fileName = generateSuggestedFileName(batch, format)
                    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                    localFile = File(exportDir, fileName)

                    val bytes = when (format) {
                        ExportFormat.EXCEL -> {
                            val byteBuffer = ByteArrayOutputStream()
                            ExcelExporter.exportBatchToExcel(
                                batch = batch,
                                parcels = parcels,
                                templateConfig = templateConfig.value,
                                outputStream = byteBuffer
                            )
                            byteBuffer.toByteArray()
                        }
                        ExportFormat.CSV -> {
                            TextAndCsvExporter.generateCsv(parcels, templateConfig.value).toByteArray(Charsets.UTF_8)
                        }
                        ExportFormat.TEXT -> {
                            TextAndCsvExporter.generateFormattedText(batch, parcels, templateConfig.value).toByteArray(Charsets.UTF_8)
                        }
                    }
                    FileOutputStream(localFile).use { out ->
                        out.write(bytes)
                        out.flush()
                    }
                    _generatedFile.value = localFile
                }

                val bytes = localFile.readBytes()
                val fileName = localFile.name

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Modern Android 10+ MediaStore Download Directory
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ParcelEntryPro")
                    }
                    val contentUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw java.io.IOException("Unable to create MediaStore record.")

                    context.contentResolver.openOutputStream(contentUri)?.use { out ->
                        out.write(bytes)
                        out.flush()
                    }
                } else {
                    // Legacy Download directory
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val appDir = File(downloadsDir, "ParcelEntryPro").apply { mkdirs() }
                    val destFile = File(appDir, fileName)
                    FileOutputStream(destFile).use { out ->
                        out.write(bytes)
                        out.flush()
                    }
                }

                repository.recordExport(batch.id, fileName, parcels.size)
                loadBatch(batch.id)

                withContext(Dispatchers.Main) {
                    _exportStatus.value = "Saved successfully to Downloads/ParcelEntryPro folder: $fileName (${bytes.size / 1024} KB)"
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _exportStatus.value = "Save to local storage failed: ${e.localizedMessage}"
                }
            }
        }
    }

    /**
     * Save via Android Storage Access Framework (Folder Picker)
     */
    fun exportToUri(context: Context, uri: Uri, onComplete: () -> Unit) {
        val batch = selectedBatch.value ?: return
        val parcels = batchParcels.value
        val format = selectedFormat.value

        if (parcels.isEmpty()) {
            _exportStatus.value = "Cannot export an empty batch! Please add parcel records first."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = _generatedFile.value
                val bytes = if (file != null && file.exists() && file.length() > 0L) {
                    file.readBytes()
                } else {
                    val byteBuffer = ByteArrayOutputStream()
                    when (format) {
                        ExportFormat.EXCEL -> {
                            ExcelExporter.exportBatchToExcel(
                                batch = batch,
                                parcels = parcels,
                                templateConfig = templateConfig.value,
                                outputStream = byteBuffer
                            )
                        }
                        ExportFormat.CSV -> {
                            byteBuffer.write(TextAndCsvExporter.generateCsv(parcels, templateConfig.value).toByteArray(Charsets.UTF_8))
                        }
                        ExportFormat.TEXT -> {
                            byteBuffer.write(TextAndCsvExporter.generateFormattedText(batch, parcels, templateConfig.value).toByteArray(Charsets.UTF_8))
                        }
                    }
                    byteBuffer.toByteArray()
                }

                val outputStream = context.contentResolver.openOutputStream(uri, "rwt")
                    ?: context.contentResolver.openOutputStream(uri)
                    ?: throw java.io.IOException("Unable to open output stream for chosen destination.")

                outputStream.use { out ->
                    out.write(bytes)
                    out.flush()
                }

                val fileName = uri.lastPathSegment ?: generateSuggestedFileName(batch, format)
                repository.recordExport(batch.id, fileName, parcels.size)

                withContext(Dispatchers.Main) {
                    _exportStatus.value = "${format.label} saved successfully with ${parcels.size} records (${bytes.size / 1024} KB)!"
                    loadBatch(batch.id)
                    onComplete()
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _exportStatus.value = "Export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    /**
     * Share directly via WhatsApp, Gmail, Drive, etc.
     */
    fun shareExportFile(context: Context) {
        val batch = selectedBatch.value ?: return
        val parcels = batchParcels.value
        val format = selectedFormat.value

        if (parcels.isEmpty()) {
            _exportStatus.value = "Cannot share an empty batch! Please enter parcel data first."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var exportFile = _generatedFile.value
                if (exportFile == null || !exportFile.exists() || exportFile.length() == 0L) {
                    val fileName = generateSuggestedFileName(batch, format)
                    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                    exportFile = File(exportDir, fileName)

                    val bytes = when (format) {
                        ExportFormat.EXCEL -> {
                            val byteBuffer = ByteArrayOutputStream()
                            ExcelExporter.exportBatchToExcel(
                                batch = batch,
                                parcels = parcels,
                                templateConfig = templateConfig.value,
                                outputStream = byteBuffer
                            )
                            byteBuffer.toByteArray()
                        }
                        ExportFormat.CSV -> {
                            TextAndCsvExporter.generateCsv(parcels, templateConfig.value).toByteArray(Charsets.UTF_8)
                        }
                        ExportFormat.TEXT -> {
                            TextAndCsvExporter.generateFormattedText(batch, parcels, templateConfig.value).toByteArray(Charsets.UTF_8)
                        }
                    }

                    FileOutputStream(exportFile).use { out ->
                        out.write(bytes)
                        out.flush()
                    }
                    _generatedFile.value = exportFile
                }

                val authority = "${context.packageName}.fileprovider"
                val contentUri = FileProvider.getUriForFile(context, authority, exportFile)

                repository.recordExport(batch.id, exportFile.name, parcels.size)
                loadBatch(batch.id)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = format.mimeType
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Parcel Entry Export - ${batch.batchCode} (${parcels.size} Parcels)")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Attached is the ${format.label} export for Batch ${batch.batchCode} with ${parcels.size} parcel entries generated via Parcel Entry Pro."
                    )
                    clipData = android.content.ClipData.newRawUri("Parcel Export", contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Share ${format.label} via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    try {
                        context.startActivity(chooser)
                        _exportStatus.value = "Sharing ${format.label}: ${exportFile.name}"
                    } catch (activityEx: Exception) {
                        _exportStatus.value = "No application found to share file: ${activityEx.localizedMessage}"
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _exportStatus.value = "Share failed: ${e.localizedMessage}"
                }
            }
        }
    }

    fun dismissStatus() {
        _exportStatus.value = null
    }
}
