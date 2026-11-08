package com.example.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.abs

enum class LabelFilterMode(val displayName: String) {
    ORIGINAL("Original"),
    BW_HIGH_CONTRAST("B&W Thermal"),
    THERMAL_SHARP("Thermal Sharp"),
    GRAYSCALE("Grayscale")
}

object ImageEnhancer {

    /**
     * Applies document label image enhancement filter to [bitmap].
     */
    fun applyFilter(bitmap: Bitmap, mode: LabelFilterMode): Bitmap {
        return when (mode) {
            LabelFilterMode.ORIGINAL -> bitmap
            LabelFilterMode.GRAYSCALE -> toGrayscale(bitmap)
            LabelFilterMode.BW_HIGH_CONTRAST -> toBwHighContrast(bitmap)
            LabelFilterMode.THERMAL_SHARP -> toThermalSharp(bitmap)
        }
    }

    private fun toGrayscale(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun toBwHighContrast(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // ColorMatrix for extreme contrast & brightness adjustment (Thermal B&W binarization)
        val matrix = ColorMatrix(
            floatArrayOf(
                2.2f, 0f, 0f, 0f, -110f,
                0f, 2.2f, 0f, 0f, -110f,
                0f, 0f, 2.2f, 0f, -110f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun toThermalSharp(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // ColorMatrix for thermal label edge contrast
        val matrix = ColorMatrix(
            floatArrayOf(
                1.5f, 0f, 0f, 0f, -50f,
                0f, 1.5f, 0f, 0f, -50f,
                0f, 0f, 1.5f, 0f, -50f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }
}
