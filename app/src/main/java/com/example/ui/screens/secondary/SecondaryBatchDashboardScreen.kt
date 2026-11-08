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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryBatchDashboardScreen(
    batchId: Long,
    viewModel: SecondaryBatchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBarcodeStage: () -> Unit,
    onNavigateToPincodeStage: () -> Unit,
    onNavigateToAddresseeStage: () -> Unit,
    onNavigateToMeasurementsStage: () -> Unit,
    onNavigateToReviewStage: () -> Unit
) {
    LaunchedEffect(batchId) {
        viewModel.loadBatch(batchId)
    }

    val currentBatch by viewModel.currentBatch.collectAsStateWithLifecycle()
    val parcels by viewModel.parcels.collectAsStateWithLifecycle()
    val pincodeCount by viewModel.pincodeCompletedCount.collectAsStateWithLifecycle()
    val addresseeCount by viewModel.addresseeCompletedCount.collectAsStateWithLifecycle()
    val measurementsCount by viewModel.measurementsCompletedCount.collectAsStateWithLifecycle()
    val fullyCompletedCount by viewModel.fullyCompletedCount.collectAsStateWithLifecycle()

    val total = parcels.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Field-Wise Batch Entry", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        modifier = Modifier.testTag("secondary_dashboard_back_button")
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
                )
            )
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
            // Batch Overview Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SECONDARY MODE BATCH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentBatch?.batchCode ?: "YYB-BATCH",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$total Parcels Total",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Resume Next Stage Prominent Button
                    val nextStageAction = when {
                        total == 0 -> onNavigateToBarcodeStage
                        pincodeCount < total -> onNavigateToPincodeStage
                        addresseeCount < total -> onNavigateToAddresseeStage
                        measurementsCount < total -> onNavigateToMeasurementsStage
                        else -> onNavigateToReviewStage
                    }

                    val nextStageTitle = when {
                        total == 0 -> "Start Stage 1: Scan Barcodes"
                        pincodeCount < total -> "Resume Stage 2: Enter Pincodes ($pincodeCount/$total)"
                        addresseeCount < total -> "Resume Stage 3: Enter Addressees ($addresseeCount/$total)"
                        measurementsCount < total -> "Resume Stage 4: Enter Measurements ($measurementsCount/$total)"
                        else -> "Open Stage 5: Final Review & Export"
                    }

                    Button(
                        onClick = nextStageAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("resume_next_stage_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(nextStageTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Text(
                text = "Field-Wise Workflow Stages",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Stage 1: Barcode Entry
            StageCardItem(
                stageNumber = 1,
                title = "1. Barcode Entry Stage",
                subtitle = "Scan or enter all barcodes continuously in sequence",
                completedCount = total,
                totalCount = total,
                icon = Icons.Default.QrCodeScanner,
                color = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToBarcodeStage,
                testTag = "stage_1_card"
            )

            // Stage 2: Destination Pincode
            StageCardItem(
                stageNumber = 2,
                title = "2. Destination Pincode Stage",
                subtitle = "Enter 6-digit PIN codes for each parcel in sequence order",
                completedCount = pincodeCount,
                totalCount = total,
                icon = Icons.Default.LocationOn,
                color = Color(0xFF0284C7),
                onClick = onNavigateToPincodeStage,
                testTag = "stage_2_card"
            )

            // Stage 3: Addressee Name
            StageCardItem(
                stageNumber = 3,
                title = "3. Addressee Name Stage",
                subtitle = "Enter recipient names in original barcode sequence",
                completedCount = addresseeCount,
                totalCount = total,
                icon = Icons.Default.Person,
                color = Color(0xFF7C3AED),
                onClick = onNavigateToAddresseeStage,
                testTag = "stage_3_card"
            )

            // Stage 4: Physical Measurements
            StageCardItem(
                stageNumber = 4,
                title = "4. Physical Measurements Stage",
                subtitle = "Enter Weight (g) & Dimensions (L × B × H in cm) together",
                completedCount = measurementsCount,
                totalCount = total,
                icon = Icons.Default.Scale,
                color = Color(0xFFEA580C),
                onClick = onNavigateToMeasurementsStage,
                testTag = "stage_4_card"
            )

            // Stage 5: Final Review & Export
            Card(
                onClick = onNavigateToReviewStage,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stage_5_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color(0xFF16A34A)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "5. Final Review & Excel Export",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Review sequence integrity & export .xlsx report",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Fully Completed: $fullyCompletedCount / $total parcels",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StageCardItem(
    stageNumber: Int,
    title: String,
    subtitle: String,
    completedCount: Int,
    totalCount: Int,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val isComplete = totalCount > 0 && completedCount == totalCount

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = color)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isComplete) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Complete",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress: $completedCount / $totalCount Parcels",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}
