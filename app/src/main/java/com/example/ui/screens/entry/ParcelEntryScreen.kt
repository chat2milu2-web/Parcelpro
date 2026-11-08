package com.example.ui.screens.entry

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import com.example.util.VoiceInputHelper
import androidx.compose.material3.AlertDialog
import coil.compose.AsyncImage
import com.example.scanner.AddressPhotoScannerDialog
import com.example.ui.components.AnimatedGradientBorderCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scanner.CameraBarcodeScannerDialog
import kotlinx.coroutines.launch

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

    val pincodeLength by viewModel.pincodeLength.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val dimensionUnit by viewModel.dimensionUnit.collectAsStateWithLifecycle()
    val maxLength by viewModel.maxLength.collectAsStateWithLifecycle()
    val maxBreadth by viewModel.maxBreadth.collectAsStateWithLifecycle()
    val maxHeight by viewModel.maxHeight.collectAsStateWithLifecycle()
    val volumetricDivisor by viewModel.volumetricDivisor.collectAsStateWithLifecycle()

    var showCameraScanner by remember { mutableStateOf(false) }
    var showAddressPhotoScanner by remember { mutableStateOf(false) }

    var pendingVoiceTarget by remember { mutableStateOf<String?>(null) }
    val voiceSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenResults = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenResults?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                when (pendingVoiceTarget) {
                    "PINCODE" -> {
                        val parsed = VoiceInputHelper.parseVoiceResult(spokenText, isNumericField = true).take(pincodeLength)
                        viewModel.updatePincode(parsed)
                    }
                    "ADDRESSEE" -> {
                        val parsed = VoiceInputHelper.parseVoiceResult(spokenText, isNumericField = false)
                        viewModel.updateAddressee(parsed)
                    }
                    "WEIGHT" -> {
                        val parsed = VoiceInputHelper.parseVoiceResult(spokenText, isNumericField = true)
                        viewModel.updateWeight(parsed)
                    }
                    "LENGTH" -> {
                        val parsed = VoiceInputHelper.parseVoiceResult(spokenText, isNumericField = true)
                        viewModel.updateLength(parsed)
                    }
                    "BREADTH" -> {
                        val parsed = VoiceInputHelper.parseVoiceResult(spokenText, isNumericField = true)
                        viewModel.updateBreadth(parsed)
                    }
                    "HEIGHT" -> {
                        val parsed = VoiceInputHelper.parseVoiceResult(spokenText, isNumericField = true)
                        viewModel.updateHeight(parsed)
                    }
                    "ALL_DIMENSIONS" -> {
                        val numbers = VoiceInputHelper.extractMultipleNumbers(spokenText)
                        if (numbers.isNotEmpty()) viewModel.updateLength(numbers[0])
                        if (numbers.size > 1) viewModel.updateBreadth(numbers[1])
                        if (numbers.size > 2) viewModel.updateHeight(numbers[2])
                    }
                }
            }
        }
    }

    val startVoiceInput: (String, String) -> Unit = { target, promptMsg ->
        pendingVoiceTarget = target
        try {
            val intent = VoiceInputHelper.createSpeechIntent(promptMsg)
            voiceSpeechLauncher.launch(intent)
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Voice input service unavailable on device.")
            }
        }
    }

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
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isFormReady = !formState.addressee.isBlank() && !formState.pincode.isBlank()

                    // Sticky Save & Next Parcel Button (Always Visible - No Scroll Required!)
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
                            .height(52.dp)
                            .testTag("save_next_button")
                    ) {
                        Icon(
                            imageVector = if (isFormReady) Icons.Default.CheckCircle else Icons.Default.Save,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFormReady) "✓ READY TO SAVE & NEXT PARCEL" else "SAVE & NEXT PARCEL",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick Actions Row
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
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save & View Batch", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.clearForm()
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Form", fontSize = 12.sp)
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
                            Text("New Parcel Entry", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                text = "Batch: ${currentBatch?.batchCode ?: "Loading..."}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Message Banner
            formState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp,
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
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = warning,
                            color = Color(0xFF92400E),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissCrossBatchWarning() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Identification & Destination",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Barcode Field with Scanner Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = formState.barcode,
                            onValueChange = { viewModel.updateBarcode(it) },
                            label = { Text("Barcode Number (13 Chars) *") },
                            placeholder = { Text("e.g. EW112223445IN") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(barcodeFocusRequester)
                                .testTag("barcode_input"),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { pincodeFocusRequester.requestFocus() }
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

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
                                .height(56.dp)
                                .testTag("camera_scan_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode"
                            )
                        }
                    }

                    // Prominent Address Photo Scanner Button
                    Button(
                        onClick = { showAddressPhotoScanner = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("scan_address_photo_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📷 Scan Address Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Scanned Image Preview Card
                    formState.addressPhotoUri?.let { uriStr ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = uriStr,
                                    contentDescription = "Scanned Address Label",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Address Photo Scanned",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                    formState.ocrNotice?.let { notice ->
                                        Text(
                                            text = notice,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.clearAddressPhoto() },
                                    modifier = Modifier.testTag("clear_address_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove photo",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Destination Pincode
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
                        placeholder = { Text("e.g. 110001") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { startVoiceInput("PINCODE", "Speak Pincode (English, Hindi, Bengali)") },
                                modifier = Modifier.testTag("voice_pincode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input Pincode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
                                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("pincode_resolved_location_badge")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Verified Area: $locationName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }

                    // Addressee Name (Capital Letters)
                    OutlinedTextField(
                        value = formState.addressee,
                        onValueChange = { viewModel.updateAddressee(it.uppercase()) },
                        label = { Text("Name of Addressee (CAPITAL) *") },
                        placeholder = { Text("ENTER RECIPIENT NAME") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { startVoiceInput("ADDRESSEE", "Speak Recipient Name (English, Hindi, Bengali)") },
                                modifier = Modifier.testTag("voice_addressee_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input Addressee",
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "2. Weight & Dimensions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { viewModel.toggleLockWeight(!formState.lockWeight) }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = formState.lockWeight,
                                onCheckedChange = { viewModel.toggleLockWeight(it) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("lock_weight_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fix Weight", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { viewModel.toggleLockDimensions(!formState.lockDimensions) }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = formState.lockDimensions,
                                onCheckedChange = { viewModel.toggleLockDimensions(it) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("lock_dimensions_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fix Dimensions", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Quick Weight Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Presets:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        listOf("0.2", "0.5", "1.0", "2.0", "5.0").forEach { preset ->
                            Surface(
                                onClick = { viewModel.updateWeight(preset) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$preset $weightUnit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Weight Field
                    OutlinedTextField(
                        value = formState.weight,
                        onValueChange = { viewModel.updateWeight(it) },
                        label = { Text("Physical Weight ($weightUnit) *") },
                        placeholder = { Text("e.g. 0.5") },
                        singleLine = true,
                        trailingIcon = {
                            Row {
                                if (formState.weight.isNotBlank()) {
                                    IconButton(onClick = { viewModel.updateWeight("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Weight", modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { startVoiceInput("WEIGHT", "Speak Physical Weight (English, Hindi, Bengali)") },
                                    modifier = Modifier.testTag("voice_weight_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input Weight",
                                        tint = MaterialTheme.colorScheme.primary
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

                    // Google Search-Style Predictive Dimension Presets Bar
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

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = presetSearchQuery,
                            onValueChange = { presetSearchQuery = it },
                            placeholder = { Text("🔍 Search preset (e.g. Shoe Box, Envelope)...", fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Search,
                                    contentDescription = "Search Presets",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (presetSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { presetSearchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("preset_search_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Predictive Suggestion Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
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
                    }

                    // Dimensions Row Header with Voice Mic button
                    val lengthVal = formState.length.toDoubleOrNull() ?: 0.0
                    val breadthVal = formState.breadth.toDoubleOrNull() ?: 0.0
                    val heightVal = formState.height.toDoubleOrNull() ?: 0.0

                    val isLengthExceeded = maxLength > 0.0 && lengthVal > maxLength
                    val isBreadthExceeded = maxBreadth > 0.0 && breadthVal > maxBreadth
                    val isHeightExceeded = maxHeight > 0.0 && heightVal > maxHeight

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dimensions ($dimensionUnit):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { startVoiceInput("ALL_DIMENSIONS", "Speak L B H e.g. 10 20 15") },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("voice_all_dimensions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input All Dimensions",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Volumetric Weight Calculation Display
                    if (lengthVal > 0.0 && breadthVal > 0.0 && heightVal > 0.0) {
                        val divisor = if (volumetricDivisor > 0.0) volumetricDivisor else 5000.0
                        val volWeightKg = (lengthVal * breadthVal * heightVal) / divisor
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("volumetric_weight_card"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "📦 Volumetric Weight:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = "Rule: (L × B × H) / ${divisor.toInt()}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                                Text(
                                    text = String.format(java.util.Locale.US, "%.3f kg", volWeightKg),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.length,
                            onValueChange = { viewModel.updateLength(it) },
                            label = { Text(if (maxLength > 0.0) "L (Max $maxLength)" else "Length") },
                            placeholder = { Text("0.0") },
                            singleLine = true,
                            isError = isLengthExceeded,
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
                            label = { Text(if (maxBreadth > 0.0) "B (Max $maxBreadth)" else "Breadth") },
                            placeholder = { Text("0.0") },
                            singleLine = true,
                            isError = isBreadthExceeded,
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
                            label = { Text(if (maxHeight > 0.0) "H (Max $maxHeight)" else "Height") },
                            placeholder = { Text("0.0") },
                            singleLine = true,
                            isError = isHeightExceeded,
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
