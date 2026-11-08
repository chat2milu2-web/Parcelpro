package com.example.ui.screens.secondary

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entities.ParcelEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryBatchReviewScreen(
    batchId: Long,
    viewModel: SecondaryBatchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToExport: (Long) -> Unit
) {
    LaunchedEffect(batchId) {
        viewModel.loadBatch(batchId)
    }

    val currentBatch by viewModel.currentBatch.collectAsStateWithLifecycle()
    val parcels by viewModel.parcels.collectAsStateWithLifecycle()
    val fullyCompletedCount by viewModel.fullyCompletedCount.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val dimensionUnit by viewModel.dimensionUnit.collectAsStateWithLifecycle()

    val total = parcels.size
    val incompleteCount = total - fullyCompletedCount

    var parcelToEdit by remember { mutableStateOf<ParcelEntity?>(null) }
    var showIncompleteExportWarning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stage 5: Final Batch Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("review_stage_back_button")
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
        ) {
            // Batch Review Summary Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Batch Review: ${currentBatch?.batchCode ?: ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Fully Completed: $fullyCompletedCount / $total | Incomplete: $incompleteCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (incompleteCount > 0) MaterialTheme.colorScheme.error else Color(0xFF16A34A)
                            )
                        }

                        Button(
                            onClick = {
                                if (incompleteCount > 0) {
                                    showIncompleteExportWarning = true
                                } else {
                                    onNavigateToExport(batchId)
                                }
                            },
                            modifier = Modifier.testTag("review_export_excel_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (incompleteCount == 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Excel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Sequence Table List
            if (parcels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records found in this batch.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(parcels, key = { it.id }) { parcel ->
                        ReviewParcelCardItem(
                            parcel = parcel,
                            weightUnit = weightUnit,
                            dimensionUnit = dimensionUnit,
                            onEdit = { parcelToEdit = parcel }
                        )
                    }
                }
            }
        }
    }

    // Edit Parcel Dialog
    parcelToEdit?.let { parcel ->
        EditReviewParcelDialog(
            parcel = parcel,
            weightUnit = weightUnit,
            dimensionUnit = dimensionUnit,
            onDismiss = { parcelToEdit = null },
            onConfirmSave = { updated ->
                viewModel.updateParcelFull(updated)
                parcelToEdit = null
            }
        )
    }

    // Incomplete Export Warning Dialog
    if (showIncompleteExportWarning) {
        AlertDialog(
            onDismissRequest = { showIncompleteExportWarning = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Incomplete Records Warning", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("This batch contains $incompleteCount incomplete parcel records. Are you sure you want to export to Excel anyway?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIncompleteExportWarning = false
                        onNavigateToExport(batchId)
                    }
                ) {
                    Text("Export Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncompleteExportWarning = false }) {
                    Text("Review Incomplete")
                }
            }
        )
    }
}

@Composable
private fun ReviewParcelCardItem(
    parcel: ParcelEntity,
    weightUnit: String,
    dimensionUnit: String,
    onEdit: () -> Unit
) {
    val isComplete = parcel.isFullyCompleted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_item_${parcel.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = String.format("%02d", parcel.sequenceNumber),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = parcel.barcode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Incomplete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Row", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pincode: ${parcel.destinationPincode.ifBlank { "MISSING" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (parcel.pincodeCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Addressee: ${parcel.addresseeName.ifBlank { "MISSING" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (parcel.addresseeCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Weight: ${if (parcel.physicalWeight > 0.0) "${parcel.physicalWeight} $weightUnit" else "MISSING"}",
                    fontSize = 12.sp,
                    color = if (parcel.measurementsCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Dims: ${parcel.length} × ${parcel.breadth} × ${parcel.height} $dimensionUnit",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditReviewParcelDialog(
    parcel: ParcelEntity,
    weightUnit: String,
    dimensionUnit: String,
    onDismiss: () -> Unit,
    onConfirmSave: (ParcelEntity) -> Unit
) {
    var barcode by remember { mutableStateOf(parcel.barcode) }
    var pincode by remember { mutableStateOf(parcel.destinationPincode) }
    var addressee by remember { mutableStateOf(parcel.addresseeName) }
    var weight by remember { mutableStateOf(if (parcel.physicalWeight > 0.0) parcel.physicalWeight.toString() else "") }
    var length by remember { mutableStateOf(if (parcel.length > 0.0) parcel.length.toString() else "") }
    var breadth by remember { mutableStateOf(if (parcel.breadth > 0.0) parcel.breadth.toString() else "") }
    var height by remember { mutableStateOf(if (parcel.height > 0.0) parcel.height.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Parcel #${parcel.sequenceNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = { Text("Pincode (6 Digits)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = addressee,
                    onValueChange = { addressee = it.uppercase() },
                    label = { Text("Addressee Name (CAPITAL)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight ($weightUnit)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = length,
                        onValueChange = { length = it },
                        label = { Text("L ($dimensionUnit)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = breadth,
                        onValueChange = { breadth = it },
                        label = { Text("B ($dimensionUnit)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("H ($dimensionUnit)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toDoubleOrNull() ?: 0.0
                    val l = length.toDoubleOrNull() ?: 0.0
                    val b = breadth.toDoubleOrNull() ?: 0.0
                    val h = height.toDoubleOrNull() ?: 0.0

                    val updated = parcel.copy(
                        barcode = barcode.trim(),
                        destinationPincode = pincode.trim(),
                        addresseeName = addressee.trim().uppercase(),
                        physicalWeight = w,
                        length = l,
                        breadth = b,
                        height = h,
                        updatedAt = System.currentTimeMillis()
                    )
                    onConfirmSave(updated)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
