package com.example.scanner

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Point in ORIGINAL IMAGE pixel coordinates [0..imageWidth, 0..imageHeight].
 */
data class CropPoint(val x: Float, val y: Float) {
    fun toOffset(): Offset = Offset(x, y)
    
    fun distanceTo(other: CropPoint): Float {
        return hypot((x - other.x).toDouble(), (y - other.y).toDouble()).toFloat()
    }
}

/**
 * 4-Corner Quadrilateral representing the document boundary in image pixel coordinates.
 * Coordinates are always ordered: TL (Top-Left), TR (Top-Right), BR (Bottom-Right), BL (Bottom-Left).
 */
data class CropQuadrilateral(
    val topLeft: CropPoint,
    val topRight: CropPoint,
    val bottomRight: CropPoint,
    val bottomLeft: CropPoint
) {
    fun toFloatArray(): FloatArray {
        return floatArrayOf(
            topLeft.x, topLeft.y,
            topRight.x, topRight.y,
            bottomRight.x, bottomRight.y,
            bottomLeft.x, bottomLeft.y
        )
    }

    /**
     * Clamps all 4 points within the image dimensions [0..width, 0..height].
     */
    fun clampedToBounds(imageWidth: Float, imageHeight: Float): CropQuadrilateral {
        return CropQuadrilateral(
            topLeft = CropPoint(topLeft.x.coerceIn(0f, imageWidth), topLeft.y.coerceIn(0f, imageHeight)),
            topRight = CropPoint(topRight.x.coerceIn(0f, imageWidth), topRight.y.coerceIn(0f, imageHeight)),
            bottomRight = CropPoint(bottomRight.x.coerceIn(0f, imageWidth), bottomRight.y.coerceIn(0f, imageHeight)),
            bottomLeft = CropPoint(bottomLeft.x.coerceIn(0f, imageWidth), bottomLeft.y.coerceIn(0f, imageHeight))
        )
    }

    /**
     * Validates that the quadrilateral is convex and does not self-intersect.
     */
    fun isValidConvex(): Boolean {
        // Cross products of consecutive edges must all have the same sign (positive for counter-clockwise / negative for clockwise)
        val p0 = topLeft
        val p1 = topRight
        val p2 = bottomRight
        val p3 = bottomLeft

        fun crossProduct(a: CropPoint, b: CropPoint, c: CropPoint): Float {
            return (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
        }

        val cp1 = crossProduct(p0, p1, p2)
        val cp2 = crossProduct(p1, p2, p3)
        val cp3 = crossProduct(p2, p3, p0)
        val cp4 = crossProduct(p3, p0, p1)

        val allPositive = cp1 > 0 && cp2 > 0 && cp3 > 0 && cp4 > 0
        val allNegative = cp1 < 0 && cp2 < 0 && cp3 < 0 && cp4 < 0

        // Minimum side length check
        val minSide = 30f
        val sidesOk = p0.distanceTo(p1) > minSide &&
                p1.distanceTo(p2) > minSide &&
                p2.distanceTo(p3) > minSide &&
                p3.distanceTo(p0) > minSide

        return (allPositive || allNegative) && sidesOk
    }

    /**
     * Checks if a given point in image coordinates is inside the quadrilateral.
     */
    fun containsPoint(p: CropPoint): Boolean {
        val pts = listOf(topLeft, topRight, bottomRight, bottomLeft)
        var inside = false
        var j = pts.size - 1
        for (i in pts.indices) {
            val pi = pts[i]
            val pj = pts[j]
            if ((pi.y > p.y) != (pj.y > p.y) &&
                (p.x < (pj.x - pi.x) * (p.y - pi.y) / (pj.y - pi.y + 0.0001f) + pi.x)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    companion object {
        /**
         * Creates a default inset quadrilateral spanning [insetPct] from the edges of the image.
         */
        fun createDefaultInset(imageWidth: Float, imageHeight: Float, insetPct: Float = 0.06f): CropQuadrilateral {
            val insetX = imageWidth * insetPct
            val insetY = imageHeight * insetPct
            return CropQuadrilateral(
                topLeft = CropPoint(insetX, insetY),
                topRight = CropPoint(imageWidth - insetX, insetY),
                bottomRight = CropPoint(imageWidth - insetX, imageHeight - insetY),
                bottomLeft = CropPoint(insetX, imageHeight - insetY)
            )
        }
    }
}

/**
 * Handle selection target for gestures.
 */
enum class ActiveHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    CENTER_QUAD
}
