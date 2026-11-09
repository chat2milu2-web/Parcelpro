package com.example.ui.screens.entry

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.scanner.AddressPhotoScannerDialog
import com.example.scanner.BarcodeAccuracyVerifier
import com.example.ui.components.AnimatedGradientBorderCard
import com.example.util.InlineVoiceListeningBanner
import com.example.util.VoiceInputHelper
import com.example.util.rememberInlineVoiceController
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelEntryScreen(
    batchId: Long,
    viewModel: ParcelEntryViewModel,
    onNavigateBack: () -> Unit,
    onViewBatchHistory: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    val barcodeFocusRequester = remember { FocusRequester() }
    val pincodeFocusRequester = remember { FocusRequester() }
    val addresseeFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }
    val lengthFocusRequester = remember { FocusRequester() }
    val breadthFocusRequester = remember { FocusRequester() }
    val heightFocusRequester = remember { FocusRequester() }

    val currentBatch by viewModel.currentBatch.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val recentMessage by viewModel.recentSavedMessage.collectAsStateWithLifecycle()

    val batchParcels by viewModel.batchParcels.collectAsStateWithLifecycle()
    val lastSavedParcel by viewModel.lastSavedParcel.collectAsStateWithLifecycle()
    val recentPincodes by viewModel.recentPincodes.collectAsStateWithLifecycle()
    val recentAddressees by viewModel.recentAddressees.collectAsStateWithLifecycle()

    val pincodeLength by viewModel.pincodeLength.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val dimensionUnit by viewModel.dimensionUnit.collectAsStateWithLifecycle()
    val maxLength by viewModel.maxLength.collectAsStateWithLifecycle()
    val maxBreadth by viewModel.maxBreadth.collectAsStateWithLifecycle()
    val maxHeight by viewModel.maxHeight.collectAsStateWithLifecycle()
    val volumetricDivisor by viewModel.volumetricDivisor.collectAsStateWithLifecycle()

    var showCameraScanner by remember { mutableStateOf(false) }
    var showAddressPhotoScanner by remember { mutableStateOf(false) }
    var showPresetSearchBox by remember { mutableStateOf(false) }

    val voiceController = rememberInlineVoiceController(
        onCandidatesReceived = { target, candidates, _ ->
            when (target) {
                "PINCODE" -> {
                    val parsed = VoiceInputHelper.parseVoiceCandidates(candidates, isNumericField = true).take(pincodeLength)
                    if (parsed.isNotBlank()) viewModel.updatePincode(parsed)
                }
                "ADDRESSEE" -> {
                    val parsed = VoiceInputHelper.parseVoiceCandidates(candidates, isNumericField = false)
                    if (parsed.isNotBlank()) viewModel.updateAddressee(parsed)
                }
                "WEIGHT" -> {
                    val parsed = VoiceInputHelper.parseVoiceCandidates(candidates, isNumericField = true)
                    if (parsed.isNotBlank()) viewModel.updateWeight(parsed)
                }
                "LENGTH" -> {
                    val parsed = VoiceInputHelper.parseVoiceCandidates(candidates, isNumericField = true)
                    if (parsed.isNotBlank()) viewModel.updateLength(parsed)
                }
                "BREADTH" -> {
                    val parsed = VoiceInputHelper.parseVoiceCandidates(candidates, isNumericField = true)
                    if (parsed.isNotBlank()) viewModel.updateBreadth(parsed)
                }
                "HEIGHT" -> {
                    val parsed = VoiceInputHelper.parseVoiceCandidates(candidates, isNumericField = true)
                    if (parsed.isNotBlank()) viewModel.updateHeight(parsed)
                }
                "ALL_DIMENSIONS" -> {
                    val numbers = VoiceInputHelper.extractMultipleNumbersFromCandidates(candidates)
                    if (numbers.isNotEmpty()) viewModel.updateLength(numbers[0])
                    if (numbers.size > 1) viewModel.updateBreadth(numbers[1])
                    if (numbers.size > 2) viewModel.updateHeight(numbers[2])
                }
            }
        },
        onError = { errMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(errMsg)
            }
        }
    )

    val startVoiceInput: (String, String) -> Unit = { target, promptMsg ->
        voiceController.startListening(target, promptMsg)
    }

    val isBarcodeValid = remember(formState.barcode) {
        formState.barcode.length == BarcodeAccuracyVerifier.EXACT_LENGTH &&
            BarcodeAccuracyVerifier.STRICT_BARCODE_REGEX.matches(formState.barcode)
    }
    val isPincodeValid = formState.pincode.length == pincodeLength
    val isAddresseeValid = formState.addressee.isNotBlank()
    val weightNumeric = formState.weight.toDoubleOrNull() ?: 0.0
    val isWeightValid = weightNumeric > 0.0

    val readyCount = (if (isBarcodeValid) 1 else 0) +
        (if (isPincodeValid) 1 else 0) +
        (if (isAddresseeValid) 1 else 0) +
        (if (isWeightValid) 1 else 0)

    val isFormReady = isBarcodeValid && isPincodeValid && isAddresseeValid && isWeightValid

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraScanner = true
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to scan barcodes.")
            }
        }
    }

    LaunchedEffect(batchId) {
        viewModel.loadBatch(batchId)
    }

    LaunchedEffect(recentMessage) {
        recentMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.dismissSuccessMessage()
            }
        }
    }

    @Composable
    fun voiceHighlightColors(targetKey: String) = if (
        voiceController.isListening && voiceController.activeTarget == targetKey
    ) {
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.error,
            unfocusedBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = MaterialTheme.colorScheme.error,
            unfocusedLabelColor = MaterialTheme.colorScheme.error,
            focusedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.14f),
            unfocusedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.14f)
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!showCameraScanner && !showAddressPhotoScanner) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.saveParcel(onSuccess = {})
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFormReady) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                                contentColor = if (isFormReady) Color.Black else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_next_button")
                        ) {
                            Icon(
                                imageVector = if (isFormReady) Icons.Default.CheckCircle else Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFormReady) "✓ READY — SAVE & NEXT PARCEL" else "SAVE & NEXT PARCEL ($readyCount/4 Ready)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.saveParcel(andNavigateToHistory = true, onSuccess = {
                                        currentBatch?.let { batch -> onViewBatchHistory(batch.id) }
                                    })
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save & View Batch", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.clearForm()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Form", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        topBar = {
            if (!showCameraScanner && !showAddressPhotoScanner) {
                TopAppBar(
                    title = {
                        Column {
                            Text("New Parcel Entry", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                text = "Batch: ${currentBatch?.batchCode ?: "Loading..."}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("entry_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        currentBatch?.let { batch ->
                            Surface(
                                onClick = { onViewBatchHistory(batch.id) },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("live_parcel_counter_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "📦 ${batchParcels.size} Saved",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onViewBatchHistory(batch.id) },
                                modifier = Modifier.testTag("entry_view_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "View Batch Records",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InlineVoiceListeningBanner(controller = voiceController)

            // 1. Last Saved Parcel Mini-Bar with 1-Tap Undo/Edit
            lastSavedParcel?.let { last ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("last_saved_parcel_strip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Last #${last.sequenceNumber}: ${last.barcode}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${last.addresseeName} • PIN ${last.destinationPincode} • ${last.physicalWeight} $weightUnit",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            onClick = { viewModel.recallLastSavedParcel(last) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("undo_last_saved_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Recall & Edit Last Saved Parcel",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit Last",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Error Message Banner
            formState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Cross-Batch Duplicate Warning Notice
            formState.crossBatchWarning?.let { warning ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = warning,
                            color = Color(0xFF92400E),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissCrossBatchWarning() },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Main Parcel Entry Form Card (Subtle Animated Gradient Border)
            AnimatedGradientBorderCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_parcel_entry_card"),
                borderWidth = 2.dp,
                outerCornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Section Header + Readiness Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Identification & Destination",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isFormReady) {
                                Color(0xFF16A34A).copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = if (isFormReady) "✓ All 4 Ready" else "$readyCount/4 Ready",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFormReady) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Compact Dual-Scanner Quick Action Bar (Barcode Scanner + Address OCR Side-by-Side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    showCameraScanner = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("camera_scan_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scan Barcode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = { showAddressPhotoScanner = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("scan_address_photo_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scan Address Photo",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Address OCR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Scanned Image Preview Card
                    formState.addressPhotoUri?.let { uriStr ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = uriStr,
                                    contentDescription = "Scanned Address Label",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Address Photo Scanned",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                    formState.ocrNotice?.let { notice ->
                                        Text(
                                            text = notice,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.clearAddressPhoto() },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("clear_address_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove photo",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Barcode Field with Live Counter (13/13), Checkmark & Clear Button
                    OutlinedTextField(
                        value = formState.barcode,
                        onValueChange = { viewModel.updateBarcode(it) },
                        label = { Text("Barcode Number (13 Chars) *") },
                        placeholder = { Text("e.g. EW112223445IN") },
                        singleLine = true,
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                if (isBarcodeValid) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid Barcode",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${formState.barcode.length}/13",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (formState.barcode.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.updateBarcode("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Barcode",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(barcodeFocusRequester)
                            .testTag("barcode_input"),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { pincodeFocusRequester.requestFocus() }
                        )
                    )

                    // Destination Pincode with Live Counter (6/6), Checkmark, Clear & Active Voice Highlight
                    OutlinedTextField(
                        value = formState.pincode,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }.take(pincodeLength)
                            viewModel.updatePincode(filtered)
                            if (filtered.length == pincodeLength) {
                                addresseeFocusRequester.requestFocus()
                            }
                        },
                        label = { Text("Destination Pincode ($pincodeLength Digits) *") },
                        placeholder = { Text("e.g. 700001") },
                        singleLine = true,
                        colors = voiceHighlightColors("PINCODE"),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isPincodeValid) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid Pincode",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(17.dp)
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${formState.pincode.length}/$pincodeLength",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (formState.pincode.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.updatePincode("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Pincode",
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { startVoiceInput("PINCODE", "Speak Pincode (English, Hindi, Bengali)") },
                                    modifier = Modifier.testTag("voice_pincode_button")
                                ) {
                                    val isPinListening = voiceController.isListening && voiceController.activeTarget == "PINCODE"
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input Pincode",
                                        tint = if (isPinListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(pincodeFocusRequester)
                            .testTag("pincode_input"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { addresseeFocusRequester.requestFocus() }
                        )
                    )

                    // Auto-Resolved Indian City / State Location Badge
                    formState.resolvedLocation?.let { locationName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("pincode_resolved_location_badge")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Verified Area: $locationName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF059669)
                            )
                        }
                    }

                    // 4. Recent Pincode 1-Tap Chips (from current batch)
                    if (recentPincodes.isNotEmpty() && formState.pincode.length < pincodeLength) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent PINs:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            recentPincodes.forEach { pin ->
                                Surface(
                                    onClick = {
                                        viewModel.updatePincode(pin)
                                        addresseeFocusRequester.requestFocus()
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "📌 $pin",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Addressee Name (Capital Letters) with Checkmark, Clear & Active Voice Highlight
                    OutlinedTextField(
                        value = formState.addressee,
                        onValueChange = { viewModel.updateAddressee(it.uppercase()) },
                        label = { Text("Name of Addressee (CAPITAL) *") },
                        placeholder = { Text("ENTER RECIPIENT NAME") },
                        singleLine = true,
                        colors = voiceHighlightColors("ADDRESSEE"),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAddresseeValid) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid Addressee",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateAddressee("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Addressee",
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { startVoiceInput("ADDRESSEE", "Speak Recipient Name (English, Hindi, Bengali)") },
                                    modifier = Modifier.testTag("voice_addressee_button")
                                ) {
                                    val isNameListening = voiceController.isListening && voiceController.activeTarget == "ADDRESSEE"
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input Addressee",
                                        tint = if (isNameListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(addresseeFocusRequester)
                            .testTag("addressee_input"),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { weightFocusRequester.requestFocus() }
                        )
                    )

                    // 4. Frequent / Recent Addressee 1-Tap Chips (from current batch)
                    if (recentAddressees.isNotEmpty() && formState.addressee.isBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            recentAddressees.forEach { recName ->
                                Surface(
                                    onClick = {
                                        viewModel.updateAddressee(recName)
                                        weightFocusRequester.requestFocus()
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = recName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Form Section 2: Physical Measurements
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header + Modern FilterChip Lock Toggles (#6)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Weight & Dimensions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = formState.lockWeight,
                                onClick = { viewModel.toggleLockWeight(!formState.lockWeight) },
                                label = { Text("Fix Wt", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (formState.lockWeight) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("lock_weight_checkbox")
                            )

                            FilterChip(
                                selected = formState.lockDimensions,
                                onClick = { viewModel.toggleLockDimensions(!formState.lockDimensions) },
                                label = { Text("Fix L×B×H", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (formState.lockDimensions) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("lock_dimensions_checkbox")
                            )
                        }
                    }

                    // Quick Weight Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wt:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        val weightPresets = if (weightUnit.lowercase() == "g") {
                            listOf("100", "250", "500", "1000", "2000")
                        } else {
                            listOf("0.2", "0.5", "1.0", "2.0", "5.0")
                        }
                        weightPresets.forEach { preset ->
                            val isSelectedPreset = formState.weight == preset
                            Surface(
                                onClick = { viewModel.updateWeight(preset) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelectedPreset) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$preset $weightUnit",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelectedPreset) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Weight Field with Checkmark, Clear & Active Voice Highlight
                    OutlinedTextField(
                        value = formState.weight,
                        onValueChange = { viewModel.updateWeight(it) },
                        label = { Text("Physical Weight ($weightUnit) *") },
                        placeholder = { Text(if (weightUnit.lowercase() == "g") "e.g. 500" else "e.g. 0.5") },
                        singleLine = true,
                        colors = voiceHighlightColors("WEIGHT"),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isWeightValid) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid Weight",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                if (formState.weight.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.updateWeight("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Weight",
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { startVoiceInput("WEIGHT", "Speak Physical Weight (English, Hindi, Bengali)") },
                                    modifier = Modifier.testTag("voice_weight_button")
                                ) {
                                    val isWtListening = voiceController.isListening && voiceController.activeTarget == "WEIGHT"
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input Weight",
                                        tint = if (isWtListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(weightFocusRequester)
                            .testTag("weight_input"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { lengthFocusRequester.requestFocus() }
                        )
                    )

                    // Compact Dimension Presets Row + Collapsible Search Box (#2)
                    val customDimensionPresets by viewModel.dimensionPresets.collectAsStateWithLifecycle()
                    var presetSearchQuery by remember { mutableStateOf("") }

                    val filteredPresets = remember(customDimensionPresets, presetSearchQuery) {
                        if (presetSearchQuery.isBlank()) {
                            customDimensionPresets
                        } else {
                            val q = presetSearchQuery.trim().lowercase()
                            customDimensionPresets.filter { p ->
                                p.name.lowercase().contains(q) ||
                                    "${p.length.toInt()}x${p.breadth.toInt()}x${p.height.toInt()}".lowercase().contains(q) ||
                                    "${p.length.toInt()} ${p.breadth.toInt()} ${p.height.toInt()}".lowercase().contains(q)
                            }
                        }
                    }

                    val lengthVal = formState.length.toDoubleOrNull() ?: 0.0
                    val breadthVal = formState.breadth.toDoubleOrNull() ?: 0.0
                    val heightVal = formState.height.toDoubleOrNull() ?: 0.0

                    val isLengthExceeded = maxLength > 0.0 && lengthVal > maxLength
                    val isBreadthExceeded = maxBreadth > 0.0 && breadthVal > maxBreadth
                    val isHeightExceeded = maxHeight > 0.0 && heightVal > maxHeight

                    val isAllDimListening = voiceController.isListening && voiceController.activeTarget == "ALL_DIMENSIONS"

                    // Dimensions Header + Search Toggle + Voice All Dimensions Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dimensions ($dimensionUnit):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                onClick = { showPresetSearchBox = !showPresetSearchBox },
                                shape = RoundedCornerShape(50),
                                color = if (showPresetSearchBox) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Box Presets",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Search Box",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                onClick = { startVoiceInput("ALL_DIMENSIONS", "Speak L B H e.g. 10 20 15") },
                                shape = RoundedCornerShape(50),
                                color = if (isAllDimListening) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                                modifier = Modifier.testTag("voice_all_dimensions_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input All Dimensions",
                                        tint = if (isAllDimListening) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Speak L×B×H",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAllDimListening) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showPresetSearchBox,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = presetSearchQuery,
                            onValueChange = { presetSearchQuery = it },
                            placeholder = { Text("🔍 Search preset (e.g. Shoe Box, Envelope)...", fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Presets",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (presetSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { presetSearchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Search",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("preset_search_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Predictive Suggestion Chips (1 Compact Row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        filteredPresets.take(4).forEach { preset ->
                            Surface(
                                onClick = {
                                    viewModel.updateLength(if (preset.length % 1.0 == 0.0) preset.length.toInt().toString() else preset.length.toString())
                                    viewModel.updateBreadth(if (preset.breadth % 1.0 == 0.0) preset.breadth.toInt().toString() else preset.breadth.toString())
                                    viewModel.updateHeight(if (preset.height % 1.0 == 0.0) preset.height.toInt().toString() else preset.height.toString())
                                    presetSearchQuery = ""
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = preset.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "${preset.length.toInt()}×${preset.breadth.toInt()}×${preset.height.toInt()} $dimensionUnit",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // L × B × H Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.length,
                            onValueChange = { viewModel.updateLength(it) },
                            label = { Text(if (maxLength > 0.0) "L (≤${maxLength.toInt()})" else "Length") },
                            placeholder = { Text("0.0") },
                            singleLine = true,
                            isError = isLengthExceeded,
                            colors = voiceHighlightColors("ALL_DIMENSIONS"),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(lengthFocusRequester)
                                .testTag("length_input"),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { breadthFocusRequester.requestFocus() }
                            )
                        )

                        OutlinedTextField(
                            value = formState.breadth,
                            onValueChange = { viewModel.updateBreadth(it) },
                            label = { Text(if (maxBreadth > 0.0) "B (≤${maxBreadth.toInt()})" else "Breadth") },
                            placeholder = { Text("0.0") },
                            singleLine = true,
                            isError = isBreadthExceeded,
                            colors = voiceHighlightColors("ALL_DIMENSIONS"),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(breadthFocusRequester)
                                .testTag("breadth_input"),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { heightFocusRequester.requestFocus() }
                            )
                        )

                        OutlinedTextField(
                            value = formState.height,
                            onValueChange = { viewModel.updateHeight(it) },
                            label = { Text(if (maxHeight > 0.0) "H (≤${maxHeight.toInt()})" else "Height") },
                            placeholder = { Text("0.0") },
                            singleLine = true,
                            isError = isHeightExceeded,
                            colors = voiceHighlightColors("ALL_DIMENSIONS"),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(heightFocusRequester)
                                .testTag("height_input"),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.saveParcel(onSuccess = {})
                                }
                            )
                        )
                    }

                    // 5. Smart Chargeable Weight Comparison Card (Physical vs Volumetric Weight)
                    if (lengthVal > 0.0 && breadthVal > 0.0 && heightVal > 0.0) {
                        val divisor = if (volumetricDivisor > 0.0) volumetricDivisor else 5000.0
                        val volWeightKg = (lengthVal * breadthVal * heightVal) / divisor
                        val isGramUnit = weightUnit.lowercase() == "g"
                        val volWeightInActiveUnit = if (isGramUnit) volWeightKg * 1000.0 else volWeightKg
                        val physicalInKg = if (isGramUnit) weightNumeric / 1000.0 else weightNumeric
                        val isVolumetricHigher = volWeightKg > physicalInKg + 0.001

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("volumetric_weight_card"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isVolumetricHigher) {
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isVolumetricHigher) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = if (isVolumetricHigher) {
                                                MaterialTheme.colorScheme.tertiary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isVolumetricHigher) {
                                                "Volumetric is Higher (Chargeable)"
                                            } else {
                                                "Volumetric Weight ((L×B×H)/${divisor.toInt()})"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%.3f kg (%d g)",
                                            volWeightKg,
                                            (volWeightKg * 1000.0).toInt()
                                        ),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Surface(
                                    onClick = {
                                        val formattedVol = if (isGramUnit) {
                                            (volWeightInActiveUnit).toInt().toString()
                                        } else {
                                            String.format(Locale.US, "%.2f", volWeightInActiveUnit)
                                        }
                                        viewModel.updateWeight(formattedVol)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("apply_volumetric_weight_button")
                                ) {
                                    Text(
                                        text = "Use Volumetric",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Camera Scanner Sheet/Dialog
    if (showCameraScanner) {
        val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
        val soundEnabled by viewModel.scanSoundEnabled.collectAsStateWithLifecycle()
        val defaultModeStr by viewModel.defaultScanModeStr.collectAsStateWithLifecycle()
        val batchBarcodes by viewModel.batchBarcodes.collectAsStateWithLifecycle()
        val initialMode = if (defaultModeStr == "CONTINUOUS") com.example.scanner.ScanMode.CONTINUOUS else com.example.scanner.ScanMode.SINGLE

        com.example.scanner.AdvancedBarcodeScannerScreen(
            batchCode = currentBatch?.batchCode ?: "ACTIVE",
            savedParcelCount = batchBarcodes.size,
            initialScanMode = initialMode,
            hapticFeedbackEnabled = hapticEnabled,
            soundFeedbackEnabled = soundEnabled,
            existingBarcodes = batchBarcodes,
            onDuplicateDetected = { dupCode ->
                viewModel.updateBarcode(dupCode)
                showCameraScanner = false
            },
            onBarcodeScanned = { scannedCode, isContinuous ->
                viewModel.updateBarcode(scannedCode)
                if (!isContinuous) {
                    showCameraScanner = false
                    pincodeFocusRequester.requestFocus()
                } else {
                    pincodeFocusRequester.requestFocus()
                }
            },
            onCloseScanner = { showCameraScanner = false }
        )
    }

    // Address Photo Scanner Dialog
    if (showAddressPhotoScanner) {
        AddressPhotoScannerDialog(
            onAddressExtracted = { name, pincode, photoUri, _ ->
                viewModel.onAddressPhotoScanned(name, pincode, photoUri)
                showAddressPhotoScanner = false
                if (pincode.isNullOrBlank()) {
                    pincodeFocusRequester.requestFocus()
                } else if (name.isNullOrBlank()) {
                    addresseeFocusRequester.requestFocus()
                } else {
                    weightFocusRequester.requestFocus()
                }
            },
            onDismiss = { showAddressPhotoScanner = false }
        )
    }

    // Duplicate Warning Dialog
    formState.duplicateWarningParcel?.let { duplicate ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateWarning() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duplicate Barcode Warning", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Barcode '${duplicate.barcode}' already exists in this batch!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Existing Record Details:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• Addressee: ${duplicate.addresseeName}")
                    Text("• Pincode: ${duplicate.destinationPincode}")
                    Text("• Weight: ${duplicate.physicalWeight} $weightUnit")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissDuplicateWarning()
                        currentBatch?.let { batch -> onViewBatchHistory(batch.id) }
                    },
                    modifier = Modifier.testTag("view_existing_record_button")
                ) {
                    Text("View Existing Record")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDuplicateWarning() },
                    modifier = Modifier.testTag("cancel_duplicate_entry_button")
                ) {
                    Text("Cancel New Entry")
                }
            }
        )
    }
}
