package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import com.example.data.models.ExcelColumnConfig
import com.example.data.models.DimensionPreset
import com.example.util.AppUpdateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val pincodeLength by viewModel.pincodeLength.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val dimensionUnit by viewModel.dimensionUnit.collectAsStateWithLifecycle()
    val maxLength by viewModel.maxLength.collectAsStateWithLifecycle()
    val maxBreadth by viewModel.maxBreadth.collectAsStateWithLifecycle()
    val maxHeight by viewModel.maxHeight.collectAsStateWithLifecycle()
    val hapticFeedback by viewModel.hapticFeedback.collectAsStateWithLifecycle()
    val autoOpenCamera by viewModel.autoOpenCamera.collectAsStateWithLifecycle()
    val excelConfig by viewModel.excelConfig.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("settings_back_button")
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

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("General & Theme", fontSize = 12.sp, fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium) },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "General & Theme", modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.testTag("settings_tab_general")
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Parcel & Presets", fontSize = 12.sp, fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium) },
                        icon = { Icon(Icons.Default.Category, contentDescription = "Parcel & Presets", modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.testTag("settings_tab_presets")
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Excel Export", fontSize = 12.sp, fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Medium) },
                        icon = { Icon(Icons.Default.TableChart, contentDescription = "Excel Export", modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.testTag("settings_tab_excel")
                    )
                }
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
            when (selectedTabIndex) {
                0 -> {
                    // App Appearance / Theme Mode Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_theme_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "App Appearance & Theme",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Choose Light, Dark, or System default theme",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeOptionButton(
                                    title = "System",
                                    icon = Icons.Default.BrightnessAuto,
                                    isSelected = themeMode == "SYSTEM",
                                    onClick = { viewModel.updateThemeMode("SYSTEM") },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionButton(
                                    title = "Light",
                                    icon = Icons.Default.LightMode,
                                    isSelected = themeMode == "LIGHT",
                                    onClick = { viewModel.updateThemeMode("LIGHT") },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionButton(
                                    title = "Dark",
                                    icon = Icons.Default.DarkMode,
                                    isSelected = themeMode == "DARK",
                                    onClick = { viewModel.updateThemeMode("DARK") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Scanner & Feedback Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Scanner & Feedback",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Vibration & Haptic Feedback", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Vibrate on successful barcode scan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = hapticFeedback,
                                    onCheckedChange = { viewModel.updateHapticFeedback(it) },
                                    modifier = Modifier.testTag("haptic_feedback_switch")
                                )
                            }

                            val scanSound by viewModel.scanSound.collectAsStateWithLifecycle()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Audible Beep Feedback", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Play a brief confirmation sound when a barcode is detected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = scanSound,
                                    onCheckedChange = { viewModel.updateScanSound(it) },
                                    modifier = Modifier.testTag("scan_sound_switch")
                                )
                            }

                            val defaultScanMode by viewModel.defaultScanMode.collectAsStateWithLifecycle()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Default Scanner Mode", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Single scan or continuous scanning", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    listOf("SINGLE", "CONTINUOUS").forEach { mode ->
                                        OutlinedButton(
                                            onClick = { viewModel.updateDefaultScanMode(mode) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(horizontal = 2.dp),
                                            colors = if (defaultScanMode == mode) ButtonDefaults.outlinedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            ) else ButtonDefaults.outlinedButtonColors()
                                        ) {
                                            Text(if (mode == "SINGLE") "Single" else "Continuous", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Open Camera After Save", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Automatically launch scanner after saving parcel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = autoOpenCamera,
                                    onCheckedChange = { viewModel.updateAutoOpenCamera(it) },
                                    modifier = Modifier.testTag("auto_camera_switch")
                                )
                            }
                        }
                    }

                    // App Updates Card (General Tab Only)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_app_updates_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFF0EA5E9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color(0xFF0EA5E9),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "App Updates",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0EA5E9)
                                    )
                                    Text(
                                        text = "Installed Version: v${viewModel.installedVersionName} (Build ${viewModel.installedVersionCode})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("installed_version_text")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Check for Updates",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Verify if a newer signed version is available on GitHub",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val isBusy = updateState is AppUpdateUiState.Checking ||
                                    updateState is AppUpdateUiState.Downloading ||
                                    updateState is AppUpdateUiState.Verifying

                                OutlinedButton(
                                    onClick = { viewModel.checkForUpdates() },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("check_for_updates_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Check for Updates",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check for Updates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            when (val state = updateState) {
                                is AppUpdateUiState.Idle -> {
                                    // Clean default state
                                }

                                is AppUpdateUiState.Checking -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp)
                                            .testTag("update_checking_state"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp,
                                            color = Color(0xFF0EA5E9)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Checking GitHub Releases for updates...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                is AppUpdateUiState.UpToDate -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFF10B981).copy(alpha = 0.12f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp)
                                            .testTag("update_up_to_date_state"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Parcel Pro is up to date",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF10B981)
                                            )
                                            Text(
                                                text = "You have the latest version (v${state.currentVersionName}).",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                is AppUpdateUiState.UpdateAvailable -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("update_available_state"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF0EA5E9).copy(alpha = 0.1f)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF0EA5E9))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = "New Update Available: v${state.metadata.versionName} (Build ${state.metadata.versionCode})",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF0EA5E9)
                                                )
                                                Text(
                                                    text = "Your parcels, batches, and settings will be preserved during the update.",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Button(
                                                onClick = { viewModel.startUpdateDownload(state.metadata) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("update_now_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SystemUpdate,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Update Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                is AppUpdateUiState.Downloading -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp)
                                            .testTag("update_downloading_state"),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Downloading v${state.metadata.versionName}...",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${state.progressPercent}%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF0EA5E9)
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { state.progressPercent / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp),
                                            color = Color(0xFF0EA5E9)
                                        )
                                    }
                                }

                                is AppUpdateUiState.Verifying -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp)
                                            .testTag("update_verifying_state"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp,
                                            color = Color(0xFF0EA5E9)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Verifying SHA-256 checksum & package integrity...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                is AppUpdateUiState.ReadyToInstall -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("update_success_state"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF10B981).copy(alpha = 0.12f)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF10B981))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Update Verified & Ready (v${state.metadata.versionName})",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF10B981)
                                                    )
                                                    Text(
                                                        text = "SHA-256 checksum verified. Tap below if the installer did not open automatically.",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { viewModel.installDownloadedUpdate(state.apkFile) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("install_update_button")
                                            ) {
                                                Text("Install Update Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                is AppUpdateUiState.Error -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("update_error_state"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = state.message,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            if (state.metadata != null) {
                                                OutlinedButton(
                                                    onClick = { viewModel.startUpdateDownload(state.metadata) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Retry Update Download", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Data Entry Defaults Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Data Entry Defaults",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Pincode length setting
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Destination Pincode Digits", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Indian PIN codes require 6 digits", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    listOf(6, 5, 4).forEach { len ->
                                        OutlinedButton(
                                            onClick = { viewModel.updatePincodeLength(len) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(horizontal = 2.dp),
                                            colors = if (pincodeLength == len) ButtonDefaults.outlinedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            ) else ButtonDefaults.outlinedButtonColors()
                                        ) {
                                            Text("$len", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Weight Unit setting
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Weight Unit", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Default unit for physical weight", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    listOf("g", "kg", "lbs").forEach { unit ->
                                        OutlinedButton(
                                            onClick = { viewModel.updateWeightUnit(unit) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(horizontal = 2.dp),
                                            colors = if (weightUnit == unit) ButtonDefaults.outlinedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            ) else ButtonDefaults.outlinedButtonColors()
                                        ) {
                                            Text(unit, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Dimension Unit setting
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dimension Unit", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Default unit for Length, Breadth & Height", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    listOf("cm", "m", "in").forEach { unit ->
                                        OutlinedButton(
                                            onClick = { viewModel.updateDimensionUnit(unit) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(horizontal = 2.dp),
                                            colors = if (dimensionUnit == unit) ButtonDefaults.outlinedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            ) else ButtonDefaults.outlinedButtonColors()
                                        ) {
                                            Text(unit, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Volumetric Weight Divisor setting
                            val volumetricDivisor by viewModel.volumetricDivisor.collectAsStateWithLifecycle()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Volumetric Weight Divisor", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Formula: (L × B × H) / Divisor (Default: 5000)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedTextField(
                                    value = if (volumetricDivisor > 0.0) volumetricDivisor.toInt().toString() else "5000",
                                    onValueChange = { input ->
                                        val v = input.toDoubleOrNull() ?: 5000.0
                                        viewModel.updateVolumetricDivisor(v)
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .testTag("settings_volumetric_divisor_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }

                    // Custom Dimension Presets Manager Card (Unlimited Presets)
                    val dimensionPresets by viewModel.dimensionPresets.collectAsStateWithLifecycle()
                    var showAddPresetDialog by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_dimension_presets_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Custom Dimension Presets",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8B5CF6)
                                    )
                                    Text(
                                        text = "Create unlimited dimension presets for 1-tap entry & quick search",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { showAddPresetDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("add_dimension_preset_button")
                                ) {
                                    Text("+ Add Preset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                dimensionPresets.forEach { preset ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${preset.length.toInt()} × ${preset.breadth.toInt()} × ${preset.height.toInt()} $dimensionUnit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteDimensionPreset(preset.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Delete Preset", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dialog for Adding New Dimension Preset
                    if (showAddPresetDialog) {
                        var newName by remember { mutableStateOf("") }
                        var newLength by remember { mutableStateOf("") }
                        var newBreadth by remember { mutableStateOf("") }
                        var newHeight by remember { mutableStateOf("") }
                        var inputError by remember { mutableStateOf<String?>(null) }

                        AlertDialog(
                            onDismissRequest = { showAddPresetDialog = false },
                            title = { Text("Add Custom Dimension Preset", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = newName,
                                        onValueChange = { newName = it },
                                        label = { Text("Preset Name (e.g. Shoe Box)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = newLength,
                                            onValueChange = { newLength = it },
                                            label = { Text("Length") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                        )
                                        OutlinedTextField(
                                            value = newBreadth,
                                            onValueChange = { newBreadth = it },
                                            label = { Text("Breadth") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                        )
                                        OutlinedTextField(
                                            value = newHeight,
                                            onValueChange = { newHeight = it },
                                            label = { Text("Height") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                        )
                                    }

                                    inputError?.let { err ->
                                        Text(err, color = Color.Red, fontSize = 12.sp)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val l = newLength.toDoubleOrNull() ?: 0.0
                                        val b = newBreadth.toDoubleOrNull() ?: 0.0
                                        val h = newHeight.toDoubleOrNull() ?: 0.0
                                        if (newName.isBlank()) {
                                            inputError = "Please enter a preset name."
                                        } else if (l <= 0 || b <= 0 || h <= 0) {
                                            inputError = "Dimensions must be greater than 0."
                                        } else {
                                            viewModel.addDimensionPreset(newName, l, b, h)
                                            showAddPresetDialog = false
                                        }
                                    }
                                ) {
                                    Text("Save Preset")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddPresetDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Maximum Dimensions Validation Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Maximum Dimensions Validation",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "Set the maximum allowed dimensions ($dimensionUnit) for parcel entry. Enter 0 for unlimited.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (maxLength > 0.0) maxLength.toString() else "",
                                    onValueChange = { input ->
                                        val v = input.toDoubleOrNull() ?: 0.0
                                        viewModel.updateMaxLength(v)
                                    },
                                    label = { Text("Max Length") },
                                    placeholder = { Text("No limit") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_max_length_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )

                                OutlinedTextField(
                                    value = if (maxBreadth > 0.0) maxBreadth.toString() else "",
                                    onValueChange = { input ->
                                        val v = input.toDoubleOrNull() ?: 0.0
                                        viewModel.updateMaxBreadth(v)
                                    },
                                    label = { Text("Max Breadth") },
                                    placeholder = { Text("No limit") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_max_breadth_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )

                                OutlinedTextField(
                                    value = if (maxHeight > 0.0) maxHeight.toString() else "",
                                    onValueChange = { input ->
                                        val v = input.toDoubleOrNull() ?: 0.0
                                        viewModel.updateMaxHeight(v)
                                    },
                                    label = { Text("Max Height") },
                                    placeholder = { Text("No limit") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_max_height_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Excel Template Customization Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFF06B6D4)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Excel Template Configuration",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { viewModel.resetExcelTemplateToDefault() },
                                    modifier = Modifier.testTag("reset_excel_template_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Defaults", fontSize = 12.sp)
                                }
                            }

                            Text(
                                text = "Customize header names, toggle visibility, and reorder columns to match your office's Excel template.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            excelConfig.columns.forEachIndexed { index, col ->
                                ExcelColumnSettingItem(
                                    index = index,
                                    config = col,
                                    isFirst = index == 0,
                                    isLast = index == excelConfig.columns.size - 1,
                                    onUpdateHeader = { newHeader ->
                                        viewModel.updateExcelColumn(col.key, newHeader, col.enabled)
                                    },
                                    onToggleEnabled = { enabled ->
                                        viewModel.updateExcelColumn(col.key, col.customHeader, enabled)
                                    },
                                    onMoveUp = { viewModel.moveExcelColumn(index, true) },
                                    onMoveDown = { viewModel.moveExcelColumn(index, false) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExcelColumnSettingItem(
    index: Int,
    config: ExcelColumnConfig,
    isFirst: Boolean,
    isLast: Boolean,
    onUpdateHeader: (String) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = config.enabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.testTag("column_switch_${config.key}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = config.customHeader,
                onValueChange = onUpdateHeader,
                label = { Text("Col ${(('A'.code + index).toChar())} (${config.defaultHeader})") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("column_header_input_${config.key}")
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst
                ) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast
                ) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
