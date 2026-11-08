package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.backup.BackupRestoreScreen
import com.example.ui.screens.backup.BackupRestoreViewModel
import com.example.ui.screens.batch.BatchManagementScreen
import com.example.ui.screens.batch.BatchViewModel
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.entry.ParcelEntryScreen
import com.example.ui.screens.entry.ParcelEntryViewModel
import com.example.ui.screens.export.ExcelExportScreen
import com.example.ui.screens.export.ExcelExportViewModel
import com.example.ui.screens.history.ParcelHistoryScreen
import com.example.ui.screens.history.ParcelHistoryViewModel
import com.example.ui.screens.secondary.SecondaryAddresseeStageScreen
import com.example.ui.screens.secondary.SecondaryBarcodeStageScreen
import com.example.ui.screens.secondary.SecondaryBatchDashboardScreen
import com.example.ui.screens.secondary.SecondaryBatchReviewScreen
import com.example.ui.screens.secondary.SecondaryBatchViewModel
import com.example.ui.screens.secondary.SecondaryMeasurementsStageScreen
import com.example.ui.screens.secondary.SecondaryPincodeStageScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppPreferencesRepository
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.welcome.WelcomeScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepository = remember { AppPreferencesRepository(context) }
    val isFirstTime by prefsRepository.isFirstTimeOpen.collectAsStateWithLifecycle(initialValue = false)

    NavHost(
        navController = navController,
        startDestination = if (isFirstTime) Screen.Welcome.route else Screen.Dashboard.route
    ) {
        // Welcome Screen (Only for 1st time app open)
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    scope.launch {
                        prefsRepository.setFirstTimeOpenCompleted()
                    }
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard
        composable(Screen.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = viewModel()
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToNewBatch = {
                    navController.navigate(Screen.BatchManagement.route)
                },
                onNavigateToContinueBatch = { batchId ->
                    navController.navigate(Screen.ParcelEntry.createRoute(batchId))
                },
                onNavigateToSecondaryMode = { batchId ->
                    navController.navigate(Screen.SecondaryDashboard.createRoute(batchId ?: -1L))
                },
                onNavigateToHistory = { batchId ->
                    navController.navigate(Screen.ParcelHistory.createRoute(batchId ?: -1L))
                },
                onNavigateToExport = { batchId ->
                    navController.navigate(Screen.ExcelExport.createRoute(batchId ?: -1L))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToBackup = {
                    navController.navigate(Screen.BackupRestore.route)
                }
            )
        }

        // Batch Management
        composable(Screen.BatchManagement.route) {
            val batchViewModel: BatchViewModel = viewModel()
            BatchManagementScreen(
                viewModel = batchViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenParcelEntry = { batchId ->
                    navController.navigate(Screen.ParcelEntry.createRoute(batchId))
                },
                onViewHistory = { batchId ->
                    navController.navigate(Screen.ParcelHistory.createRoute(batchId))
                },
                onExportBatch = { batchId ->
                    navController.navigate(Screen.ExcelExport.createRoute(batchId))
                }
            )
        }

        // Parcel Entry Screen (Primary Mode A)
        composable(
            route = Screen.ParcelEntry.route,
            arguments = listOf(
                navArgument("batchId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: -1L
            val entryViewModel: ParcelEntryViewModel = viewModel()
            ParcelEntryScreen(
                batchId = batchId,
                viewModel = entryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onViewBatchHistory = { bId ->
                    navController.navigate(Screen.ParcelHistory.createRoute(bId))
                }
            )
        }

        // Secondary Entry Mode Dashboard
        composable(
            route = Screen.SecondaryDashboard.route,
            arguments = listOf(
                navArgument("batchId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: -1L
            val secondaryViewModel: SecondaryBatchViewModel = viewModel()
            SecondaryBatchDashboardScreen(
                batchId = batchId,
                viewModel = secondaryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBarcodeStage = {
                    navController.navigate(Screen.SecondaryBarcodeStage.createRoute(batchId))
                },
                onNavigateToPincodeStage = {
                    navController.navigate(Screen.SecondaryPincodeStage.createRoute(batchId))
                },
                onNavigateToAddresseeStage = {
                    navController.navigate(Screen.SecondaryAddresseeStage.createRoute(batchId))
                },
                onNavigateToMeasurementsStage = {
                    navController.navigate(Screen.SecondaryMeasurementsStage.createRoute(batchId))
                },
                onNavigateToReviewStage = {
                    navController.navigate(Screen.SecondaryBatchReview.createRoute(batchId))
                }
            )
        }

        // Secondary Stage 1: Barcode Entry
        composable(
            route = Screen.SecondaryBarcodeStage.route,
            arguments = listOf(navArgument("batchId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: -1L
            val secondaryViewModel: SecondaryBatchViewModel = viewModel()
            SecondaryBarcodeStageScreen(
                batchId = batchId,
                viewModel = secondaryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onProceedToNextStage = {
                    navController.navigate(Screen.SecondaryPincodeStage.createRoute(batchId)) {
                        popUpTo(Screen.SecondaryDashboard.route)
                    }
                }
            )
        }

        // Secondary Stage 2: Pincode Entry
        composable(
            route = Screen.SecondaryPincodeStage.route,
            arguments = listOf(navArgument("batchId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: -1L
            val secondaryViewModel: SecondaryBatchViewModel = viewModel()
            SecondaryPincodeStageScreen(
                batchId = batchId,
                viewModel = secondaryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onProceedToNextStage = {
                    navController.navigate(Screen.SecondaryAddresseeStage.createRoute(batchId))
                }
            )
        }

        // Secondary Stage 3: Addressee Entry
        composable(
            route = Screen.SecondaryAddresseeStage.route,
            arguments = listOf(navArgument("batchId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: -1L
            val secondaryViewModel: SecondaryBatchViewModel = viewModel()
            SecondaryAddresseeStageScreen(
                batchId = batchId,
                viewModel = secondaryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onProceedToNextStage = {
                    navController.navigate(Screen.SecondaryMeasurementsStage.createRoute(batchId))
                }
            )
        }

        // Secondary Stage 4: Measurements Entry
        composable(
            route = Screen.SecondaryMeasurementsStage.route,
            arguments = listOf(navArgument("batchId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: -1L
            val secondaryViewModel: SecondaryBatchViewModel = viewModel()
            SecondaryMeasurementsStageScreen(
                batchId = batchId,
                viewModel = secondaryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onProceedToReviewStage = {
                    navController.navigate(Screen.SecondaryBatchReview.createRoute(batchId))
                }
            )
        }

        // Secondary Stage 5: Final Batch Review
        composable(
            route = Screen.SecondaryBatchReview.route,
            arguments = listOf(navArgument("batchId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: -1L
            val secondaryViewModel: SecondaryBatchViewModel = viewModel()
            SecondaryBatchReviewScreen(
                batchId = batchId,
                viewModel = secondaryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExport = { bId ->
                    navController.navigate(Screen.ExcelExport.createRoute(bId))
                }
            )
        }

        // Parcel History Screen
        composable(
            route = Screen.ParcelHistory.route,
            arguments = listOf(
                navArgument("batchId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: -1L
            val historyViewModel: ParcelHistoryViewModel = viewModel()
            ParcelHistoryScreen(
                batchId = batchId,
                viewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { bId ->
                    navController.navigate(Screen.ParcelEntry.createRoute(bId))
                },
                onNavigateToExport = { bId ->
                    navController.navigate(Screen.ExcelExport.createRoute(bId))
                }
            )
        }

        // Excel Export Screen
        composable(
            route = Screen.ExcelExport.route,
            arguments = listOf(
                navArgument("batchId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: -1L
            val exportViewModel: ExcelExportViewModel = viewModel()
            ExcelExportScreen(
                batchId = batchId,
                viewModel = exportViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTemplateSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Backup and Restore Screen
        composable(Screen.BackupRestore.route) {
            val backupViewModel: BackupRestoreViewModel = viewModel()
            BackupRestoreScreen(
                viewModel = backupViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
