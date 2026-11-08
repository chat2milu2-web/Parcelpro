package com.example.scanner

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint as NativePaint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropEditorScreen(
    imageBitmap: Bitmap,
    onRetake: () -> Unit,
    onRotate: () -> Unit,
    onCancel: () -> Unit,
    onConfirmAndScan: (Bitmap) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val imgW = imageBitmap.width.toFloat()
    val imgH = imageBitmap.height.toFloat()

    // 4-Corner Quadrilateral state in ORIGINAL IMAGE PIXEL space
    var quad by remember(imageBitmap) {
        mutableStateOf(CropQuadrilateral.createDefaultInset(imgW, imgH, insetPct = 0.06f))
    }

    var isAutoDetecting by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(LabelFilterMode.ORIGINAL) }

    // Auto-detect boundary on initial image load
    LaunchedEffect(imageBitmap) {
        isAutoDetecting = true
        val detected = BoundaryDetector.detectBoundary(imageBitmap)
        quad = detected
        isAutoDetecting = false
    }

    // Zoom and pan state
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Active drag handle & Loupe state
    var activeHandle by remember { mutableStateOf<ActiveHandle?>(null) }
    var loupePointInImage by remember { mutableStateOf<CropPoint?>(null) }

    val currentQuadState = rememberUpdatedState(quad)

    BackHandler {
        onRetake()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Crop Address Label",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("crop_editor_close_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            zoomScale = 1.0f
                            panOffsetX = 0f
                            panOffsetY = 0f
                            onRotate()
                        },
                        modifier = Modifier.testTag("crop_editor_rotate_top_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CropRotate, contentDescription = "Rotate 90°", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0A0A0A))
        ) {
            // Interactive Document Scanner Canvas with Loupe Lens Overlay
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
            ) {
                val containerW = constraints.maxWidth.toFloat()
                val containerH = constraints.maxHeight.toFloat()

                // Calculate image-to-screen matrix & transformation values
                val bmpRatio = imgW / imgH
                val containerRatio = containerW / containerH

                val dispW: Float
                val dispH: Float
                if (bmpRatio > containerRatio) {
                    dispW = containerW
                    dispH = containerW / bmpRatio
                } else {
                    dispH = containerH
                    dispW = containerH * bmpRatio
                }

                val baseImgLeft = (containerW - dispW) / 2f
                val baseImgTop = (containerH - dispH) / 2f

                // Matrix mapping Image Coordinates [0..imgW, 0..imgH] -> Screen Canvas Coordinates
                val imageToScreenMatrix = remember(dispW, dispH, baseImgLeft, baseImgTop, zoomScale, panOffsetX, panOffsetY) {
                    Matrix().apply {
                        postScale(dispW / imgW, dispH / imgH)
                        postTranslate(baseImgLeft, baseImgTop)
                        if (zoomScale != 1.0f || panOffsetX != 0f || panOffsetY != 0f) {
                            postScale(zoomScale, zoomScale, containerW / 2f, containerH / 2f)
                            postTranslate(panOffsetX, panOffsetY)
                        }
                    }
                }

                // Inverse matrix mapping Screen Canvas Coordinates -> Image Coordinates [0..imgW, 0..imgH]
                val screenToImageMatrix = remember(imageToScreenMatrix) {
                    Matrix().apply {
                        imageToScreenMatrix.invert(this)
                    }
                }

                val currentScreenToImgMatrixState = rememberUpdatedState(screenToImageMatrix)
                val currentImgToScreenMatrixState = rememberUpdatedState(imageToScreenMatrix)

                fun screenFromImage(pt: CropPoint): Offset {
                    val pts = floatArrayOf(pt.x, pt.y)
                    imageToScreenMatrix.mapPoints(pts)
                    return Offset(pts[0], pts[1])
                }

                fun imageFromScreen(screenOffset: Offset, invMatrix: Matrix): CropPoint {
                    val pts = floatArrayOf(screenOffset.x, screenOffset.y)
                    invMatrix.mapPoints(pts)
                    return CropPoint(
                        x = pts[0].coerceIn(0f, imgW),
                        y = pts[1].coerceIn(0f, imgH)
                    )
                }

                val screenTL = screenFromImage(quad.topLeft)
                val screenTR = screenFromImage(quad.topRight)
                val screenBR = screenFromImage(quad.bottomRight)
                val screenBL = screenFromImage(quad.bottomLeft)

                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            // Unified, non-resetting Pointer Input Gesture Loop
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startPos = down.position
                                    val invMatrix = currentScreenToImgMatrixState.value

                                    val currentQuad = currentQuadState.value
                                    val sTL = screenFromImage(currentQuad.topLeft)
                                    val sTR = screenFromImage(currentQuad.topRight)
                                    val sBR = screenFromImage(currentQuad.bottomRight)
                                    val sBL = screenFromImage(currentQuad.bottomLeft)

                                    val sTopMid = Offset((sTL.x + sTR.x) / 2f, (sTL.y + sTR.y) / 2f)
                                    val sRightMid = Offset((sTR.x + sBR.x) / 2f, (sTR.y + sBR.y) / 2f)
                                    val sBottomMid = Offset((sBL.x + sBR.x) / 2f, (sBL.y + sBR.y) / 2f)
                                    val sLeftMid = Offset((sTL.x + sBL.x) / 2f, (sTL.y + sBL.y) / 2f)

                                    val startImgPt = imageFromScreen(startPos, invMatrix)
                                    val touchHitRadiusPx = with(density) { 46.dp.toPx() }
                                    val edgeSegmentHitRadiusPx = with(density) { 30.dp.toPx() }

                                    fun distSq(p1: Offset, p2: Offset): Float {
                                        val dx = p1.x - p2.x
                                        val dy = p1.y - p2.y
                                        return dx * dx + dy * dy
                                    }

                                    fun distToSegmentSq(p: Offset, a: Offset, b: Offset): Float {
                                        val vx = b.x - a.x
                                        val vy = b.y - a.y
                                        val lenSq = vx * vx + vy * vy
                                        if (lenSq <= 0.0001f) return distSq(p, a)
                                        val t = (((p.x - a.x) * vx + (p.y - a.y) * vy) / lenSq).coerceIn(0f, 1f)
                                        val proj = Offset(a.x + t * vx, a.y + t * vy)
                                        return distSq(p, proj)
                                    }

                                    val rSq = touchHitRadiusPx * touchHitRadiusPx
                                    val edgeSegRSq = edgeSegmentHitRadiusPx * edgeSegmentHitRadiusPx

                                    val handleCandidates = listOf(
                                        ActiveHandle.TOP_LEFT to distSq(startPos, sTL),
                                        ActiveHandle.TOP_RIGHT to distSq(startPos, sTR),
                                        ActiveHandle.BOTTOM_RIGHT to distSq(startPos, sBR),
                                        ActiveHandle.BOTTOM_LEFT to distSq(startPos, sBL),
                                        ActiveHandle.TOP_EDGE to distSq(startPos, sTopMid),
                                        ActiveHandle.RIGHT_EDGE to distSq(startPos, sRightMid),
                                        ActiveHandle.BOTTOM_EDGE to distSq(startPos, sBottomMid),
                                        ActiveHandle.LEFT_EDGE to distSq(startPos, sLeftMid)
                                    )
                                    val closestPointHandle = handleCandidates
                                        .filter { it.second <= rSq }
                                        .minByOrNull { it.second }
                                        ?.first

                                    val edgeSegmentCandidates = listOf(
                                        ActiveHandle.TOP_EDGE to distToSegmentSq(startPos, sTL, sTR),
                                        ActiveHandle.RIGHT_EDGE to distToSegmentSq(startPos, sTR, sBR),
                                        ActiveHandle.BOTTOM_EDGE to distToSegmentSq(startPos, sBL, sBR),
                                        ActiveHandle.LEFT_EDGE to distToSegmentSq(startPos, sTL, sBL)
                                    )
                                    val closestEdgeSegmentHandle = edgeSegmentCandidates
                                        .filter { it.second <= edgeSegRSq }
                                        .minByOrNull { it.second }
                                        ?.first

                                    val handle = when {
                                        closestPointHandle != null -> closestPointHandle
                                        closestEdgeSegmentHandle != null -> closestEdgeSegmentHandle
                                        currentQuad.containsPoint(startImgPt) -> ActiveHandle.CENTER_QUAD
                                        else -> null
                                    }

                                    activeHandle = handle

                                    if (handle != null) {
                                        var dragOffsetInImage = CropPoint(0f, 0f)
                                        val initialDragQuad = currentQuad
                                        val initialTouchImgPt = startImgPt

                                        when (handle) {
                                            ActiveHandle.TOP_LEFT -> {
                                                dragOffsetInImage = CropPoint(startImgPt.x - currentQuad.topLeft.x, startImgPt.y - currentQuad.topLeft.y)
                                                loupePointInImage = currentQuad.topLeft
                                            }
                                            ActiveHandle.TOP_RIGHT -> {
                                                dragOffsetInImage = CropPoint(startImgPt.x - currentQuad.topRight.x, startImgPt.y - currentQuad.topRight.y)
                                                loupePointInImage = currentQuad.topRight
                                            }
                                            ActiveHandle.BOTTOM_RIGHT -> {
                                                dragOffsetInImage = CropPoint(startImgPt.x - currentQuad.bottomRight.x, startImgPt.y - currentQuad.bottomRight.y)
                                                loupePointInImage = currentQuad.bottomRight
                                            }
                                            ActiveHandle.BOTTOM_LEFT -> {
                                                dragOffsetInImage = CropPoint(startImgPt.x - currentQuad.bottomLeft.x, startImgPt.y - currentQuad.bottomLeft.y)
                                                loupePointInImage = currentQuad.bottomLeft
                                            }
                                            ActiveHandle.TOP_EDGE -> {
                                                loupePointInImage = CropPoint(
                                                    (currentQuad.topLeft.x + currentQuad.topRight.x) / 2f,
                                                    (currentQuad.topLeft.y + currentQuad.topRight.y) / 2f
                                                )
                                            }
                                            ActiveHandle.RIGHT_EDGE -> {
                                                loupePointInImage = CropPoint(
                                                    (currentQuad.topRight.x + currentQuad.bottomRight.x) / 2f,
                                                    (currentQuad.topRight.y + currentQuad.bottomRight.y) / 2f
                                                )
                                            }
                                            ActiveHandle.BOTTOM_EDGE -> {
                                                loupePointInImage = CropPoint(
                                                    (currentQuad.bottomLeft.x + currentQuad.bottomRight.x) / 2f,
                                                    (currentQuad.bottomLeft.y + currentQuad.bottomRight.y) / 2f
                                                )
                                            }
                                            ActiveHandle.LEFT_EDGE -> {
                                                loupePointInImage = CropPoint(
                                                    (currentQuad.topLeft.x + currentQuad.bottomLeft.x) / 2f,
                                                    (currentQuad.topLeft.y + currentQuad.bottomLeft.y) / 2f
                                                )
                                            }
                                            ActiveHandle.CENTER_QUAD -> {
                                                loupePointInImage = null
                                            }
                                        }

                                        // Drag loop while pointer is held down
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pointerChange = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!pointerChange.pressed) break

                                            if (pointerChange.positionChanged()) {
                                                pointerChange.consume()
                                                val currScreenPos = pointerChange.position
                                                val currImgPt = imageFromScreen(currScreenPos, invMatrix)

                                                val latestQuad = currentQuadState.value

                                                when (handle) {
                                                    ActiveHandle.TOP_LEFT -> {
                                                        val target = CropPoint((currImgPt.x - dragOffsetInImage.x).coerceIn(0f, imgW), (currImgPt.y - dragOffsetInImage.y).coerceIn(0f, imgH))
                                                        val testQuad = latestQuad.copy(topLeft = target)
                                                        if (testQuad.isValidConvex()) {
                                                            quad = testQuad
                                                            loupePointInImage = target
                                                        }
                                                    }
                                                    ActiveHandle.TOP_RIGHT -> {
                                                        val target = CropPoint((currImgPt.x - dragOffsetInImage.x).coerceIn(0f, imgW), (currImgPt.y - dragOffsetInImage.y).coerceIn(0f, imgH))
                                                        val testQuad = latestQuad.copy(topRight = target)
                                                        if (testQuad.isValidConvex()) {
                                                            quad = testQuad
                                                            loupePointInImage = target
                                                        }
                                                    }
                                                    ActiveHandle.BOTTOM_RIGHT -> {
                                                        val target = CropPoint((currImgPt.x - dragOffsetInImage.x).coerceIn(0f, imgW), (currImgPt.y - dragOffsetInImage.y).coerceIn(0f, imgH))
                                                        val testQuad = latestQuad.copy(bottomRight = target)
                                                        if (testQuad.isValidConvex()) {
                                                            quad = testQuad
                                                            loupePointInImage = target
                                                        }
                                                    }
                                                    ActiveHandle.BOTTOM_LEFT -> {
                                                        val target = CropPoint((currImgPt.x - dragOffsetInImage.x).coerceIn(0f, imgW), (currImgPt.y - dragOffsetInImage.y).coerceIn(0f, imgH))
                                                        val testQuad = latestQuad.copy(bottomLeft = target)
                                                        if (testQuad.isValidConvex()) {
                                                            quad = testQuad
                                                            loupePointInImage = target
                                                        }
                                                    }
                                                    ActiveHandle.TOP_EDGE,
                                                    ActiveHandle.BOTTOM_EDGE,
                                                    ActiveHandle.LEFT_EDGE,
                                                    ActiveHandle.RIGHT_EDGE -> {
                                                        val dx = currImgPt.x - initialTouchImgPt.x
                                                        val dy = currImgPt.y - initialTouchImgPt.y
                                                        val movedQuad = initialDragQuad.moveEdge(
                                                            handle = handle,
                                                            deltaX = dx,
                                                            deltaY = dy,
                                                            imageWidth = imgW,
                                                            imageHeight = imgH
                                                        )
                                                        quad = movedQuad
                                                        loupePointInImage = when (handle) {
                                                            ActiveHandle.TOP_EDGE -> CropPoint(
                                                                (movedQuad.topLeft.x + movedQuad.topRight.x) / 2f,
                                                                (movedQuad.topLeft.y + movedQuad.topRight.y) / 2f
                                                            )
                                                            ActiveHandle.BOTTOM_EDGE -> CropPoint(
                                                                (movedQuad.bottomLeft.x + movedQuad.bottomRight.x) / 2f,
                                                                (movedQuad.bottomLeft.y + movedQuad.bottomRight.y) / 2f
                                                            )
                                                            ActiveHandle.LEFT_EDGE -> CropPoint(
                                                                (movedQuad.topLeft.x + movedQuad.bottomLeft.x) / 2f,
                                                                (movedQuad.topLeft.y + movedQuad.bottomLeft.y) / 2f
                                                            )
                                                            ActiveHandle.RIGHT_EDGE -> CropPoint(
                                                                (movedQuad.topRight.x + movedQuad.bottomRight.x) / 2f,
                                                                (movedQuad.topRight.y + movedQuad.bottomRight.y) / 2f
                                                            )
                                                            else -> null
                                                        }
                                                    }
                                                    ActiveHandle.CENTER_QUAD -> {
                                                        val dx = currImgPt.x - initialTouchImgPt.x
                                                        val dy = currImgPt.y - initialTouchImgPt.y

                                                        val minX = minOf(initialDragQuad.topLeft.x, initialDragQuad.topRight.x, initialDragQuad.bottomRight.x, initialDragQuad.bottomLeft.x)
                                                        val maxX = maxOf(initialDragQuad.topLeft.x, initialDragQuad.topRight.x, initialDragQuad.bottomRight.x, initialDragQuad.bottomLeft.x)
                                                        val minY = minOf(initialDragQuad.topLeft.y, initialDragQuad.topRight.y, initialDragQuad.bottomRight.y, initialDragQuad.bottomLeft.y)
                                                        val maxY = maxOf(initialDragQuad.topLeft.y, initialDragQuad.topRight.y, initialDragQuad.bottomRight.y, initialDragQuad.bottomLeft.y)

                                                        val clampedDx = dx.coerceIn(-minX, imgW - maxX)
                                                        val clampedDy = dy.coerceIn(-minY, imgH - maxY)

                                                        val shiftedQuad = CropQuadrilateral(
                                                            topLeft = CropPoint(initialDragQuad.topLeft.x + clampedDx, initialDragQuad.topLeft.y + clampedDy),
                                                            topRight = CropPoint(initialDragQuad.topRight.x + clampedDx, initialDragQuad.topRight.y + clampedDy),
                                                            bottomRight = CropPoint(initialDragQuad.bottomRight.x + clampedDx, initialDragQuad.bottomRight.y + clampedDy),
                                                            bottomLeft = CropPoint(initialDragQuad.bottomLeft.x + clampedDx, initialDragQuad.bottomLeft.y + clampedDy)
                                                        )
                                                        if (shiftedQuad.isValidConvex()) {
                                                            quad = shiftedQuad
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        activeHandle = null
                                        loupePointInImage = null
                                    }
                                }
                            }
                    ) {
                        // 1. Draw Document Bitmap
                        drawContext.canvas.nativeCanvas.drawBitmap(imageBitmap, imageToScreenMatrix, null)

                        // 2. Draw Dark Scrim Mask Outside Crop Quadrilateral
                        val quadPath = Path().apply {
                            moveTo(screenTL.x, screenTL.y)
                            lineTo(screenTR.x, screenTR.y)
                            lineTo(screenBR.x, screenBR.y)
                            lineTo(screenBL.x, screenBL.y)
                            close()
                        }

                        val fullScreenPath = Path().apply {
                            addRect(Rect(0f, 0f, size.width, size.height))
                        }

                        val scrimPath = Path.combine(
                            operation = PathOperation.Difference,
                            path1 = fullScreenPath,
                            path2 = quadPath
                        )

                        drawPath(scrimPath, color = Color.Black.copy(alpha = 0.68f))

                        // 3. Draw High-Contrast Connecting Quadrilateral Outline
                        val boundaryColor = if (activeHandle != null) Color(0xFF00E676) else Color(0xFF10B981)
                        drawPath(
                            path = quadPath,
                            color = boundaryColor,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // 4. Draw Interactive Edge Midpoint Handles (Top, Right, Bottom, Left)
                        val topMid = Offset((screenTL.x + screenTR.x) / 2f, (screenTL.y + screenTR.y) / 2f)
                        val rightMid = Offset((screenTR.x + screenBR.x) / 2f, (screenTR.y + screenBR.y) / 2f)
                        val bottomMid = Offset((screenBL.x + screenBR.x) / 2f, (screenBL.y + screenBR.y) / 2f)
                        val leftMid = Offset((screenTL.x + screenBL.x) / 2f, (screenTL.y + screenBL.y) / 2f)

                        val edgeHandles = listOf(
                            ActiveHandle.TOP_EDGE to topMid,
                            ActiveHandle.RIGHT_EDGE to rightMid,
                            ActiveHandle.BOTTOM_EDGE to bottomMid,
                            ActiveHandle.LEFT_EDGE to leftMid
                        )

                        edgeHandles.forEach { (eType, mid) ->
                            val isEdgeActive = activeHandle == eType
                            val edgeHaloRadius = if (isEdgeActive) 20.dp.toPx() else 14.dp.toPx()
                            val edgeOuterRadius = if (isEdgeActive) 13.dp.toPx() else 9.dp.toPx()
                            val edgeInnerRadius = if (isEdgeActive) 8.dp.toPx() else 5.5.dp.toPx()

                            drawCircle(
                                color = if (isEdgeActive) Color(0xFF00E676).copy(alpha = 0.45f) else Color(0xFF10B981).copy(alpha = 0.22f),
                                radius = edgeHaloRadius,
                                center = mid
                            )
                            drawCircle(
                                color = if (isEdgeActive) Color(0xFF00E676) else Color(0xFF10B981),
                                radius = edgeOuterRadius,
                                center = mid
                            )
                            drawCircle(
                                color = Color.White,
                                radius = edgeInnerRadius,
                                center = mid
                            )
                        }

                        // 5. Draw 4 CamScanner-Style Corner Handles
                        val handles = listOf(
                            ActiveHandle.TOP_LEFT to screenTL,
                            ActiveHandle.TOP_RIGHT to screenTR,
                            ActiveHandle.BOTTOM_RIGHT to screenBR,
                            ActiveHandle.BOTTOM_LEFT to screenBL
                        )

                        handles.forEach { (hType, pt) ->
                            val isActive = activeHandle == hType
                            val outerRadius = if (isActive) 18.dp.toPx() else 14.dp.toPx()
                            val innerRadius = if (isActive) 13.dp.toPx() else 10.dp.toPx()
                            val haloRadius = if (isActive) 26.dp.toPx() else 20.dp.toPx()

                            // Outer halo
                            drawCircle(
                                color = if (isActive) Color(0xFF00E676).copy(alpha = 0.45f) else Color(0xFF10B981).copy(alpha = 0.25f),
                                radius = haloRadius,
                                center = pt
                            )
                            // Solid outer ring
                            drawCircle(
                                color = if (isActive) Color(0xFF00E676) else Color(0xFF10B981),
                                radius = outerRadius,
                                center = pt
                            )
                            // White target center
                            drawCircle(
                                color = Color.White,
                                radius = innerRadius,
                                center = pt
                            )
                            // Center dot
                            drawCircle(
                                color = if (isActive) Color(0xFF00E676) else Color(0xFF10B981),
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                        }
                    }

                    // ==============================================================
                    // CAMSCANNER MAGNIFYING GLASS LOUPE LENS (ULTRA-PREMIUM)
                    // ==============================================================
                    loupePointInImage?.let { loupePt ->
                        Box(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape),
                                color = Color.Black,
                                shape = CircleShape,
                                shadowElevation = 12.dp,
                                border = androidx.compose.foundation.BorderStroke(3.5.dp, Color(0xFF00E676))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val loupeW = size.width
                                    val loupeH = size.height
                                    val loupeCenter = Offset(loupeW / 2f, loupeH / 2f)

                                    // Matrix to draw magnified 2.6x slice of imageBitmap
                                    val loupeZoom = 2.6f
                                    val loupeMatrix = Matrix().apply {
                                        postTranslate(-loupePt.x, -loupePt.y)
                                        postScale(loupeZoom, loupeZoom)
                                        postTranslate(loupeCenter.x, loupeCenter.y)
                                    }

                                    // Draw magnified bitmap
                                    val paint = NativePaint().apply {
                                        isAntiAlias = true
                                        isFilterBitmap = true
                                    }
                                    drawContext.canvas.nativeCanvas.drawBitmap(imageBitmap, loupeMatrix, paint)

                                    // Draw precision crosshair (+) in center
                                    val crosshairColor = Color(0xFF00E676)
                                    val len = 16.dp.toPx()

                                    // Horizontal crosshair line
                                    drawLine(
                                        color = crosshairColor,
                                        start = Offset(loupeCenter.x - len, loupeCenter.y),
                                        end = Offset(loupeCenter.x + len, loupeCenter.y),
                                        strokeWidth = 2.dp.toPx()
                                    )

                                    // Vertical crosshair line
                                    drawLine(
                                        color = crosshairColor,
                                        start = Offset(loupeCenter.x, loupeCenter.y - len),
                                        end = Offset(loupeCenter.x, loupeCenter.y + len),
                                        strokeWidth = 2.dp.toPx()
                                    )

                                    // Center dot target
                                    drawCircle(
                                        color = Color.White,
                                        radius = 3.dp.toPx(),
                                        center = loupeCenter
                                    )
                                }
                            }
                        }
                    }
                }

                // Auto-detection loading overlay
                if (isAutoDetecting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.DarkGray,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00E676),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Detecting Address Label...", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // FILTER SELECTION CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LabelFilterMode.values().forEach { mode ->
                    val isSelected = selectedFilter == mode
                    Surface(
                        onClick = { selectedFilter = mode },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF00E676) else Color(0xFF222222),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // REQUIRED BOTTOM TOOLBAR
            // Buttons: Retake, Reset, Auto Detect, Confirm & Scan
            Surface(
                color = Color(0xFF141414),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Retake Button
                        OutlinedButton(
                            onClick = onRetake,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("crop_editor_retake_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Retake", fontSize = 11.sp)
                        }

                        // Reset Button
                        OutlinedButton(
                            onClick = {
                                quad = CropQuadrilateral.createDefaultInset(imgW, imgH, insetPct = 0.06f)
                                zoomScale = 1.0f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("crop_editor_reset_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Reset", fontSize = 11.sp)
                        }

                        // Auto Detect Button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isAutoDetecting = true
                                    quad = BoundaryDetector.detectBoundary(imageBitmap)
                                    isAutoDetecting = false
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("crop_editor_autodetect_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF00E676))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Auto Detect", fontSize = 11.sp, color = Color(0xFF00E676), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Confirm & Scan Button (Perspective Correction + Filter + OCR)
                    Button(
                        onClick = {
                            scope.launch {
                                val warped = PerspectiveTransformer.perspectiveCropAndWarp(imageBitmap, quad)
                                val filtered = ImageEnhancer.applyFilter(warped, selectedFilter)
                                onConfirmAndScan(filtered)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("crop_editor_confirm_scan_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm & Scan (OCR)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
