package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AnimatedGradientBorderCard
import com.example.ui.theme.LocalThemeIsDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToNewBatch: () -> Unit,
    onNavigateToContinueBatch: (Long) -> Unit,
    onNavigateToSecondaryMode: (Long?) -> Unit,
    onNavigateToHistory: (Long?) -> Unit,
    onNavigateToExport: (Long?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val todayCount by viewModel.todayEntriesCount.collectAsStateWithLifecycle()
    val todayTotalWeight by viewModel.todayTotalWeight.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val activeBatch by viewModel.activeBatch.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeBatchParcelCount.collectAsStateWithLifecycle()
    val completedCount by viewModel.completedBatchesCount.collectAsStateWithLifecycle()
    val exportedCount by viewModel.exportedBatchesCount.collectAsStateWithLifecycle()
    val currentIsDark = LocalThemeIsDark.current

    val formattedWeight = androidx.compose.runtime.remember(todayTotalWeight, weightUnit) {
        if (weightUnit.equals("g", ignoreCase = true) && todayTotalWeight >= 1000.0) {
            String.format(java.util.Locale.US, "%.2f kg", todayTotalWeight / 1000.0)
        } else {
            String.format(java.util.Locale.US, "%.1f %s", todayTotalWeight, weightUnit)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_parcel_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Parcel Entry Pro",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Smart Barcode & Address OCR",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Dark / Light Mode Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleTheme(currentIsDark) },
                        modifier = Modifier.testTag("dashboard_theme_toggle_button")
                    ) {
                        AnimatedContent(
                            targetState = currentIsDark,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "theme_icon_anim"
                        ) { isDark ->
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Settings Button
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // ACTIVE BATCH ENTRY CARD (ANIMATED GRADIENT BORDER)
            // ==========================================
            AnimatedGradientBorderCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_batch_card"),
                borderWidth = 2.dp,
                outerCornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Soft pill badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ACTIVE BATCH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Theme indicator chip with border
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { viewModel.toggleTheme(currentIsDark) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (currentIsDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = if (currentIsDark) Color(0xFFFBBF24) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentIsDark) "Dark" else "Light",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeBatch?.batchCode ?: "No Active Batch",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (activeBatch != null && activeBatch!!.description.isNotEmpty()) {
                                    activeBatch!!.description
                                } else {
                                    "Ready to scan barcodes & photos"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Parcels in Batch",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$activeCount",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "items",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }

                        if (activeBatch != null) {
                            Button(
                                onClick = { onNavigateToContinueBatch(activeBatch!!.id) },
                                modifier = Modifier.testTag("continue_current_batch_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Continue Entry", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.createQuickBatch { batchId ->
                                        onNavigateToContinueBatch(batchId)
                                    }
                                },
                                modifier = Modifier.testTag("quick_create_batch_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start New Batch", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // OPERATIONAL STATS (COLOURFUL BORDERS)
            // ==========================================
            Text(
                text = "Operational Overview",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BorderStatCard(
                    title = "Entries Today",
                    value = "$todayCount",
                    icon = Icons.Default.Today,
                    accentColor = Color(0xFF2563EB), // Blue border
                    modifier = Modifier.weight(1f)
                )
                BorderStatCard(
                    title = "Completed",
                    value = "$completedCount",
                    icon = Icons.Default.CheckCircle,
                    accentColor = Color(0xFF10B981), // Green border
                    modifier = Modifier.weight(1f)
                )
                BorderStatCard(
                    title = "Exported",
                    value = "$exportedCount",
                    icon = Icons.Default.FileDownload,
                    accentColor = Color(0xFFF59E0B), // Amber border
                    modifier = Modifier.weight(1f)
                )
            }

            // Daily Dispatch Manifest Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_daily_manifest_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Today's Dispatch Manifest",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$formattedWeight total • $todayCount parcels",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onNavigateToExport(activeBatch?.id) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF10B981)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text("Export Manifest", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ==========================================
            // DATA ENTRY WORKFLOWS (COLOURFUL BORDERS)
            // ==========================================
            Text(
                text = "Data Entry Workflows",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BorderWorkflowCard(
                    title = "Mode A: Sequential",
                    subtitle = "One parcel (Barcode + OCR)",
                    badgeText = "⚡ 5/5 ACCURACY",
                    icon = Icons.Default.QrCodeScanner,
                    accentColor = Color(0xFF2563EB), // Electric Blue border
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_mode_a_card"),
                    onClick = {
                        if (activeBatch != null) {
                            onNavigateToContinueBatch(activeBatch!!.id)
                        } else {
                            onNavigateToNewBatch()
                        }
                    }
                )

                BorderWorkflowCard(
                    title = "Mode B: Field-Wise",
                    subtitle = "Barcodes → Pincodes → Names",
                    badgeText = "📑 MULTI-STAGE",
                    icon = Icons.Default.TableChart,
                    accentColor = Color(0xFF8B5CF6), // Purple border
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_mode_b_card"),
                    onClick = {
                        onNavigateToSecondaryMode(activeBatch?.id)
                    }
                )
            }

            // ==========================================
            // QUICK ACTIONS (COLOURFUL BORDERS)
            // ==========================================
            Text(
                text = "Quick Actions & Reports",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BorderActionCard(
                        title = "Create New Batch",
                        subtitle = "Start fresh parcel logging",
                        icon = Icons.Default.AddCircle,
                        accentColor = Color(0xFFF59E0B), // Amber border
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_create_batch_card"),
                        onClick = onNavigateToNewBatch
                    )

                    BorderActionCard(
                        title = "Continue Batch",
                        subtitle = activeBatch?.batchCode ?: "Select active batch",
                        icon = Icons.Default.PlayArrow,
                        accentColor = Color(0xFF2563EB), // Blue border
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_continue_batch_card"),
                        onClick = {
                            if (activeBatch != null) {
                                onNavigateToContinueBatch(activeBatch!!.id)
                            } else {
                                onNavigateToNewBatch()
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BorderActionCard(
                        title = "Parcel History",
                        subtitle = "View, edit & search records",
                        icon = Icons.Default.History,
                        accentColor = Color(0xFF8B5CF6), // Purple border
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_history_card"),
                        onClick = { onNavigateToHistory(activeBatch?.id) }
                    )

                    BorderActionCard(
                        title = "Export Excel / CSV",
                        subtitle = "Generate .xlsx, .csv reports",
                        icon = Icons.Default.FileDownload,
                        accentColor = Color(0xFF10B981), // Emerald Green border
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_export_card"),
                        onClick = { onNavigateToExport(activeBatch?.id) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BorderActionCard(
                        title = "Backup & Restore",
                        subtitle = "Local offline database backup",
                        icon = Icons.Default.Backup,
                        accentColor = Color(0xFFF43F5E), // Rose/Pink border
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_backup_card"),
                        onClick = onNavigateToBackup
                    )

                    BorderActionCard(
                        title = "Settings & Theme",
                        subtitle = "Appearance, Units & Template",
                        icon = Icons.Default.Settings,
                        accentColor = Color(0xFF06B6D4), // Cyan/Teal border
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_settings_card"),
                        onClick = onNavigateToSettings
                    )
                }
            }
        }
    }
}

// ==========================================
// CARD WITH COLOURFUL BORDER IMPLEMENTATIONS
// ==========================================

@Composable
private fun BorderStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accentColor), // Colourful Border
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BorderWorkflowCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(125.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accentColor), // Colourful Border
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BorderActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(95.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accentColor), // Colourful Border
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
