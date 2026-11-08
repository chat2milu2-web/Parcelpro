package com.example.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressPhotoScannerDialog(
    onAddressExtracted: (name: String?, pincode: String?, imageUri: Uri?, fullText: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Hide software keyboard on open to prevent overlap
    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isProcessingOcr by remember { mutableStateOf(false) }
    var ocrErrorMessage by remember { mutableStateOf<String?>(null) }

    // Cropping flow states
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Continuous Batch Scan Mode
    var isContinuousMode by remember { mutableStateOf(false) }
    var sessionScannedCount by remember { mutableStateOf(0) }

    // Multi-pincode selection state
    var pendingScanResult by remember { mutableStateOf<AddressScanResult?>(null) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val input = context.contentResolver.openInputStream(selectedUri)
                    val bmp = BitmapFactory.decodeStream(input)
                    input?.close()
                    withContext(Dispatchers.Main) {
                        if (bmp != null) {
                            capturedBitmap = bmp
                        } else {
                            ocrErrorMessage = "Could not decode gallery image."
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        ocrErrorMessage = "Failed to load image: ${e.localizedMessage}"
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler {
        if (capturedBitmap != null) {
            capturedBitmap = null
        } else {
            onDismiss()
        }
    }

    Scaffold(
        topBar = {
            if (capturedBitmap == null) {
                TopAppBar(
                    title = { Text("Scan Address Photo", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.testTag("address_scanner_close_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    actions = {
                        // Continuous Scan Mode Toggle Button
                        IconButton(
                            onClick = { isContinuousMode = !isContinuousMode },
                            modifier = Modifier.testTag("address_scanner_continuous_toggle")
                        ) {
                            Surface(
                                color = if (isContinuousMode) Color(0xFF00E676) else Color.DarkGray,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isContinuousMode) "Auto: ON ($sessionScannedCount)" else "Auto: OFF",
                                    color = if (isContinuousMode) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Flash Toggle
                        IconButton(
                            onClick = {
                                val newFlash = !isFlashOn
                                camera?.cameraControl?.enableTorch(newFlash)
                                isFlashOn = newFlash
                            },
                            modifier = Modifier.testTag("address_scanner_flash_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = if (isFlashOn) Color.Yellow else Color.White
                            )
                        }

                        // Gallery Picker
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("address_scanner_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Pick from Gallery",
                                tint = Color.White
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (capturedBitmap != null) {
                // ==========================================
                // DOCUMENT CROP EDITOR SCREEN
                // ==========================================
                // NATIVE CAMSCANNER-STYLE CROP ENGINE SCREEN
                // ==========================================
                CropEditorScreen(
                    imageBitmap = capturedBitmap!!,
                    onRetake = { capturedBitmap = null },
                    onRotate = {
                        capturedBitmap?.let { bmp ->
                            capturedBitmap = PerspectiveTransformer.rotate90(bmp)
                        }
                    },
                    onCancel = { onDismiss() },
                    onConfirmAndScan = { croppedBmp ->
                        isProcessingOcr = true
                        ocrErrorMessage = null

                        scope.launch(Dispatchers.IO) {
                            try {
                                val stableUri = PerspectiveTransformer.saveToCache(context, croppedBmp)

                                withContext(Dispatchers.Main) {
                                    pendingImageUri = stableUri
                                    AddressOcrProcessor.processBitmap(
                                        bitmap = croppedBmp,
                                        onSuccess = { result ->
                                            isProcessingOcr = false
                                            handleOcrResult(result, stableUri, onAddressExtracted = { n, p, u, t ->
                                                onAddressExtracted(n, p, u, t)
                                                if (isContinuousMode) {
                                                    sessionScannedCount++
                                                    capturedBitmap = null // Resets back to live camera view immediately!
                                                } else {
                                                    onDismiss()
                                                }
                                            }, onShowPinDialog = { res ->
                                                pendingScanResult = res
                                            }, onError = { err ->
                                                ocrErrorMessage = err
                                            })
                                        },
                                        onError = { err ->
                                            isProcessingOcr = false
                                            ocrErrorMessage = err
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessingOcr = false
                                    ocrErrorMessage = "Cropping failed: ${e.localizedMessage}"
                                }
                            }
                        }
                    }
                )
            } else if (hasCameraPermission) {
                // ==========================================
                // LIVE CAMERA PREVIEW SCREEN
                // ==========================================
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture
                                )
                                camera = cam
                            } catch (e: Exception) {
                                ocrErrorMessage = "Failed to start camera: ${e.localizedMessage}"
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Viewfinder Framing Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameWidth = size.width * 0.80f
                    val frameHeight = size.height * 0.42f
                    val left = (size.width - frameWidth) / 2f
                    val top = (size.height - frameHeight) / 2f - 30.dp.toPx()

                    // Dim background outside frame
                    drawRect(
                        color = Color.Black.copy(alpha = 0.55f),
                        size = size
                    )

                    // Clear frame center border
                    drawRoundRect(
                        color = Color(0xFF22C55E),
                        topLeft = Offset(left, top),
                        size = Size(frameWidth, frameHeight),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Instruction & Guide Text
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Align address label inside the box & capture",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bottom Controls: Shutter Button and Gallery Pick
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pick from Gallery shortcut
                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("address_scanner_gallery_btn_bottom")
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery")
                        }

                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f))
                                .clickable {
                                    val capture = imageCapture ?: return@clickable
                                    isProcessingOcr = true
                                    ocrErrorMessage = null

                                    val photoFile = File(context.cacheDir, "addr_${System.currentTimeMillis()}.jpg")
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                    capture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                isProcessingOcr = false
                                                val bmp = BitmapFactory.decodeFile(photoFile.absolutePath)
                                                val rotatedBmp = rotateBitmapIfNeeded(photoFile.absolutePath, bmp)
                                                // Crop captured bitmap to match the green viewfinder box area
                                                val viewfinderCroppedBmp = cropBitmapToViewfinderFrame(rotatedBmp, 0.10f, 0.24f, 0.90f, 0.66f)
                                                capturedBitmap = viewfinderCroppedBmp
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                isProcessingOcr = false
                                                ocrErrorMessage = "Capture failed: ${exception.localizedMessage}"
                                            }
                                        }
                                    )
                                }
                                .testTag("address_scanner_shutter_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Capture",
                                        tint = Color.Black,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        // Cancel button
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("address_scanner_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            } else {
                // Camera Permission Denied View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Yellow,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To scan address photos directly with your camera, please grant camera access, or select a photo from your gallery.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.testTag("grant_camera_permission_button")
                    ) {
                        Text("Grant Camera Permission")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Photo from Gallery")
                    }
                }
            }

            // Processing Loading Indicator Overlay
            if (isProcessingOcr) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Reading Cropped Address...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "On-device ML Kit OCR is extracting Name & Pincode from selected frame",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // OCR Error Dialog / Banner
            ocrErrorMessage?.let { err ->
                AlertDialog(
                    onDismissRequest = { ocrErrorMessage = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Address Scan Notice", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text(err)
                    },
                    confirmButton = {
                        Button(onClick = { ocrErrorMessage = null }) {
                            Text("Try Again")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Enter Manually")
                        }
                    }
                )
            }

            // Multiple Pincodes Found Selection Dialog
            pendingScanResult?.let { result ->
                if (result.pincodes.size > 1) {
                    AlertDialog(
                        onDismissRequest = { pendingScanResult = null },
                        title = {
                            Text("Select Destination Pincode", fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Multiple 6-digit Pincodes were found in this address label. Please select the correct one:",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                result.addresseeName?.let { name ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Extracted Name: $name",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                result.pincodes.forEach { pin ->
                                    Button(
                                        onClick = {
                                            onAddressExtracted(
                                                result.addresseeName,
                                                pin,
                                                pendingImageUri,
                                                result.fullText
                                            )
                                            pendingScanResult = null
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("select_pincode_$pin"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("PINCODE: $pin", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onAddressExtracted(
                                        result.addresseeName,
                                        null,
                                        pendingImageUri,
                                        result.fullText
                                    )
                                    pendingScanResult = null
                                    onDismiss()
                                }
                            ) {
                                Text("Enter Pincode Manually")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingScanResult = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun cropBitmapToViewfinderFrame(
    bitmap: Bitmap?,
    leftPct: Float = 0.10f,
    topPct: Float = 0.24f,
    rightPct: Float = 0.90f,
    bottomPct: Float = 0.66f
): Bitmap? {
    if (bitmap == null) return null
    return try {
        val width = bitmap.width
        val height = bitmap.height
        val x = (leftPct * width).toInt().coerceIn(0, width - 2)
        val y = (topPct * height).toInt().coerceIn(0, height - 2)
        val w = ((rightPct - leftPct) * width).toInt().coerceIn(2, width - x)
        val h = ((bottomPct - topPct) * height).toInt().coerceIn(2, height - y)
        Bitmap.createBitmap(bitmap, x, y, w, h)
    } catch (e: Exception) {
        bitmap
    }
}

private fun rotateBitmapIfNeeded(imagePath: String, bitmap: Bitmap?): Bitmap? {
    if (bitmap == null) return null
    return try {
        val exif = android.media.ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(
            android.media.ExifInterface.TAG_ORIENTATION,
            android.media.ExifInterface.ORIENTATION_UNDEFINED
        )
        val matrix = Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        bitmap
    }
}

private fun handleOcrResult(
    result: AddressScanResult,
    imageUri: Uri,
    onAddressExtracted: (name: String?, pincode: String?, imageUri: Uri?, fullText: String) -> Unit,
    onShowPinDialog: (AddressScanResult) -> Unit,
    onError: (String) -> Unit
) {
    if (result.errorMessage != null) {
        onError(result.errorMessage)
        return
    }

    if (result.addresseeName.isNullOrBlank() && result.pincodes.isEmpty()) {
        onError("Could not detect Name or Pincode on the cropped area. Please try adjusting the crop frame or enter manually.")
        return
    }

    if (result.pincodes.size > 1) {
        onShowPinDialog(result)
    } else {
        val singlePin = result.pincodes.firstOrNull()
        onAddressExtracted(result.addresseeName, singlePin, imageUri, result.fullText)
    }
}
