package com.example.excel

import com.example.data.entities.BatchEntity
import com.example.data.entities.ParcelEntity
import com.example.data.models.ExcelTemplateConfig

object TextAndCsvExporter {

    fun generateCsv(
        parcels: List<ParcelEntity>,
        templateConfig: ExcelTemplateConfig
    ): String {
        val enabledColumns = templateConfig.columns
            .filter { it.enabled }
            .sortedBy { it.order }

        val sb = StringBuilder()

        // CSV Header
        val headerRow = enabledColumns.joinToString(",") { escapeCsv(it.customHeader) }
        sb.append(headerRow).append("\r\n")

        // CSV Data Rows
        parcels.forEach { parcel ->
            val rowValues = enabledColumns.map { col ->
                val rawVal = when (col.key) {
                    "barcode" -> parcel.barcode
                    "pincode" -> parcel.destinationPincode
                    "addressee" -> parcel.addresseeName
                    "weight" -> if (parcel.physicalWeight > 0.0) parcel.physicalWeight.toString() else "0"
                    "length" -> if (parcel.length > 0.0) parcel.length.toString() else "0"
                    "breadth" -> if (parcel.breadth > 0.0) parcel.breadth.toString() else "0"
                    "height" -> if (parcel.height > 0.0) parcel.height.toString() else "0"
                    else -> ""
                }
                escapeCsv(rawVal)
            }
            sb.append(rowValues.joinToString(",")).append("\r\n")
        }

        return sb.toString()
    }

    fun generateFormattedText(
        batch: BatchEntity,
        parcels: List<ParcelEntity>,
        templateConfig: ExcelTemplateConfig
    ): String {
        val enabledColumns = templateConfig.columns
            .filter { it.enabled }
            .sortedBy { it.order }

        val sb = StringBuilder()
        sb.append("======================================================================\r\n")
        sb.append("PARCEL ENTRY PRO - BATCH EXPORT SUMMARY\r\n")
        sb.append("======================================================================\r\n")
        sb.append("Batch ID    : ${batch.batchCode}\r\n")
        sb.append("Batch Date  : ${batch.batchDate}\r\n")
        sb.append("Description : ${batch.description}\r\n")
        sb.append("Total Items : ${parcels.size} parcels\r\n")
        sb.append("Export Time : ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\r\n")
        sb.append("======================================================================\r\n\r\n")

        // Tab delimited data
        val header = enabledColumns.joinToString("\t") { it.customHeader }
        sb.append(header).append("\r\n")
        sb.append("-".repeat(70)).append("\r\n")

        parcels.forEachIndexed { index, parcel ->
            val row = enabledColumns.joinToString("\t") { col ->
                when (col.key) {
                    "barcode" -> parcel.barcode
                    "pincode" -> parcel.destinationPincode
                    "addressee" -> parcel.addresseeName
                    "weight" -> parcel.physicalWeight.toString()
                    "length" -> parcel.length.toString()
                    "breadth" -> parcel.breadth.toString()
                    "height" -> parcel.height.toString()
                    else -> ""
                }
            }
            sb.append(row).append("\r\n")
        }

        sb.append("\r\n======================================================================\r\n")
        sb.append("End of Batch Export (${parcels.size} records)\r\n")
        sb.append("======================================================================\r\n")

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var str = value
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }
}
