package com.example.scanner

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object BoundaryDetector {

    /**
     * Detects document/address-label quadrilateral boundaries using luminance gradient edge analysis.
     * Runs asynchronously on a background thread. Returns high-contrast bounding quadrilateral or default inset.
     */
    suspend fun detectBoundary(bitmap: Bitmap): CropQuadrilateral = withContext(Dispatchers.Default) {
        val imgWidth = bitmap.width.toFloat()
        val imgHeight = bitmap.height.toFloat()

        try {
            val maxDim = 400
            val scale = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            } else 1.0f

            val smallW = (bitmap.width * scale).toInt().coerceAtLeast(40)
            val smallH = (bitmap.height * scale).toInt().coerceAtLeast(40)

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, smallW, smallH, false)
            val pixels = IntArray(smallW * smallH)
            scaledBitmap.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            // Grayscale + Sobel Magnitude
            val grad = FloatArray(smallW * smallH)
            var sumGrad = 0.0f

            for (y in 1 until smallH - 1) {
                for (x in 1 until smallW - 1) {
                    val idx = y * smallW + x

                    fun luma(c: Int): Float {
                        val r = (c shr 16) and 0xFF
                        val g = (c shr 8) and 0xFF
                        val b = c and 0xFF
                        return 0.299f * r + 0.587f * g + 0.114f * b
                    }

                    val gx = abs(luma(pixels[idx + 1]) - luma(pixels[idx - 1]))
                    val gy = abs(luma(pixels[idx + smallW]) - luma(pixels[idx - smallW]))
                    val mag = gx + gy
                    grad[idx] = mag
                    sumGrad += mag
                }
            }

            val avgGrad = sumGrad / (smallW * smallH)
            val threshold = avgGrad * 1.25f

            // Find corner points among edge candidates
            var minSum = Float.MAX_VALUE
            var maxSum = -Float.MAX_VALUE
            var minDiff = Float.MAX_VALUE
            var maxDiff = -Float.MAX_VALUE

            var tl = CropPoint(smallW * 0.08f, smallH * 0.12f)
            var tr = CropPoint(smallW * 0.92f, smallH * 0.12f)
            var br = CropPoint(smallW * 0.92f, smallH * 0.88f)
            var bl = CropPoint(smallW * 0.08f, smallH * 0.88f)

            var foundEdges = 0

            for (y in (smallH * 0.04).toInt() until (smallH * 0.96).toInt()) {
                for (x in (smallW * 0.04).toInt() until (smallW * 0.96).toInt()) {
                    val idx = y * smallW + x
                    if (grad[idx] > threshold) {
                        foundEdges++
                        val sum = (x + y).toFloat()
                        val diff = (x - y).toFloat()

                        if (sum < minSum) { minSum = sum; tl = CropPoint(x.toFloat(), y.toFloat()) }
                        if (sum > maxSum) { maxSum = sum; br = CropPoint(x.toFloat(), y.toFloat()) }
                        if (diff > maxDiff) { maxDiff = diff; tr = CropPoint(x.toFloat(), y.toFloat()) }
                        if (diff < minDiff) { minDiff = diff; bl = CropPoint(x.toFloat(), y.toFloat()) }
                    }
                }
            }

            val invScale = 1.0f / scale
            val candidate = if (foundEdges > 30) {
                CropQuadrilateral(
                    topLeft = CropPoint(tl.x * invScale, tl.y * invScale),
                    topRight = CropPoint(tr.x * invScale, tr.y * invScale),
                    bottomRight = CropPoint(br.x * invScale, br.y * invScale),
                    bottomLeft = CropPoint(bl.x * invScale, bl.y * invScale)
                ).clampedToBounds(imgWidth, imgHeight)
            } else {
                CropQuadrilateral.createDefaultInset(imgWidth, imgHeight, insetPct = 0.06f)
            }

            if (candidate.isValidConvex()) {
                candidate
            } else {
                CropQuadrilateral.createDefaultInset(imgWidth, imgHeight, insetPct = 0.06f)
            }
        } catch (e: Exception) {
            CropQuadrilateral.createDefaultInset(imgWidth, imgHeight, insetPct = 0.06f)
        }
    }
}
