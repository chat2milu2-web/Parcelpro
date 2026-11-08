package com.example.backup

import com.example.data.entities.BatchEntity
import com.example.data.entities.ExportHistoryEntity
import com.example.data.entities.ParcelEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val batches: List<BatchEntity>,
    val parcels: List<ParcelEntity>,
    val exportHistories: List<ExportHistoryEntity>
)

object BackupManager {

    fun exportBackupToJson(
        batches: List<BatchEntity>,
        parcels: List<ParcelEntity>,
        exportHistories: List<ExportHistoryEntity>,
        outputStream: OutputStream
    ) {
        val rootObj = JSONObject()
        rootObj.put("version", 1)
        rootObj.put("appName", "Parcel Entry Pro")
        rootObj.put("exportedAt", System.currentTimeMillis())

        val batchesArray = JSONArray()
        batches.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("batchCode", b.batchCode)
                put("batchDate", b.batchDate)
                put("description", b.description)
                put("status", b.status)
                put("createdAt", b.createdAt)
                put("updatedAt", b.updatedAt)
            }
            batchesArray.put(obj)
        }
        rootObj.put("batches", batchesArray)

        val parcelsArray = JSONArray()
        parcels.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("batchId", p.batchId)
                put("barcode", p.barcode)
                put("destinationPincode", p.destinationPincode)
                put("addresseeName", p.addresseeName)
                put("physicalWeight", p.physicalWeight)
                put("length", p.length)
                put("breadth", p.breadth)
                put("height", p.height)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            }
            parcelsArray.put(obj)
        }
        rootObj.put("parcels", parcelsArray)

        val exportArray = JSONArray()
        exportHistories.forEach { e ->
            val obj = JSONObject().apply {
                put("id", e.id)
                put("batchId", e.batchId)
                put("fileName", e.fileName)
                put("exportDate", e.exportDate)
                put("exportedRecordCount", e.exportedRecordCount)
            }
            exportArray.put(obj)
        }
        rootObj.put("exportHistories", exportArray)

        outputStream.write(rootObj.toString(2).toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    fun parseAndValidateBackupJson(inputStream: InputStream): BackupData {
        val jsonStr = inputStream.bufferedReader().use { it.readText() }
        val rootObj = JSONObject(jsonStr)

        if (!rootObj.has("batches") || !rootObj.has("parcels")) {
            throw IllegalArgumentException("Invalid backup file format: missing required data collections.")
        }

        val version = rootObj.optInt("version", 1)
        val exportedAt = rootObj.optLong("exportedAt", System.currentTimeMillis())

        val batchesList = mutableListOf<BatchEntity>()
        val batchesArray = rootObj.getJSONArray("batches")
        for (i in 0 until batchesArray.length()) {
            val obj = batchesArray.getJSONObject(i)
            batchesList.add(
                BatchEntity(
                    id = obj.getLong("id"),
                    batchCode = obj.getString("batchCode"),
                    batchDate = obj.getString("batchDate"),
                    description = obj.optString("description", ""),
                    status = obj.optString("status", BatchEntity.STATUS_ACTIVE),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val parcelsList = mutableListOf<ParcelEntity>()
        val parcelsArray = rootObj.getJSONArray("parcels")
        for (i in 0 until parcelsArray.length()) {
            val obj = parcelsArray.getJSONObject(i)
            parcelsList.add(
                ParcelEntity(
                    id = obj.getLong("id"),
                    batchId = obj.getLong("batchId"),
                    barcode = obj.getString("barcode"),
                    destinationPincode = obj.getString("destinationPincode"),
                    addresseeName = obj.optString("addresseeName", ""),
                    physicalWeight = obj.getDouble("physicalWeight"),
                    length = obj.getDouble("length"),
                    breadth = obj.getDouble("breadth"),
                    height = obj.getDouble("height"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val exportList = mutableListOf<ExportHistoryEntity>()
        if (rootObj.has("exportHistories")) {
            val exportArray = rootObj.getJSONArray("exportHistories")
            for (i in 0 until exportArray.length()) {
                val obj = exportArray.getJSONObject(i)
                exportList.add(
                    ExportHistoryEntity(
                        id = obj.getLong("id"),
                        batchId = obj.getLong("batchId"),
                        fileName = obj.getString("fileName"),
                        exportDate = obj.optLong("exportDate", System.currentTimeMillis()),
                        exportedRecordCount = obj.optInt("exportedRecordCount", 0)
                    )
                )
            }
        }

        return BackupData(
            version = version,
            exportedAt = exportedAt,
            batches = batchesList,
            parcels = parcelsList,
            exportHistories = exportList
        )
    }
}
