package com.example.excel

import com.example.data.entities.BatchEntity
import com.example.data.entities.ParcelEntity
import com.example.data.models.ExcelTemplateConfig
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

object ExcelExporter {

    fun exportBatchToExcel(
        batch: BatchEntity,
        parcels: List<ParcelEntity>,
        templateConfig: ExcelTemplateConfig,
        outputStream: OutputStream
    ) {
        val workbook = XSSFWorkbook()
        val sheetName = batch.batchCode.replace("[^a-zA-Z0-9]".toRegex(), "_").take(31)
        val sheet = workbook.createSheet(sheetName.ifEmpty { "Parcels" })

        // Styles
        val headerFont = workbook.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
            fontHeightInPoints = 11.toShort()
            fontName = "Calibri"
        }

        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.ROYAL_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val textCellStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.CENTER
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val numericCellStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTER
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            dataFormat = workbook.createDataFormat().getFormat("0.00")
        }

        val enabledColumns = templateConfig.columns
            .filter { it.enabled }
            .sortedBy { it.order }

        // Create Header Row (Row 0)
        val headerRow = sheet.createRow(0)
        headerRow.heightInPoints = 24f

        enabledColumns.forEachIndexed { colIdx, colConfig ->
            val cell = headerRow.createCell(colIdx)
            cell.setCellValue(colConfig.customHeader)
            cell.cellStyle = headerStyle
        }

        // Create Data Rows (Row 1..N)
        parcels.forEachIndexed { rowIdx, parcel ->
            val row = sheet.createRow(rowIdx + 1)
            row.heightInPoints = 20f

            enabledColumns.forEachIndexed { colIdx, colConfig ->
                val cell = row.createCell(colIdx)

                when (colConfig.key) {
                    "barcode" -> {
                        cell.setCellType(CellType.STRING)
                        cell.setCellValue(parcel.barcode)
                        cell.cellStyle = textCellStyle
                    }
                    "pincode" -> {
                        cell.setCellType(CellType.STRING)
                        cell.setCellValue(parcel.destinationPincode)
                        cell.cellStyle = textCellStyle
                    }
                    "addressee" -> {
                        cell.setCellType(CellType.STRING)
                        cell.setCellValue(parcel.addresseeName)
                        cell.cellStyle = textCellStyle
                    }
                    "weight" -> {
                        cell.setCellType(CellType.NUMERIC)
                        cell.setCellValue(parcel.physicalWeight)
                        cell.cellStyle = numericCellStyle
                    }
                    "length" -> {
                        cell.setCellType(CellType.NUMERIC)
                        cell.setCellValue(parcel.length)
                        cell.cellStyle = numericCellStyle
                    }
                    "breadth" -> {
                        cell.setCellType(CellType.NUMERIC)
                        cell.setCellValue(parcel.breadth)
                        cell.cellStyle = numericCellStyle
                    }
                    "height" -> {
                        cell.setCellType(CellType.NUMERIC)
                        cell.setCellValue(parcel.height)
                        cell.cellStyle = numericCellStyle
                    }
                    else -> {
                        cell.setCellType(CellType.STRING)
                        cell.setCellValue("")
                        cell.cellStyle = textCellStyle
                    }
                }
            }
        }

        // Set column widths based on maximum character length without calling AWT-dependent autoSizeColumn
        enabledColumns.forEachIndexed { colIdx, colConfig ->
            var maxLen = colConfig.customHeader.length
            parcels.forEach { p ->
                val valLen = when (colConfig.key) {
                    "barcode" -> p.barcode.length
                    "pincode" -> p.destinationPincode.length
                    "addressee" -> p.addresseeName.length
                    "weight" -> String.format(java.util.Locale.US, "%.2f", p.physicalWeight).length
                    "length" -> String.format(java.util.Locale.US, "%.2f", p.length).length
                    "breadth" -> String.format(java.util.Locale.US, "%.2f", p.breadth).length
                    "height" -> String.format(java.util.Locale.US, "%.2f", p.height).length
                    else -> 0
                }
                if (valLen > maxLen) maxLen = valLen
            }
            // Add padding of 4 characters and constrain between 12 and 50 characters (width in units of 1/256)
            val charWidth = (maxLen + 4).coerceIn(12, 50)
            sheet.setColumnWidth(colIdx, charWidth * 256)
        }

        workbook.write(outputStream)
        outputStream.flush()
        workbook.close()
    }
}
