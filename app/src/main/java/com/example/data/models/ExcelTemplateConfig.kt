package com.example.data.models

data class ExcelColumnConfig(
    val key: String, // e.g. "barcode", "pincode", "addressee", "weight", "length", "breadth", "height"
    val defaultHeader: String,
    val customHeader: String,
    val enabled: Boolean = true,
    val order: Int
)

data class ExcelTemplateConfig(
    val columns: List<ExcelColumnConfig> = defaultConfig()
) {
    companion object {
        fun defaultConfig(): List<ExcelColumnConfig> {
            return listOf(
                ExcelColumnConfig("barcode", "Barcode", "Barcode", true, 0),
                ExcelColumnConfig("pincode", "Destination Pincode", "Destination Pincode", true, 1),
                ExcelColumnConfig("addressee", "Name of Addressee", "Name of Addressee", true, 2),
                ExcelColumnConfig("weight", "Physical Weight", "Physical Weight (g)", true, 3),
                ExcelColumnConfig("length", "Length", "Length (cm)", true, 4),
                ExcelColumnConfig("breadth", "Breadth", "Breadth (cm)", true, 5),
                ExcelColumnConfig("height", "Height", "Height (cm)", true, 6)
            )
        }
    }
}
