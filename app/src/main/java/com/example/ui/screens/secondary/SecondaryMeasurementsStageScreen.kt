package com.example.ui.screens.secondary

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
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
fun SecondaryMeasurementsStageScreen(
    batchId: Long,
    viewModel: SecondaryBatchViewModel,
    onNavigateBack: () -> Unit,
    onProceedToReviewStage: () -> Unit
) {
    LaunchedEffect(batchId) {
        viewModel.loadBatch(batchId)
    }

    val parcels by viewModel.parcels.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentParcelIndex.collectAsStateWithLifecycle()
    val measurementsCompletedCount by viewModel.measurementsCompletedCount.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val dimensionUnit by viewModel.dimensionUnit.collectAsStateWithLifecycle()
    val maxLength by viewModel.maxLength.collectAsStateWithLifecycle()
    val maxBreadth by viewModel.maxBreadth.collectAsStateWithLifecycle()
    val maxHeight by viewModel.maxHeight.collectAsStateWithLifecycle()
    val volumetricDivisor by viewModel.volumetricDivisor.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val currentParcel = parcels.getOrNull(currentIndex)

    var weightText by remember(currentParcel?.id) {
        mutableStateOf(if (currentParcel != null && currentParcel.physicalWeight > 0.0) currentParcel.physicalWeight.toString() else "")
    }
    var lengthText by remember(currentParcel?.id) {
        mutableStateOf(if (currentParcel != null && currentParcel.length > 0.0) currentParcel.length.toString() else "")
    }
    var breadthText by remember(currentParcel?.id) {
        mutableStateOf(if (currentParcel != null && currentParcel.breadth > 0.0) currentParcel.breadth.toString() else "")
    }
    var heightText by remember(currentParcel?.id) {
        mutableStateOf(if (currentParcel != null && currentParcel.height > 0.0) currentParcel.height.toString() else "")
    }

    var showJumpDialog by remember { mutableStateOf(false) }

    val voiceController = rememberInlineVoiceController(
        onCandidatesReceived = { target, candidates, _ ->
            when (target) {
                "WEIGHT" -> {
                    val parsed = VoiceInputHelper.parseVoiceCandidates(candidates, isNumericField = true)
                    if (parsed.isNotBlank()) weightText = parsed
                }
                "ALL_DIMENSIONS" -> {
                    val numbers = VoiceInputHelper.extractMultipleNumbersFromCandidates(candidates)
                    if (numbers.isNotEmpty()) lengthText = numbers[0]
                    if (numbers.size > 1) breadthText = numbers[1]
                    if (numbers.size > 2) heightText = numbers[2]
                }
            }
        }
    )

    val startVoiceInput: (String, String) -> Unit = { target, prompt ->
        voiceController.startListening(target, prompt)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stage 4: Measurements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("measurements_stage_back_button")
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
                                text = "Measurements Entered: $measurementsCompletedCount / ${parcels.size}",
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
                        val prog = if (parcels.isNotEmpty()) measurementsCompletedCount.toFloat() / parcels.size.toFloat() else 0f
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
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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
                            if (currentParcel.measurementsCompleted) {
                                Surface(
                                    color = Color(0xFF16A34A),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "MEASUREMENTS SAVED",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Barcode: ${currentParcel.barcode}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Weight Input
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Physical Weight ($weightUnit) *") },
                            placeholder = { Text("e.g. 250") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = { startVoiceInput("WEIGHT", "Speak Physical Weight (English, Hindi, Bengali)") },
                                    modifier = Modifier.testTag("voice_weight_stage_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input Weight",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("weight_stage_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                        )

                        // Dimensions Row (Length, Breadth, Height)
                        val lengthVal = lengthText.toDoubleOrNull() ?: 0.0
                        val breadthVal = breadthText.toDoubleOrNull() ?: 0.0
                        val heightVal = heightText.toDoubleOrNull() ?: 0.0

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
                                    .testTag("voice_all_dimensions_stage_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input All Dimensions",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Volumetric Weight Calculation
                        if (lengthVal > 0.0 && breadthVal > 0.0 && heightVal > 0.0) {
                            val divisor = if (volumetricDivisor > 0.0) volumetricDivisor else 5000.0
                            val volWeightKg = (lengthVal * breadthVal * heightVal) / divisor
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("volumetric_weight_stage_card"),
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
                                value = lengthText,
                                onValueChange = { lengthText = it },
                                label = { Text(if (maxLength > 0.0) "L (Max $maxLength)" else "Length") },
                                placeholder = { Text("0.0") },
                                singleLine = true,
                                isError = isLengthExceeded,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("length_stage_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                            )

                            OutlinedTextField(
                                value = breadthText,
                                onValueChange = { breadthText = it },
                                label = { Text(if (maxBreadth > 0.0) "B (Max $maxBreadth)" else "Breadth") },
                                placeholder = { Text("0.0") },
                                singleLine = true,
                                isError = isBreadthExceeded,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("breadth_stage_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                            )

                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it },
                                label = { Text(if (maxHeight > 0.0) "H (Max $maxHeight)" else "Height") },
                                placeholder = { Text("0.0") },
                                singleLine = true,
                                isError = isHeightExceeded,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("height_stage_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val w = weightText.toDoubleOrNull() ?: 0.0
                                        val l = lengthText.toDoubleOrNull() ?: 0.0
                                        val b = breadthText.toDoubleOrNull() ?: 0.0
                                        val h = heightText.toDoubleOrNull() ?: 0.0
                                        viewModel.saveMeasurements(currentParcel.id, w, l, b, h)
                                    }
                                )
                            )
                        }

                        if (statusMessage != null) {
                            Text(
                                text = statusMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Controls
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: 0.0
                            val l = lengthText.toDoubleOrNull() ?: 0.0
                            val b = breadthText.toDoubleOrNull() ?: 0.0
                            val h = heightText.toDoubleOrNull() ?: 0.0
                            viewModel.saveMeasurements(currentParcel.id, w, l, b, h)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("measurements_save_next_button"),
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

                    if (measurementsCompletedCount == parcels.size) {
                        Button(
                            onClick = onProceedToReviewStage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("measurements_proceed_review_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Text("All Stages Completed • Proceed to Final Review", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Jump Dialog
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
}
