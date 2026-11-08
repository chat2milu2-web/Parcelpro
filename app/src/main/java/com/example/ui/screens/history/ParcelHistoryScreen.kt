package com.example.ui.screens.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun ParcelHistoryScreen(
    batchId: Long,
    viewModel: ParcelHistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (Long) -> Unit,
    onNavigateToExport: (Long) -> Unit
) {
    LaunchedEffect(batchId) {
        viewModel.loadBatch(batchId)
    }

    val selectedBatch by viewModel.selectedBatch.collectAsStateWithLifecycle()
    val allBatches by viewModel.allBatches.collectAsStateWithLifecycle()
    val parcels by viewModel.filteredParcels.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val dimensionUnit by viewModel.dimensionUnit.collectAsStateWithLifecycle()

    var showBatchDropdown by remember { mutableStateOf(false) }
    var parcelToEdit by remember { mutableStateOf<ParcelEntity?>(null) }
    var parcelToDelete by remember { mutableStateOf<ParcelEntity?>(null) }
    var parcelDetailView by remember { mutableStateOf<ParcelEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Parcel History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Batch: ${selectedBatch?.batchCode ?: "All"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_button")
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
                    // Batch Selector Dropdown Menu
                    Box {
                        IconButton(onClick = { showBatchDropdown = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Select Batch",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showBatchDropdown,
                            onDismissRequest = { showBatchDropdown = false }
                        ) {
                            allBatches.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text("${b.batchCode} (${b.batchDate})") },
                                    onClick = {
                                        viewModel.selectBatch(b.id)
                                        showBatchDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    selectedBatch?.let { b ->
                        IconButton(onClick = { onNavigateToExport(b.id) }) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export Excel",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search & Stats Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search by Barcode, Pincode, or Addressee...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Records: $totalCount",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        selectedBatch?.let { b ->
                            Button(
                                onClick = { onNavigateToEntry(b.id) },
                                modifier = Modifier.testTag("history_add_entry_button")
                            ) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add Entry", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Parcels List
            if (parcels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No parcels match your search query." else "No parcel records found in this batch.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(parcels, key = { it.id }) { parcel ->
                        ParcelItemCard(
                            parcel = parcel,
                            weightUnit = weightUnit,
                            dimensionUnit = dimensionUnit,
                            onEdit = { parcelToEdit = parcel },
                            onDelete = { parcelToDelete = parcel },
                            onViewDetail = { parcelDetailView = parcel }
                        )
                    }
                }
            }
        }
    }

    // Edit Parcel Dialog
    parcelToEdit?.let { parcel ->
        EditParcelDialog(
            parcel = parcel,
            weightUnit = weightUnit,
            dimensionUnit = dimensionUnit,
            onDismiss = { parcelToEdit = null },
            onConfirmSave = { updatedParcel ->
                viewModel.updateParcel(updatedParcel)
                parcelToEdit = null
            }
        )
    }

    // Delete Parcel Dialog
    parcelToDelete?.let { parcel ->
        AlertDialog(
            onDismissRequest = { parcelToDelete = null },
            title = { Text("Delete Parcel Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete parcel with barcode ${parcel.barcode}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteParcel(parcel.id)
                        parcelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { parcelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Detail Dialog
    parcelDetailView?.let { parcel ->
        AlertDialog(
            onDismissRequest = { parcelDetailView = null },
            title = { Text("Parcel Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Barcode: ${parcel.barcode}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Destination Pincode: ${parcel.destinationPincode}")
                    Text("Addressee: ${parcel.addresseeName}")
                    Text("Physical Weight: ${parcel.physicalWeight} $weightUnit")
                    Text("Dimensions (L x B x H): ${parcel.length} x ${parcel.breadth} x ${parcel.height} $dimensionUnit")
                }
            },
            confirmButton = {
                Button(onClick = { parcelDetailView = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ParcelItemCard(
    parcel: ParcelEntity,
    weightUnit: String,
    dimensionUnit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parcel_item_${parcel.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parcel.barcode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "To: ${parcel.addresseeName} (PIN: ${parcel.destinationPincode})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row {
                    IconButton(onClick = onViewDetail) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "View Details", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Parcel", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Parcel", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Weight: ${parcel.physicalWeight} $weightUnit",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${parcel.length} × ${parcel.breadth} × ${parcel.height} $dimensionUnit",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditParcelDialog(
    parcel: ParcelEntity,
    weightUnit: String,
    dimensionUnit: String,
    onDismiss: () -> Unit,
    onConfirmSave: (ParcelEntity) -> Unit
) {
    var barcode by remember { mutableStateOf(parcel.barcode) }
    var pincode by remember { mutableStateOf(parcel.destinationPincode) }
    var addressee by remember { mutableStateOf(parcel.addresseeName) }
    var weight by remember { mutableStateOf(parcel.physicalWeight.toString()) }
    var length by remember { mutableStateOf(parcel.length.toString()) }
    var breadth by remember { mutableStateOf(parcel.breadth.toString()) }
    var height by remember { mutableStateOf(parcel.height.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Parcel Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
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
                    label = { Text("Pincode") },
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
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = breadth,
                        onValueChange = { breadth = it },
                        label = { Text("B ($dimensionUnit)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("H ($dimensionUnit)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = parcel.copy(
                        barcode = barcode.trim(),
                        destinationPincode = pincode.trim(),
                        addresseeName = addressee.trim(),
                        physicalWeight = weight.toDoubleOrNull() ?: parcel.physicalWeight,
                        length = length.toDoubleOrNull() ?: parcel.length,
                        breadth = breadth.toDoubleOrNull() ?: parcel.breadth,
                        height = height.toDoubleOrNull() ?: parcel.height,
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
