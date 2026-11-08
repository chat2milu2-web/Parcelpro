package com.example.scanner

import androidx.compose.runtime.Composable

@Composable
fun CameraBarcodeScannerDialog(
    batchCode: String = "ACTIVE",
    savedParcelCount: Int = 0,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AdvancedBarcodeScannerScreen(
        batchCode = batchCode,
        savedParcelCount = savedParcelCount,
        initialScanMode = ScanMode.SINGLE,
        onBarcodeScanned = { code, _ ->
            onBarcodeScanned(code)
            onDismiss()
        },
        onCloseScanner = onDismiss
    )
}
