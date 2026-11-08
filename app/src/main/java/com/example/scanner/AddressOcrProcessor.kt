package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.regex.Pattern

data class AddressScanResult(
    val addresseeName: String? = null,
    val pincodes: List<String> = emptyList(),
    val phoneNumbers: List<String> = emptyList(),
    val trackingNumbers: List<String> = emptyList(),
    val fullText: String = "",
    val errorMessage: String? = null
)

object AddressOcrProcessor {

    // Regex to match standalone 6-digit Indian PIN codes (starting with 1-9, exactly 6 digits)
    private val PIN_PATTERN = Pattern.compile("(?<!\\d)([1-9][0-9]{5})(?!\\d)")

    // Regex to match 10-digit Indian Mobile Numbers (starting with 6, 7, 8, 9)
    private val PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:(?:\\+91|91|0)[\\-\\s]?)?([6-9]\\d{9})(?!\\d)")

    // Regex to match Courier Tracking / AWB IDs (10 to 18 uppercase alphanumeric)
    private val TRACKING_PATTERN = Pattern.compile("(?i)\\b(?:AWB|TRACKING|TRACK|WAYBILL|REF|SF|POD|EXPRESS|LABEL)?\\s*[:–-]?\\s*([A-Z0-9]{10,18})\\b")

    // Keywords indicating phone/mobile numbers
    private val PHONE_KEYWORDS = listOf("mob", "mobile", "phone", "ph", "tel", "contact", "cell", "call", "whatsapp")

    // Common label prefixes to clean from addressee name
    private val NAME_PREFIX_CLEANERS = listOf(
        Regex("^(?i)(?:to|ship\\s*to|deliver\\s*to|addressee|name|mr\\.?|mrs\\.?|ms\\.?|shri\\.?|recipient)\\s*[:–-]?\\s*"),
        Regex("^(?i)(?:c/o|care\\s*of)\\s*[:–-]?\\s*")
    )

    // Lines that should not be considered an Addressee Name
    private val IGNORED_NAME_STARTS = listOf(
        "FROM", "SENDER", "RETURN", "IF UNDELIVERED", "DISPATCH", "PARCEL", "TRACKING", "AWB", "INVOICE", "ORDER", "BILL TO"
    )

    /**
     * Fully offline, on-device OCR using Google ML Kit Text Recognition (Latin).
     */
    fun processImageUri(
        context: Context,
        imageUri: Uri,
        onSuccess: (AddressScanResult) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = extractAddressInfo(visionText)
                    onSuccess(result)
                }
                .addOnFailureListener { ex ->
                    onError(ex.localizedMessage ?: "Failed to process image OCR.")
                }
        } catch (e: Exception) {
            onError("Unable to load image for OCR: ${e.localizedMessage}")
        }
    }

    fun processBitmap(
        bitmap: Bitmap,
        onSuccess: (AddressScanResult) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = extractAddressInfo(visionText)
                    onSuccess(result)
                }
                .addOnFailureListener { ex ->
                    onError(ex.localizedMessage ?: "Failed to process image OCR.")
                }
        } catch (e: Exception) {
            onError("Unable to process bitmap for OCR: ${e.localizedMessage}")
        }
    }

    /**
     * Extracts Addressee Name, 6-digit Pincode(s), 10-digit Phone Number(s), and Tracking ID(s).
     */
    fun extractAddressInfo(visionText: Text): AddressScanResult {
        val lines = visionText.textBlocks.flatMap { it.lines.map { line -> line.text.trim() } }
            .ifEmpty { visionText.text.lines().map { it.trim() } }
            .filter { it.isNotBlank() }
        return extractAddressInfoFromLines(lines, visionText.text)
    }

    fun extractAddressInfoFromText(fullText: String): AddressScanResult {
        val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
        return extractAddressInfoFromLines(lines, fullText)
    }

    private fun extractAddressInfoFromLines(allLines: List<String>, fullText: String): AddressScanResult {
        if (fullText.isBlank() || allLines.isEmpty()) {
            return AddressScanResult(
                errorMessage = "No text could be detected from the image. Please retake photo with clear lighting or enter manually."
            )
        }

        // 1. Extract Addressee Name (first non-empty valid name line)
        var extractedName: String? = null
        for (line in allLines) {
            val upper = line.uppercase()
            val isIgnored = IGNORED_NAME_STARTS.any { upper.startsWith(it) }
            if (!isIgnored) {
                var cleaned = line
                for (cleaner in NAME_PREFIX_CLEANERS) {
                    cleaned = cleaner.replace(cleaned, "")
                }
                cleaned = cleaned.trim()
                if (cleaned.isNotBlank() && cleaned.length > 2 && !cleaned.all { it.isDigit() }) {
                    extractedName = cleaned.uppercase()
                    break
                }
            }
        }

        if (extractedName.isNullOrBlank() && allLines.isNotEmpty()) {
            extractedName = allLines.first().uppercase()
        }

        // 2. Extract 6-digit Indian PIN Code(s)
        val foundPins = mutableListOf<String>()
        allLines.forEach { line ->
            val matcher = PIN_PATTERN.matcher(line)
            while (matcher.find()) {
                val candidate = matcher.group(1) ?: continue
                val startIdx = matcher.start()
                val endIdx = matcher.end()

                val prefix = line.substring(0, startIdx).lowercase()
                val suffix = line.substring(endIdx).lowercase()

                val isPhoneRelated = PHONE_KEYWORDS.any { kw ->
                    prefix.takeLast(12).contains(kw) || suffix.take(12).contains(kw)
                }

                if (!isPhoneRelated && !foundPins.contains(candidate)) {
                    foundPins.add(candidate)
                }
            }
        }

        if (foundPins.isEmpty()) {
            val matcher = PIN_PATTERN.matcher(fullText)
            while (matcher.find()) {
                val candidate = matcher.group(1) ?: continue
                if (!foundPins.contains(candidate)) {
                    foundPins.add(candidate)
                }
            }
        }

        // 3. Extract 10-digit Indian Mobile Numbers
        val foundPhones = mutableListOf<String>()
        allLines.forEach { line ->
            val matcher = PHONE_PATTERN.matcher(line)
            while (matcher.find()) {
                val candidate = matcher.group(1) ?: continue
                if (!foundPhones.contains(candidate)) {
                    foundPhones.add(candidate)
                }
            }
        }

        // 4. Extract Courier Tracking IDs / AWB
        val foundTracking = mutableListOf<String>()
        allLines.forEach { line ->
            val matcher = TRACKING_PATTERN.matcher(line)
            while (matcher.find()) {
                val candidate = matcher.group(1) ?: continue
                if (candidate.length >= 10 && !candidate.all { it.isDigit() } && !foundTracking.contains(candidate)) {
                    foundTracking.add(candidate)
                }
            }
        }

        return AddressScanResult(
            addresseeName = extractedName,
            pincodes = foundPins,
            phoneNumbers = foundPhones,
            trackingNumbers = foundTracking,
            fullText = fullText
        )
    }
}
