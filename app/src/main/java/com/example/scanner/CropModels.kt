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

    /**
     * Moves one of the 4 edges (TOP_EDGE, BOTTOM_EDGE, LEFT_EDGE, RIGHT_EDGE) by (deltaX, deltaY)
     * in image pixel coordinates while keeping the quadrilateral within bounds and convex.
     */
    fun moveEdge(
        handle: ActiveHandle,
        deltaX: Float,
        deltaY: Float,
        imageWidth: Float,
        imageHeight: Float
    ): CropQuadrilateral {
        val minGap = 32f
        val candidate = when (handle) {
            ActiveHandle.TOP_EDGE -> {
                val minDy = -min(topLeft.y, topRight.y)
                val maxDyBeforeBottom = min(bottomLeft.y - topLeft.y, bottomRight.y - topRight.y) - minGap
                val maxDyBounds = imageHeight - max(topLeft.y, topRight.y)
                val maxDy = min(maxDyBounds, maxDyBeforeBottom).coerceAtLeast(minDy)
                val clampedDy = deltaY.coerceIn(minDy, maxDy)
                copy(
                    topLeft = CropPoint(topLeft.x, (topLeft.y + clampedDy).coerceIn(0f, imageHeight)),
                    topRight = CropPoint(topRight.x, (topRight.y + clampedDy).coerceIn(0f, imageHeight))
                )
            }
            ActiveHandle.BOTTOM_EDGE -> {
                val minDyAfterTop = max(topLeft.y - bottomLeft.y, topRight.y - bottomRight.y) + minGap
                val minDyBounds = -min(bottomLeft.y, bottomRight.y)
                val maxDy = imageHeight - max(bottomLeft.y, bottomRight.y)
                val minDy = max(minDyBounds, minDyAfterTop).coerceAtMost(maxDy)
                val clampedDy = deltaY.coerceIn(minDy, maxDy)
                copy(
                    bottomLeft = CropPoint(bottomLeft.x, (bottomLeft.y + clampedDy).coerceIn(0f, imageHeight)),
                    bottomRight = CropPoint(bottomRight.x, (bottomRight.y + clampedDy).coerceIn(0f, imageHeight))
                )
            }
            ActiveHandle.LEFT_EDGE -> {
                val minDx = -min(topLeft.x, bottomLeft.x)
                val maxDxBeforeRight = min(topRight.x - topLeft.x, bottomRight.x - bottomLeft.x) - minGap
                val maxDxBounds = imageWidth - max(topLeft.x, bottomLeft.x)
                val maxDx = min(maxDxBounds, maxDxBeforeRight).coerceAtLeast(minDx)
                val clampedDx = deltaX.coerceIn(minDx, maxDx)
                copy(
                    topLeft = CropPoint((topLeft.x + clampedDx).coerceIn(0f, imageWidth), topLeft.y),
                    bottomLeft = CropPoint((bottomLeft.x + clampedDx).coerceIn(0f, imageWidth), bottomLeft.y)
                )
            }
            ActiveHandle.RIGHT_EDGE -> {
                val minDxAfterLeft = max(topLeft.x - topRight.x, bottomLeft.x - bottomRight.x) + minGap
                val minDxBounds = -min(topRight.x, bottomRight.x)
                val maxDx = imageWidth - max(topRight.x, bottomRight.x)
                val minDx = max(minDxBounds, minDxAfterLeft).coerceAtMost(maxDx)
                val clampedDx = deltaX.coerceIn(minDx, maxDx)
                copy(
                    topRight = CropPoint((topRight.x + clampedDx).coerceIn(0f, imageWidth), topRight.y),
                    bottomRight = CropPoint((bottomRight.x + clampedDx).coerceIn(0f, imageWidth), bottomRight.y)
                )
            }
            else -> this
        }
        return if (candidate.isValidConvex()) candidate else this
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
    TOP_EDGE,
    RIGHT_EDGE,
    BOTTOM_EDGE,
    LEFT_EDGE,
    CENTER_QUAD
}
