package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Dashboard : Screen("dashboard")
    object BatchManagement : Screen("batch_management")
    object ParcelEntry : Screen("parcel_entry?batchId={batchId}") {
        fun createRoute(batchId: Long = -1L) = "parcel_entry?batchId=$batchId"
    }
    object ParcelHistory : Screen("parcel_history?batchId={batchId}") {
        fun createRoute(batchId: Long = -1L) = "parcel_history?batchId=$batchId"
    }
    object ExcelExport : Screen("excel_export?batchId={batchId}") {
        fun createRoute(batchId: Long = -1L) = "excel_export?batchId=$batchId"
    }
    object Settings : Screen("settings")
    object BackupRestore : Screen("backup_restore")

    // Secondary Entry Mode Screens
    object SecondaryDashboard : Screen("secondary_dashboard?batchId={batchId}") {
        fun createRoute(batchId: Long = -1L) = "secondary_dashboard?batchId=$batchId"
    }
    object SecondaryBarcodeStage : Screen("secondary_barcode?batchId={batchId}") {
        fun createRoute(batchId: Long = -1L) = "secondary_barcode?batchId=$batchId"
    }
    object SecondaryPincodeStage : Screen("secondary_pincode?batchId={batchId}") {
        fun createRoute(batchId: Long = -1L) = "secondary_pincode?batchId=$batchId"
    }
    object SecondaryAddresseeStage : Screen("secondary_addressee?batchId={batchId}") {
        fun createRoute(batchId: Long = -1L) = "secondary_addressee?batchId=$batchId"
    }
    object SecondaryMeasurementsStage : Screen("secondary_measurements?batchId={batchId}") {
        fun createRoute(batchId: Long = -1L) = "secondary_measurements?batchId=$batchId"
    }
    object SecondaryBatchReview : Screen("secondary_review?batchId={batchId}") {
        fun createRoute(batchId: Long = -1L) = "secondary_review?batchId=$batchId"
    }
}
