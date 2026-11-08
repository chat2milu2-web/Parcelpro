package com.example

import com.example.scanner.ActiveHandle
import com.example.scanner.CropPoint
import com.example.scanner.CropQuadrilateral
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropModelsTest {

    private val imgWidth = 1000f
    private val imgHeight = 800f

    private fun initialQuad() = CropQuadrilateral(
        topLeft = CropPoint(100f, 100f),
        topRight = CropPoint(900f, 100f),
        bottomRight = CropPoint(900f, 700f),
        bottomLeft = CropPoint(100f, 700f)
    )

    @Test
    fun moveTopEdge_movesTopCornersVerticallyAndPreservesHorizontalX() {
        val movedDown = initialQuad().moveEdge(
            handle = ActiveHandle.TOP_EDGE,
            deltaX = 50f,
            deltaY = 120f,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )

        assertEquals(100f, movedDown.topLeft.x, 0.01f)
        assertEquals(900f, movedDown.topRight.x, 0.01f)
        assertEquals(220f, movedDown.topLeft.y, 0.01f)
        assertEquals(220f, movedDown.topRight.y, 0.01f)
        assertEquals(700f, movedDown.bottomLeft.y, 0.01f)
        assertEquals(700f, movedDown.bottomRight.y, 0.01f)
        assertTrue(movedDown.isValidConvex())
    }

    @Test
    fun moveBottomEdge_movesBottomCornersVerticallyAndPreservesHorizontalX() {
        val movedUp = initialQuad().moveEdge(
            handle = ActiveHandle.BOTTOM_EDGE,
            deltaX = -30f,
            deltaY = -150f,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )

        assertEquals(100f, movedUp.bottomLeft.x, 0.01f)
        assertEquals(900f, movedUp.bottomRight.x, 0.01f)
        assertEquals(550f, movedUp.bottomLeft.y, 0.01f)
        assertEquals(550f, movedUp.bottomRight.y, 0.01f)
        assertEquals(100f, movedUp.topLeft.y, 0.01f)
        assertEquals(100f, movedUp.topRight.y, 0.01f)
        assertTrue(movedUp.isValidConvex())
    }

    @Test
    fun moveLeftEdge_movesLeftCornersHorizontallyAndPreservesVerticalY() {
        val movedRight = initialQuad().moveEdge(
            handle = ActiveHandle.LEFT_EDGE,
            deltaX = 180f,
            deltaY = 40f,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )

        assertEquals(280f, movedRight.topLeft.x, 0.01f)
        assertEquals(280f, movedRight.bottomLeft.x, 0.01f)
        assertEquals(100f, movedRight.topLeft.y, 0.01f)
        assertEquals(700f, movedRight.bottomLeft.y, 0.01f)
        assertEquals(900f, movedRight.topRight.x, 0.01f)
        assertEquals(900f, movedRight.bottomRight.x, 0.01f)
        assertTrue(movedRight.isValidConvex())
    }

    @Test
    fun moveRightEdge_movesRightCornersHorizontallyAndPreservesVerticalY() {
        val movedLeft = initialQuad().moveEdge(
            handle = ActiveHandle.RIGHT_EDGE,
            deltaX = -200f,
            deltaY = -20f,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )

        assertEquals(700f, movedLeft.topRight.x, 0.01f)
        assertEquals(700f, movedLeft.bottomRight.x, 0.01f)
        assertEquals(100f, movedLeft.topRight.y, 0.01f)
        assertEquals(700f, movedLeft.bottomRight.y, 0.01f)
        assertEquals(100f, movedLeft.topLeft.x, 0.01f)
        assertEquals(100f, movedLeft.bottomLeft.x, 0.01f)
        assertTrue(movedLeft.isValidConvex())
    }

    @Test
    fun moveEdge_clampsToImageBoundsAndMinimumQuadrilateralGap() {
        val clampedTop = initialQuad().moveEdge(
            handle = ActiveHandle.TOP_EDGE,
            deltaX = 0f,
            deltaY = 2000f,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )
        assertTrue(clampedTop.topLeft.y <= clampedTop.bottomLeft.y - 32f)
        assertTrue(clampedTop.isValidConvex())

        val clampedLeft = initialQuad().moveEdge(
            handle = ActiveHandle.LEFT_EDGE,
            deltaX = -2000f,
            deltaY = 0f,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )
        assertEquals(0f, clampedLeft.topLeft.x, 0.01f)
        assertEquals(0f, clampedLeft.bottomLeft.x, 0.01f)
        assertTrue(clampedLeft.isValidConvex())
    }
}
