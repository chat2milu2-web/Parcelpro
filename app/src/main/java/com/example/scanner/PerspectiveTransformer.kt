package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max

object PerspectiveTransformer {

    /**
     * Performs a 4-point perspective transformation on [srcBitmap] mapping [quad] to a flat rectangular bitmap.
     * Preserves high resolution and text clarity.
     */
    suspend fun perspectiveCropAndWarp(
        srcBitmap: Bitmap,
        quad: CropQuadrilateral
    ): Bitmap = withContext(Dispatchers.Default) {
        val srcPts = floatArrayOf(
            quad.topLeft.x, quad.topLeft.y,
            quad.topRight.x, quad.topRight.y,
            quad.bottomRight.x, quad.bottomRight.y,
            quad.bottomLeft.x, quad.bottomLeft.y
        )

        val topW = hypot((quad.topRight.x - quad.topLeft.x).toDouble(), (quad.topRight.y - quad.topLeft.y).toDouble())
        val bottomW = hypot((quad.bottomRight.x - quad.bottomLeft.x).toDouble(), (quad.bottomRight.y - quad.bottomLeft.y).toDouble())
        val targetWidth = max(topW, bottomW).toInt().coerceAtLeast(150)

        val leftH = hypot((quad.bottomLeft.x - quad.topLeft.x).toDouble(), (quad.bottomLeft.y - quad.topLeft.y).toDouble())
        val rightH = hypot((quad.bottomRight.x - quad.topRight.x).toDouble(), (quad.bottomRight.y - quad.topRight.y).toDouble())
        val targetHeight = max(leftH, rightH).toInt().coerceAtLeast(150)

        val dstPts = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)

        val outputBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        if (success) {
            canvas.drawBitmap(srcBitmap, matrix, paint)
        } else {
            // Fallback bounding box crop if setPolyToPoly fails
            val minX = minOf(quad.topLeft.x, quad.topRight.x, quad.bottomRight.x, quad.bottomLeft.x).toInt().coerceIn(0, srcBitmap.width - 2)
            val minY = minOf(quad.topLeft.y, quad.topRight.y, quad.bottomRight.y, quad.bottomLeft.y).toInt().coerceIn(0, srcBitmap.height - 2)
            val maxX = maxOf(quad.topLeft.x, quad.topRight.x, quad.bottomRight.x, quad.bottomLeft.x).toInt().coerceIn(minX + 2, srcBitmap.width)
            val maxY = maxOf(quad.topLeft.y, quad.topRight.y, quad.bottomRight.y, quad.bottomLeft.y).toInt().coerceIn(minY + 2, srcBitmap.height)
            return@withContext Bitmap.createBitmap(srcBitmap, minX, minY, maxX - minX, maxY - minY)
        }

        outputBitmap
    }

    /**
     * Rotates bitmap by 90 degrees clockwise.
     */
    fun rotate90(srcBitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, true)
    }

    /**
     * Saves bitmap to cache file directory and returns a content Uri.
     */
    suspend fun saveToCache(context: Context, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "crop_ocr_${System.currentTimeMillis()}.jpg")
        FileOutputStream(cacheFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }
        val authority = "${context.packageName}.fileprovider"
        FileProvider.getUriForFile(context, authority, cacheFile)
    }
}
