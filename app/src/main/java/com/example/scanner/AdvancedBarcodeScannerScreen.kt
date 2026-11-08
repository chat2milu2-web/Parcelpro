package com.example.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

enum class ScanMode {
    SINGLE,
    CONTINUOUS
}

@Composable
fun AdvancedBarcodeScannerScreen(
    batchCode: String,
    savedParcelCount: Int,
    initialScanMode: ScanMode = ScanMode.SINGLE,
    hapticFeedbackEnabled: Boolean = true,
    soundFeedbackEnabled: Boolean = true,
    existingBarcodes: Set<String> = emptySet(),
    onDuplicateDetected: ((scannedValue: String) -> Unit)? = null,
    onBarcodeScanned: (scannedValue: String, isContinuous: Boolean) -> Unit,
    onCloseScanner: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentScanMode by remember { mutableStateOf(initialScanMode) }
    var isTorchOn by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(soundFeedbackEnabled) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var currentZoomRatio by remember { mutableFloatStateOf(1.0f) }

    // Multi-frame Accuracy Verifier (Requires 5 consecutive matching frames of EW112223445IN)
    val verifier = remember { BarcodeAccuracyVerifier(requiredConsecutiveFrames = 5) }
    var verifierState by remember { mutableStateOf(BarcodeAccuracyVerifier.State()) }

    var lastScannedCode by remember { mutableStateOf("") }
    var lastScannedTimestamp by remember { mutableStateOf(0L) }
    var showSuccessBanner by remember { mutableStateOf(false) }
    var duplicateDetectedCode by remember { mutableStateOf<String?>(null) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcodeValue by remember { mutableStateOf("") }
    var manualInputError by remember { mutableStateOf<String?>(null) }

    val lockDurationMs = 2500L

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
            } catch (e: Exception) {
                Log.e("Scanner", "Error resetting torch", e)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview View with Tap-to-Focus
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    previewView.setOnTouchListener { view, event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            val cam = camera
                            if (cam != null) {
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                cam.cameraControl.startFocusAndMetering(action)
                            }
                            view.performClick()
                        }
                        true
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        // High accuracy barcode options supporting India Post 1D and 2D formats
                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_CODE_93,
                                Barcode.FORMAT_ITF,
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_QR_CODE
                            )
                            .build()

                        val barcodeScanner = BarcodeScanning.getClient(options)
                        val executor = Executors.newSingleThreadExecutor()

                        // High resolution ImageAnalysis for crisp barcode edge detection
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            @SuppressLint("UnsafeOptInUsageError")
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        val detected = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                        val rawValue = detected?.rawValue

                                        // Feed detected raw value to the multi-frame accuracy verifier
                                        val newState = verifier.processFrame(rawValue)
                                        verifierState = newState

                                        if (newState.status == BarcodeAccuracyVerifier.Status.VERIFIED && newState.verifiedCode != null) {
                                            val verifiedCode = newState.verifiedCode
                                            val currentTime = System.currentTimeMillis()
                                            val isSameCode = verifiedCode == lastScannedCode
                                            val isWithinLockTime = (currentTime - lastScannedTimestamp) < lockDurationMs

                                            if (!isSameCode || !isWithinLockTime) {
                                                lastScannedCode = verifiedCode
                                                lastScannedTimestamp = currentTime

                                                if (existingBarcodes.contains(verifiedCode)) {
                                                    // Duplicate in batch detected!
                                                    duplicateDetectedCode = verifiedCode
                                                    showSuccessBanner = false
                                                    if (hapticFeedbackEnabled) triggerVibration(ctx)
                                                    onDuplicateDetected?.invoke(verifiedCode)
                                                } else {
                                                    duplicateDetectedCode = null
                                                    showSuccessBanner = true

                                                    if (hapticFeedbackEnabled) triggerVibration(ctx)
                                                    if (soundEnabled) playBeepSound()

                                                    onBarcodeScanned(verifiedCode, currentScanMode == ScanMode.CONTINUOUS)
                                                }
                                                verifier.reset()
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            camera = cam
                        } catch (e: Exception) {
                            Log.e("AdvancedScanner", "Camera binding error", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Scanning Box & Animated Laser Line
            val infiniteTransition = rememberInfiniteTransition(label = "ScanLine")
            val scanLinePos by infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = 0.85f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ScanLinePos"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxWidth = size.width * 0.84f
                val boxHeight = size.height * 0.36f
                val left = (size.width - boxWidth) / 2
                val top = (size.height - boxHeight) / 2 - 30.dp.toPx()

                // Translucent outer mask
                drawRect(
                    color = Color.Black.copy(alpha = 0.45f)
                )

                val cornerColor = when (verifierState.status) {
                    BarcodeAccuracyVerifier.Status.VERIFIED -> Color(0xFF16A34A)
                    BarcodeAccuracyVerifier.Status.VERIFYING -> Color(0xFF38BDF8)
                    BarcodeAccuracyVerifier.Status.FORMAT_ERROR -> Color(0xFFEF4444)
                    else -> Color.Cyan
                }

                // Bounding box with dynamic feedback color
                drawRoundRect(
                    color = cornerColor,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Animated scan line
                val currentLineY = top + (boxHeight * scanLinePos)
                drawLine(
                    color = when (verifierState.status) {
                        BarcodeAccuracyVerifier.Status.VERIFIED -> Color(0xFF16A34A)
                        BarcodeAccuracyVerifier.Status.VERIFYING -> Color(0xFF38BDF8)
                        else -> Color.Red
                    },
                    start = Offset(left + 12.dp.toPx(), currentLineY),
                    end = Offset(left + boxWidth - 12.dp.toPx(), currentLineY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Top Control Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Batch: $batchCode",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$savedParcelCount Saved",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                isTorchOn = !isTorchOn
                                camera?.cameraControl?.enableTorch(isTorchOn)
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                .testTag("scanner_torch_toggle")
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flashlight Toggle",
                                tint = if (isTorchOn) Color.Yellow else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { soundEnabled = !soundEnabled },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Mute Toggle",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onCloseScanner,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                .testTag("scanner_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Scanner",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Selector Bar (Single vs Continuous)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val singleSelected = currentScanMode == ScanMode.SINGLE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (singleSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { currentScanMode = ScanMode.SINGLE }
                            .padding(vertical = 8.dp)
                            .testTag("mode_single_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Single Scan",
                            color = if (singleSelected) Color.White else Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    val continuousSelected = currentScanMode == ScanMode.CONTINUOUS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (continuousSelected) Color(0xFF16A34A) else Color.Transparent)
                            .clickable { currentScanMode = ScanMode.CONTINUOUS }
                            .padding(vertical = 8.dp)
                            .testTag("mode_continuous_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = if (continuousSelected) Color.White else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Continuous Mode",
                                color = if (continuousSelected) Color.White else Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Bottom Information, Verification Progress & Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Multi-Frame Verification Progress Card (Accuracy-First Indicator)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (verifierState.status) {
                            BarcodeAccuracyVerifier.Status.VERIFIED -> Color(0xFF14532D)
                            BarcodeAccuracyVerifier.Status.VERIFYING -> Color(0xFF0F172A).copy(alpha = 0.9f)
                            BarcodeAccuracyVerifier.Status.FORMAT_ERROR -> Color(0xFF7F1D1D)
                            else -> Color.Black.copy(alpha = 0.8f)
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = when (verifierState.status) {
                            BarcodeAccuracyVerifier.Status.VERIFIED -> Color(0xFF22C55E)
                            BarcodeAccuracyVerifier.Status.VERIFYING -> Color(0xFF38BDF8)
                            BarcodeAccuracyVerifier.Status.FORMAT_ERROR -> Color(0xFFEF4444)
                            else -> Color.White.copy(alpha = 0.2f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (verifierState.status) {
                            BarcodeAccuracyVerifier.Status.VERIFYING -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "VERIFYING ACCURACY (5 FRAMES CHECK)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                        Text(
                                            text = verifierState.candidateCode ?: "",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "${verifierState.consecutiveFrames}/${verifierState.requiredFrames}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }

                                // 5 Stepped Frame Verification Pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (i in 1..5) {
                                        val isChecked = i <= verifierState.consecutiveFrames
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isChecked) Color(0xFF22C55E) else Color.White.copy(alpha = 0.15f)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isChecked) "✓ $i" else "$i",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isChecked) Color.Black else Color.White.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { verifierState.progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF38BDF8),
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )

                                Text(
                                    text = "Holding frame for strict verification... Scanning speed is secondary to 100% accuracy.",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }

                            BarcodeAccuracyVerifier.Status.FORMAT_ERROR -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "INVALID BARCODE FORMAT DETECTED",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF87171)
                                        )
                                        Text(
                                            text = verifierState.invalidNotice ?: "Expected 2 letters + 9 digits + IN",
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            BarcodeAccuracyVerifier.Status.VERIFIED -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4ADE80),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "ACCURACY VERIFIED (5/5 FRAMES CONFIRMED)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4ADE80)
                                        )
                                        Text(
                                            text = verifierState.verifiedCode ?: "",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            else -> {
                                Column {
                                    Text(
                                        text = "TARGET FORMAT: EW112223445IN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Cyan
                                    )
                                    Text(
                                        text = "Strict 13 chars (2 letters + 9 digits + IN). Continuous autofocus & 5-frame verification active.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }

                // Duplicate Warning Banner
                AnimatedVisibility(visible = duplicateDetectedCode != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DUPLICATE BARCODE DETECTED!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Barcode '${duplicateDetectedCode ?: ""}' is already in this batch!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { duplicateDetectedCode = null }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                            }
                        }
                    }
                }

                // Quick Camera Zoom Bar (1.0x, 1.5x, 2.0x, 2.5x)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zoom:",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )

                    val zoomLevels = listOf(1.0f, 1.5f, 2.0f, 2.5f)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        zoomLevels.forEach { ratio ->
                            val isSelected = currentZoomRatio == ratio
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        currentZoomRatio = ratio
                                        camera?.cameraControl?.setZoomRatio(ratio)
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${ratio}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Manual Entry & Stop Scanning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            manualBarcodeValue = ""
                            manualInputError = null
                            showManualInputDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("manual_barcode_entry_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manual Entry", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onCloseScanner,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("stop_scanning_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Close Scanner", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Manual Barcode Entry Dialog with Strict Validation
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = { Text("Manual Barcode Entry", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Enter strict 13-character Indian Post barcode (e.g. EW112223445IN):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = manualBarcodeValue,
                        onValueChange = {
                            manualBarcodeValue = it.uppercase().take(13)
                            manualInputError = null
                        },
                        label = { Text("Barcode (13 Chars)") },
                        placeholder = { Text("EW112223445IN") },
                        isError = manualInputError != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_barcode_text_input")
                    )
                    manualInputError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = manualBarcodeValue.trim().uppercase()
                        if (verifier.isValidFormat(trimmed)) {
                            onBarcodeScanned(trimmed, currentScanMode == ScanMode.CONTINUOUS)
                            showManualInputDialog = false
                            manualBarcodeValue = ""
                        } else {
                            manualInputError = "Must be 13 chars: 2 uppercase letters + 9 digits + IN (e.g. EW112223445IN)"
                        }
                    },
                    modifier = Modifier.testTag("submit_manual_barcode_button")
                ) {
                    Text("Validate & Use")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun triggerVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    } catch (e: Exception) {
        Log.e("ScannerVibration", "Vibration error", e)
    }
}

private fun playBeepSound() {
    try {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    } catch (e: Exception) {
        Log.e("ScannerBeep", "Sound feedback error", e)
    }
}
