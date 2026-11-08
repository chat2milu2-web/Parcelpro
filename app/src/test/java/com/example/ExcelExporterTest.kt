package com.example

import com.example.data.entities.BatchEntity
import com.example.data.entities.ParcelEntity
import com.example.data.models.ExcelTemplateConfig
import com.example.excel.ExcelExporter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ExcelExporterTest {

    @Test
    fun testExportBatchToExcelGeneratesValidData() {
        val batch = BatchEntity(
            id = 1L,
            batchCode = "YYB-20261106-001",
            batchDate = "2026-11-06",
            description = "Test Batch",
            status = "Active"
        )
        val parcels = listOf(
            ParcelEntity(
                id = 1L,
                batchId = 1L,
                sequenceNumber = 1,
                barcode = "IN123456789",
                destinationPincode = "700001",
                addresseeName = "JOHN DOE",
                physicalWeight = 250.0,
                length = 15.0,
                breadth = 10.0,
                height = 5.0
            )
        )
        val template = ExcelTemplateConfig()
        val outputStream = ByteArrayOutputStream()

        ExcelExporter.exportBatchToExcel(
            batch = batch,
            parcels = parcels,
            templateConfig = template,
            outputStream = outputStream
        )

        val bytes = outputStream.toByteArray()
        println("Generated Excel bytes count: ${bytes.size}")
        assertTrue("Excel file size should be greater than 1000 bytes", bytes.size > 1000)
    }
}
