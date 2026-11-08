package com.example.data.models

import java.util.UUID

data class DimensionPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val length: Double,
    val breadth: Double,
    val height: Double
) {
    fun formatDisplay(unit: String = "cm"): String {
        return "$name (${length.toInt()} × ${breadth.toInt()} × ${height.toInt()} $unit)"
    }

    companion object {
        fun defaultPresets(): List<DimensionPreset> {
            return listOf(
                DimensionPreset(id = "default_env", name = "Document Envelope", length = 25.0, breadth = 18.0, height = 1.0),
                DimensionPreset(id = "default_small", name = "Small Parcel Box", length = 20.0, breadth = 15.0, height = 10.0),
                DimensionPreset(id = "default_medium", name = "Medium Carton Box", length = 30.0, breadth = 20.0, height = 15.0),
                DimensionPreset(id = "default_large", name = "Large Heavy Box", length = 45.0, breadth = 35.0, height = 25.0),
                DimensionPreset(id = "default_shoe", name = "Shoe Box", length = 32.0, breadth = 20.0, height = 12.0)
            )
        }
    }
}
