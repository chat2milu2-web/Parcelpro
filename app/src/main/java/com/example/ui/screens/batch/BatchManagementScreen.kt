package com.example.ui.screens.batch

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entities.BatchEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchManagementScreen(
    viewModel: BatchViewModel,
    onNavigateBack: () -> Unit,
    onOpenParcelEntry: (Long) -> Unit,
    onViewHistory: (Long) -> Unit,
    onExportBatch: (Long) -> Unit
) {
    val activeBatch by viewModel.activeBatch.collectAsStateWithLifecycle()
    val allBatches by viewModel.allBatches.collectAsStateWithLifecycle()
    val suggestedCode by viewModel.suggestedBatchCode.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var customBatchName by remember(suggestedCode) { mutableStateOf(suggestedCode) }
    var batchDescription by remember { mutableStateOf("") }
    var batchToDelete by remember { mutableStateOf<BatchEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("batch_back_button")
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.generateNewBatchCode()
                    showCreateDialog = true
                },
                modifier = Modifier.testTag("create_batch_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Batch", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (allBatches.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Batches Created Yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create a batch to start entering parcel data.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.generateNewBatchCode()
                            showCreateDialog = true
                        },
                        modifier = Modifier.testTag("empty_state_create_batch_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create First Batch")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "All Batches (${allBatches.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(allBatches, key = { it.id }) { batch ->
                        val isActive = activeBatch?.id == batch.id

                        BatchCardItem(
                            batch = batch,
                            isActive = isActive,
                            onSetActive = { viewModel.setActiveBatch(batch.id) },
                            onContinueEntry = { onOpenParcelEntry(batch.id) },
                            onViewHistory = { onViewHistory(batch.id) },
                            onExport = { onExportBatch(batch.id) },
                            onMarkCompleted = { viewModel.completeBatch(batch.id) },
                            onDelete = { batchToDelete = batch }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Create New Batch Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Batch", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customBatchName,
                        onValueChange = { customBatchName = it },
                        label = { Text("Batch Name / Code *") },
                        placeholder = { Text("e.g. $suggestedCode or Delhi_Desk_01") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_name_input")
                    )

                    OutlinedTextField(
                        value = batchDescription,
                        onValueChange = { batchDescription = it },
                        label = { Text("Batch Description / Note") },
                        placeholder = { Text("e.g. Morning Shift Delhi Desk") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_description_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalBatchName = if (customBatchName.isNotBlank()) customBatchName else suggestedCode
                        viewModel.createBatch(description = batchDescription, customBatchCode = finalBatchName) { newId ->
                            showCreateDialog = false
                            batchDescription = ""
                            onOpenParcelEntry(newId)
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_batch_button")
                ) {
                    Text("Create & Start Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Batch Dialog
    if (batchToDelete != null) {
        AlertDialog(
            onDismissRequest = { batchToDelete = null },
            title = { Text("Delete Batch?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete batch ${batchToDelete!!.batchCode}? All recorded parcels in this batch will be permanently removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBatch(batchToDelete!!.id)
                        batchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { batchToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BatchCardItem(
    batch: BatchEntity,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onContinueEntry: () -> Unit,
    onViewHistory: () -> Unit,
    onExport: () -> Unit,
    onMarkCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_item_${batch.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = batch.batchCode,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(status = batch.status, isActive = isActive)
                    }

                    if (batch.description.isNotEmpty()) {
                        Text(
                            text = batch.description,
                            fontSize = 13.sp,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Date: ${batch.batchDate}",
                        fontSize = 12.sp,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Batch",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isActive) {
                    OutlinedButton(
                        onClick = onSetActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Active", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onContinueEntry,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Entry", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Records", fontSize = 12.sp)
                }

                IconButton(onClick = onExport) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export Excel", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String, isActive: Boolean) {
    val (bgColor, textColor, label) = when {
        isActive -> Triple(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, "ACTIVE")
        status == BatchEntity.STATUS_COMPLETED -> Triple(Color(0xFF16A34A), Color.White, "COMPLETED")
        status == BatchEntity.STATUS_EXPORTED -> Triple(Color(0xFF0284C7), Color.White, "EXPORTED")
        else -> Triple(Color.Gray, Color.White, status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
