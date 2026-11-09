package com.example.ui.screens.secondary

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import coil.compose.AsyncImage
import com.example.scanner.AddressPhotoScannerDialog
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Mic
import com.example.util.InlineVoiceListeningBanner
import com.example.util.VoiceInputHelper
import com.example.util.rememberInlineVoiceController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryPincodeStageScreen(
    batchId: Long,
    viewModel: SecondaryBatchViewModel,
    onNavigateBack: () -> Unit,
    onProceedToNextStage: () -> Unit
) {
    LaunchedEffect(batchId) {
        viewModel.loadBatch(batchId)
    }

    val parcels by viewModel.parcels.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentParcelIndex.collectAsStateWithLifecycle()
    val pincodeLength by viewModel.pincodeLength.collectAsStateWithLifecycle()
    val pincodeCompletedCount by viewModel.pincodeCompletedCount.collectAsStateWithLifecycle()

    val currentParcel = parcels.getOrNull(currentIndex)
    var pincodeText by remember(currentParcel?.id) {
        mutableStateOf(currentParcel?.destinationPincode ?: "")
    }
    var showAddressScanner by remember { mutableStateOf(false) }
    var scannedPhotoUri by remember(currentParcel?.id) { mutableStateOf<Uri?>(null) }

    var showJumpDialog by remember { mutableStateOf(false) }

    val voiceController = rememberInlineVoiceController(
        onCandidatesReceived = { _, candidates, isFinal ->
            val parsed = VoiceInputHelper.parseVoiceCandidates(candidates, isNumericField = true).take(pincodeLength)
            if (parsed.isNotBlank()) {
                pincodeText = parsed
                if (isFinal && parsed.length == pincodeLength && currentParcel != null) {
                    viewModel.savePincode(currentParcel.id, parsed)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stage 2: Destination Pincode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("pincode_stage_back_button")
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
                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = "Jump to Parcel",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (parcels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No barcodes in batch. Please complete Stage 1 first.")
            }
        } else if (currentParcel != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InlineVoiceListeningBanner(controller = voiceController)

                // Progress Header Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pincodes Entered: $pincodeCompletedCount / ${parcels.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Parcel ${currentIndex + 1} of ${parcels.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val prog = if (parcels.isNotEmpty()) pincodeCompletedCount.toFloat() / parcels.size.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { prog },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Parcel Identification Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "PARCEL #${String.format("%02d", currentParcel.sequenceNumber)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            if (currentParcel.pincodeCompleted) {
                                Surface(
                                    color = Color(0xFF16A34A),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "PINCODE SAVED",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Barcode Number:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentParcel.barcode,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Scan Address Photo Button
                        Button(
                            onClick = { showAddressScanner = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pincode_stage_scan_address_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📷 Scan Address Photo", fontWeight = FontWeight.Bold)
                        }

                        scannedPhotoUri?.let { uri ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Address Photo",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Address Photo Scanned", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF16A34A))
                                        Text("Pincode auto-filled. You can edit below.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { scannedPhotoUri = null }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = pincodeText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(pincodeLength)
                                pincodeText = filtered
                                if (filtered.length == pincodeLength) {
                                    viewModel.savePincode(currentParcel.id, filtered)
                                }
                            },
                            label = { Text("Destination Pincode ($pincodeLength Digits) *") },
                            placeholder = { Text("e.g. 110001") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        voiceController.startListening(
                                            "PINCODE",
                                            "Speak Destination Pincode (English, বাংলা, हिन्दी)"
                                        )
                                    },
                                    modifier = Modifier.testTag("voice_pincode_stage_button")
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
                                .testTag("pincode_stage_input"),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.savePincode(currentParcel.id, pincodeText)
                                }
                            )
                        )
                    }
                }

                // Controls Row
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.savePincode(currentParcel.id, pincodeText)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pincode_save_next_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Next Parcel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.previousParcel() },
                            enabled = currentIndex > 0,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Previous")
                        }

                        OutlinedButton(
                            onClick = { viewModel.nextParcel() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Skip / Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (pincodeCompletedCount == parcels.size) {
                        Button(
                            onClick = onProceedToNextStage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pincode_proceed_addressee_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Text("All Pincodes Done • Proceed to Stage 3: Addressee", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Jump to Parcel Dialog
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Parcel Sequence", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    parcels.forEachIndexed { idx, p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Parcel #${String.format("%02d", p.sequenceNumber)}: ${p.barcode}")
                            Button(
                                onClick = {
                                    viewModel.setParcelIndex(idx)
                                    showJumpDialog = false
                                }
                            ) {
                                Text("Select")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddressScanner && currentParcel != null) {
        AddressPhotoScannerDialog(
            onAddressExtracted = { name, pin, uri, _ ->
                scannedPhotoUri = uri
                if (!pin.isNullOrBlank()) {
                    val filtered = pin.filter { it.isDigit() }.take(pincodeLength)
                    pincodeText = filtered
                }
                viewModel.saveOcrAddressInfo(currentParcel.id, name, pin)
                showAddressScanner = false
            },
            onDismiss = { showAddressScanner = false }
        )
    }
}
