package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

class InlineVoiceController(
    val isListening: Boolean,
    val activeTarget: String?,
    val partialText: String,
    val promptLabel: String,
    private val onStart: (target: String, promptMessage: String) -> Unit,
    private val onStop: () -> Unit
) {
    fun startListening(target: String, promptMessage: String) {
        if (isListening && activeTarget == target) {
            onStop()
        } else {
            onStart(target, promptMessage)
        }
    }

    fun stopListening() {
        onStop()
    }
}

@Composable
fun InlineVoiceListeningBanner(
    controller: InlineVoiceController,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = controller.isListening) {
        Surface(
            color = Color(0xFFDC2626).copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626)),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("inline_voice_listening_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (controller.partialText.isNotBlank()) {
                            "Listening: \"${controller.partialText}\""
                        } else {
                            controller.promptLabel.ifBlank { "Listening... Speak now (English, বাংলা, हिन्दी)" }
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { controller.stopListening() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Listening",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun rememberInlineVoiceController(
    onCandidatesReceived: (target: String, candidates: List<String>, isFinal: Boolean) -> Unit,
    onError: (String) -> Unit = {}
): InlineVoiceController {
    val context = LocalContext.current
    val currentOnCandidates by rememberUpdatedState(onCandidatesReceived)
    val currentOnError by rememberUpdatedState(onError)

    var isListening by remember { mutableStateOf(false) }
    var activeTarget by remember { mutableStateOf<String?>(null) }
    var partialText by remember { mutableStateOf("") }
    var promptLabel by remember { mutableStateOf("") }

    var pendingPermissionTarget by remember { mutableStateOf<String?>(null) }
    var pendingPermissionPrompt by remember { mutableStateOf("") }

    val speechRecognizerRef = remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Fallback popup launcher if inline SpeechRecognizer service is unavailable on the device
    val fallbackPopupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        val target = activeTarget
        activeTarget = null
        partialText = ""
        if (result.resultCode == Activity.RESULT_OK && target != null) {
            val spokenResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenResults.isNullOrEmpty()) {
                currentOnCandidates(target, spokenResults, true)
            }
        }
    }

    val launchFallbackPopup: (String, String) -> Unit = { target, promptMsg ->
        activeTarget = target
        isListening = false
        partialText = ""
        try {
            fallbackPopupLauncher.launch(VoiceInputHelper.createSpeechIntent(promptMsg))
        } catch (_: Exception) {
            currentOnError("Voice input service unavailable on device.")
        }
    }

    val startInlineRecognition: (String, String) -> Unit = { target, promptMsg ->
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            launchFallbackPopup(target, promptMsg)
        } else {
            try {
                speechRecognizerRef.value?.destroy()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizerRef.value = recognizer

                activeTarget = target
                promptLabel = promptMsg
                partialText = ""
                isListening = true

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        partialText = ""
                        when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                // User stayed silent; simply stop listening cleanly
                                activeTarget = null
                            }
                            SpeechRecognizer.ERROR_CLIENT -> {
                                activeTarget = null
                            }
                            else -> {
                                // Fallback to standard speech intent if inline service failed
                                val fallbackTarget = activeTarget
                                if (fallbackTarget != null) {
                                    launchFallbackPopup(fallbackTarget, promptMsg)
                                }
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        partialText = ""
                        val currentTarget = activeTarget
                        activeTarget = null
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (currentTarget != null && !matches.isNullOrEmpty()) {
                            currentOnCandidates(currentTarget, matches, true)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val currentTarget = activeTarget ?: return
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            partialText = matches.firstOrNull().orEmpty()
                            currentOnCandidates(currentTarget, matches, false)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = VoiceInputHelper.createSpeechIntent(promptMsg).apply {
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }
                recognizer.startListening(intent)
            } catch (_: Exception) {
                launchFallbackPopup(target, promptMsg)
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingPermissionTarget
        val prompt = pendingPermissionPrompt
        pendingPermissionTarget = null
        if (target != null) {
            if (granted) {
                startInlineRecognition(target, prompt)
            } else {
                launchFallbackPopup(target, prompt)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizerRef.value?.destroy()
                speechRecognizerRef.value = null
            } catch (_: Exception) {}
        }
    }

    return InlineVoiceController(
        isListening = isListening,
        activeTarget = activeTarget,
        partialText = partialText,
        promptLabel = promptLabel,
        onStart = { target, promptMsg ->
            val hasRecordPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasRecordPermission) {
                startInlineRecognition(target, promptMsg)
            } else {
                pendingPermissionTarget = target
                pendingPermissionPrompt = promptMsg
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onStop = {
            try {
                speechRecognizerRef.value?.stopListening()
            } catch (_: Exception) {}
            isListening = false
            activeTarget = null
            partialText = ""
        }
    )
}

object VoiceInputHelper {

    fun createSpeechIntent(promptMessage: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(
                "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                arrayOf("bn-IN", "hi-IN", "en-US", "en-GB")
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptMessage)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    /**
     * Selects the best candidate from Android SpeechRecognizer's EXTRA_RESULTS list
     * and formats it for either numeric or text/name fields.
     */
    fun parseVoiceCandidates(candidates: List<String>?, isNumericField: Boolean = false): String {
        if (candidates.isNullOrEmpty()) return ""
        if (isNumericField) {
            for (candidate in candidates) {
                val parsed = parseVoiceResult(candidate, isNumericField = true)
                if (parsed.isNotBlank()) return parsed
            }
            return ""
        } else {
            // Prefer a candidate that already contains Latin characters if available
            val latinCandidate = candidates.firstOrNull { candidate ->
                candidate.any { it in 'a'..'z' || it in 'A'..'Z' }
            }
            if (latinCandidate != null) {
                val parsedLatin = parseVoiceResult(latinCandidate, isNumericField = false)
                if (parsedLatin.isNotBlank()) return parsedLatin
            }
            // Otherwise transliterate the top Indic (Bengali/Hindi) candidate into English uppercase
            for (candidate in candidates) {
                val parsed = parseVoiceResult(candidate, isNumericField = false)
                if (parsed.isNotBlank()) return parsed
            }
            return ""
        }
    }

    /**
     * Extracts multiple dimension numbers from the best matching candidate in EXTRA_RESULTS.
     */
    fun extractMultipleNumbersFromCandidates(candidates: List<String>?): List<String> {
        if (candidates.isNullOrEmpty()) return emptyList()
        var bestResult = emptyList<String>()
        for (candidate in candidates) {
            val extracted = extractMultipleNumbers(candidate)
            if (extracted.size >= 3) return extracted
            if (extracted.size > bestResult.size) {
                bestResult = extracted
            }
        }
        return bestResult
    }

    /**
     * Parses spoken voice results into clean English text or digits.
     * Supports Hindi, Bengali, and English spoken inputs (including Indic-to-English transliteration).
     */
    fun parseVoiceResult(spokenText: String, isNumericField: Boolean = false): String {
        if (spokenText.isBlank()) return ""

        if (isNumericField) {
            return extractDigitsAndNumbers(spokenText)
        }

        // Convert any Bengali or Devanagari (Hindi) script to phonetic English before formatting
        val transliterated = transliterateIndicToEnglish(spokenText.trim())

        val cleaned = transliterated
            .replace(Regex("[^a-zA-Z0-9\\s\\.\\-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .uppercase(Locale.ENGLISH)

        return cleaned
    }

    private val commonIndicWordsToEnglish = mapOf(
        // Common Bengali Names & Surnames
        "মিলন" to "MILAN", "দাস" to "DAS", "রহিম" to "RAHIM", "শেখ" to "SHEKH",
        "রায়" to "ROY", "রায়" to "ROY", "ঘোষ" to "GHOSH", "বোস" to "BOSE",
        "বসু" to "BASU", "সেন" to "SEN", "দে" to "DEY", "দত্ত" to "DUTTA",
        "মণ্ডল" to "MANDAL", "মন্ডল" to "MANDAL", "সরকার" to "SARKAR",
        "পাল" to "PAUL", "মল্লিক" to "MALLICK", "চ্যাটার্জী" to "CHATTERJEE",
        "চট্টোপাধ্যায়" to "CHATTOPADHYAY", "ব্যানার্জী" to "BANERJEE",
        "বন্দ্যোপাধ্যায়" to "BANDYOPADHYAY", "মুখার্জী" to "MUKHERJEE",
        "মুখোপাধ্যায়" to "MUKHOPADHYAY", "চক্রবর্তী" to "CHAKRABORTY",
        "খান" to "KHAN", "আলি" to "ALI", "আলী" to "ALI", "কুমার" to "KUMAR",
        "শর্মা" to "SHARMA", "সিং" to "SINGH", "সিংহ" to "SINHA",
        "গুপ্ত" to "GUPTA", "মিত্র" to "MITRA", "নস্কর" to "NASKAR",
        "হালদার" to "HALDER", "বিশ্বাস" to "BISWAS", "মজুমদার" to "MAJUMDAR",
        "সাহা" to "SAHA", "কুণ্ডু" to "KUNDU", "কুন্ডু" to "KUNDU",
        "চৌধুরী" to "CHOWDHURY", "ভট্টাচার্য" to "BHATTACHARYA",
        "সৌরভ" to "SOURAV", "গাঙ্গুলী" to "GANGULY", "গঙ্গোপাধ্যায়" to "GANGOPADHYAY",
        "সুমন" to "SUMAN", "অমিত" to "AMIT", "রাহুল" to "RAHUL",
        "রাজেশ" to "RAJESH", "রাকেশ" to "RAKESH", "সুব্রত" to "SUBRATA",
        "দেবাশীষ" to "DEBASISH", "কলকাতা" to "KOLKATA",
        // Common Hindi Names & Surnames
        "मिलन" to "MILAN", "दास" to "DAS", "राहुल" to "RAHUL", "शर्मा" to "SHARMA",
        "कुमार" to "KUMAR", "सिंह" to "SINGH", "गुप्ता" to "GUPTA",
        "वर्मा" to "VERMA", "यादव" to "YADAV", "पटेल" to "PATEL",
        "खान" to "KHAN", "अली" to "ALI", "अमित" to "AMIT", "राजेश" to "RAJESH",
        "राकेश" to "RAKESH", "रहीम" to "RAHIM", "शेख" to "SHEKH"
    )

    /**
     * Phonetic transliteration of Bengali (\u0980-\u09FF) and Devanagari/Hindi (\u0900-\u097F) text into English.
     */
    fun transliterateIndicToEnglish(input: String): String {
        if (input.isBlank()) return ""
        val hasIndic = input.any { it.code in 0x0900..0x09FF }
        if (!hasIndic) return input

        // Normalize nukta/composed characters (e.g., য় -> য়, ড় -> ড়, ঢ় -> ঢ়)
        val words = input.trim().split(Regex("\\s+"))
        val translatedWords = words.map { rawWord ->
            val cleanPunct = rawWord.trim { it == ',' || it == '।' || it == '॥' }
            commonIndicWordsToEnglish[cleanPunct] ?: transliterateSingleIndicWord(cleanPunct)
        }
        return translatedWords.joinToString(" ")
    }

    private fun transliterateSingleIndicWord(word: String): String {
        if (word.isEmpty()) return ""
        val sb = StringBuilder()
        val len = word.length
        var i = 0

        fun isBengaliConsonant(c: Char): Boolean = c.code in 0x0995..0x09B9 || c.code in listOf(0x09DC, 0x09DD, 0x09DF)
        fun isHindiConsonant(c: Char): Boolean = c.code in 0x0915..0x0939 || c.code in 0x0958..0x095F
        fun isVowelSignOrVirama(c: Char): Boolean =
            c.code in 0x09BE..0x09CD || c == '\u09CC' || c.code in 0x093E..0x094D || c == '\u093C'

        while (i < len) {
            val ch = word[i]
            // Handle composed nukta pairs in Bengali/Hindi (e.g., য + ় = Y, ড + ় = R, ঢ + ় = RH)
            if (i + 1 < len && (word[i + 1] == '\u09BC' || word[i + 1] == '\u093C')) {
                val base = when (ch) {
                    'য' -> "Y"
                    'ড', 'ड' -> "R"
                    'ঢ', 'ढ' -> "RH"
                    else -> mapConsonantBase(ch)
                }
                sb.append(base)
                val afterNuktaIdx = i + 2
                val nextChar = if (afterNuktaIdx < len) word[afterNuktaIdx] else null
                if (nextChar == null || !isVowelSignOrVirama(nextChar)) {
                    if (afterNuktaIdx < len) {
                        sb.append("A")
                    }
                }
                i += 2
                continue
            }

            when {
                // Bengali & Hindi Independent Vowels
                ch == 'অ' || ch == 'अ' -> sb.append("A")
                ch == 'আ' || ch == 'आ' -> sb.append("A")
                ch == 'ই' || ch == 'ঈ' || ch == 'इ' || ch == 'ई' -> sb.append("I")
                ch == 'উ' || ch == 'ঊ' || ch == 'उ' || ch == 'ऊ' -> sb.append("U")
                ch == 'ঋ' || ch == 'ऋ' -> sb.append("RI")
                ch == 'এ' || ch == 'ए' -> sb.append("E")
                ch == 'ঐ' || ch == 'ऐ' -> sb.append("AI")
                ch == 'ও' || ch == 'ओ' -> sb.append("O")
                ch == 'ঔ' || ch == 'औ' -> sb.append("OU")

                // Dependent Vowel Signs (Matras)
                ch == 'া' || ch == 'ा' -> sb.append("A")
                ch == 'ি' || ch == 'ী' || ch == 'ि' || ch == 'ी' -> sb.append("I")
                ch == 'ু' || ch == 'ূ' || ch == 'ु' || ch == 'ू' -> sb.append("U")
                ch == 'ৃ' || ch == 'ृ' -> sb.append("RI")
                ch == 'ে' || ch == 'े' || ch == 'ॅ' || ch == 'ॆ' -> sb.append("E")
                ch == 'ৈ' || ch == 'ै' -> sb.append("AI")
                ch == 'ো' || ch == 'ो' || ch == 'ॉ' -> sb.append("O")
                ch == 'ৌ' || ch == 'ौ' -> sb.append("OU")

                // Virama / Hasanta (suppresses inherent vowel)
                ch == '্' || ch == '्' -> { /* no vowel */ }

                // Modifiers
                ch == 'ং' || ch == 'ं' -> sb.append("NG")
                ch == 'ঁ' || ch == 'ँ' -> sb.append("N")
                ch == 'ঃ' || ch == 'ः' -> sb.append("H")
                ch == 'ৎ' -> sb.append("T")

                // Consonants
                isBengaliConsonant(ch) || isHindiConsonant(ch) -> {
                    val base = mapConsonantBase(ch)
                    sb.append(base)
                    val nextChar = if (i + 1 < len) word[i + 1] else null
                    // Add inherent 'A' if not followed by a matra/virama and not the final letter of a multi-letter word
                    if (nextChar == null) {
                        if (len == 1) sb.append("A")
                    } else if (!isVowelSignOrVirama(nextChar)) {
                        sb.append("A")
                    }
                }

                // Digits (Bengali & Devanagari)
                ch in '০'..'৯' -> sb.append(('0'.code + (ch.code - '০'.code)).toChar())
                ch in '०'..'९' -> sb.append(('0'.code + (ch.code - '०'.code)).toChar())

                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    private fun mapConsonantBase(ch: Char): String {
        return when (ch) {
            'ক', 'क' -> "K"
            'খ', 'ख' -> "KH"
            'গ', 'ग' -> "G"
            'ঘ', 'घ' -> "GH"
            'ঙ', 'ङ' -> "NG"
            'চ', 'च' -> "CH"
            'ছ', 'छ' -> "CHH"
            'জ', 'ज' -> "J"
            'ঝ', 'झ' -> "JH"
            'ঞ', 'ञ' -> "N"
            'ট', 'ত', 'ट', 'त' -> "T"
            'ঠ', 'থ', 'ठ', 'थ' -> "TH"
            'ড', 'দ', 'ड', 'द' -> "D"
            'ঢ', 'ধ', 'ढ', 'ध' -> "DH"
            'ণ', 'ন', 'ण', 'न' -> "N"
            'প', 'प' -> "P"
            'ফ', 'फ' -> "PH"
            'ব', 'ब' -> "B"
            'ভ', 'भ' -> "BH"
            'ম', 'म' -> "M"
            'য' -> "J"
            'य' -> "Y"
            '\u09DF' -> "Y"
            'র', '\u09DC', 'र', '\u095C' -> "R"
            '\u09DD', '\u095D' -> "RH"
            'ল', 'ल', 'ळ' -> "L"
            'व' -> "V"
            'শ', 'ষ', 'श', 'ष' -> "SH"
            'স', 'स' -> "S"
            'হ', 'ह' -> "H"
            else -> ""
        }
    }

    // Complete 0..99 and hundreds map for Bengali, Hindi, and English spoken numbers
    private val spokenWordToNumber: Map<String, Int> = buildMap {
        // 0..19 English
        put("zero", 0); put("oh", 0); put("o", 0); put("nil", 0)
        put("one", 1); put("two", 2); put("three", 3); put("four", 4)
        put("five", 5); put("six", 6); put("seven", 7); put("eight", 8); put("nine", 9)
        put("ten", 10); put("eleven", 11); put("twelve", 12); put("thirteen", 13); put("fourteen", 14)
        put("fifteen", 15); put("sixteen", 16); put("seventeen", 17); put("eighteen", 18); put("nineteen", 19)

        // 0..99 Bengali script & common phonetic transliterations
        put("শূন্য", 0); put("জিরো", 0); put("ও", 0); put("shunya", 0); put("shunno", 0); put("sunno", 0)
        put("এক", 1); put("দুই", 2); put("তিন", 3); put("চার", 4); put("পাঁচ", 5); put("পাচ", 5)
        put("ছয়", 6); put("ছয়", 6); put("সাত", 7); put("আট", 8); put("নয়", 9); put("নয়", 9); put("দশ", 10)
        put("এগারো", 11); put("এগার", 11); put("বারো", 12); put("বার", 12); put("তেরো", 13); put("তের", 13)
        put("চৌদ্দ", 14); put("পনেরো", 15); put("পনের", 15); put("ষোলো", 16); put("ষোল", 16)
        put("সতেরো", 17); put("সতের", 17); put("আঠারো", 18); put("আঠার", 18); put("উনিশ", 19)
        put("কুড়ি", 20); put("কুড়ি", 20); put("বিশ", 20); put("একুশ", 21); put("বাইশ", 22); put("তেইশ", 23)
        put("চব্বিশ", 24); put("পঁচিশ", 25); put("পচিশ", 25); put("ছাব্বিশ", 26); put("সাতাশ", 27)
        put("আটাশ", 28); put("ঊনত্রিশ", 29); put("উনত্রিশ", 29); put("ত্রিশ", 30); put("তিরিশ", 30)
        put("একত্রিশ", 31); put("বত্রিশ", 32); put("তেত্রিশ", 33); put("চৌত্রিশ", 34); put("পঁয়ত্রিশ", 35); put("পঁয়ত্রিশ", 35)
        put("ছত্রিশ", 36); put("সাঁইত্রিশ", 37); put("সাইত্রিশ", 37); put("আটত্রিশ", 38); put("ঊনচল্লিশ", 39)
        put("চল্লিশ", 40); put("একচল্লিশ", 41); put("বিয়াল্লিশ", 42); put("বিয়াল্লিশ", 42); put("তেতাল্লিশ", 43)
        put("চুয়াল্লিশ", 44); put("চুয়াল্লিশ", 44); put("পঁয়তাল্লিশ", 45); put("পঁয়তাল্লিশ", 45); put("ছেচল্লিশ", 46)
        put("সাতচল্লিশ", 47); put("আটচল্লিশ", 48); put("ঊনপঞ্চাশ", 49); put("পঞ্চাশ", 50)
        put("একান্ন", 51); put("বাহান্ন", 52); put("তিপ্পান্ন", 53); put("চুয়ান্ন", 54); put("চুয়ান্ন", 54)
        put("পঞ্চান্ন", 55); put("ছাপ্পান্ন", 56); put("সাতান্ন", 57); put("আটান্ন", 58); put("ঊনষাট", 59)
        put("ষাট", 60); put("একষট্টি", 61); put("বাষট্টি", 62); put("তেষট্টি", 63); put("চৌষট্টি", 64)
        put("পঁয়ষট্টি", 65); put("পঁয়ষট্টি", 65); put("ছেষট্টি", 66); put("সাতষট্টি", 67); put("আটষট্টি", 68)
        put("ঊনসত্তর", 69); put("সত্তর", 70); put("একাত্তর", 71); put("বাহাত্তর", 72); put("তিয়াত্তর", 73)
        put("চুয়াত্তর", 74); put("পঁচাত্তর", 75); put("ছিয়াত্তর", 76); put("সাতাত্তর", 77); put("আটাত্তর", 78)
        put("ঊনআশি", 79); put("আশি", 80); put("একাশি", 81); put("বিরাশি", 82); put("তিরাশি", 83)
        put("চুরাশি", 84); put("পঁচাশি", 85); put("ছিয়াশি", 86); put("সাতাশি", 87); put("আটাশি", 88)
        put("ঊননব্বই", 89); put("নব্বই", 90); put("একানব্বই", 91); put("বিরানব্বই", 92); put("তিরানব্বই", 93)
        put("চুরানব্বই", 94); put("পঁচানব্বই", 95); put("ছিয়ানব্বই", 96); put("সাতানব্বই", 97); put("আটানব্বই", 98)
        put("নিরানব্বই", 99)
        put("একশো", 100); put("একশ", 100); put("দুইশো", 200); put("দুশো", 200); put("তিনশো", 300)
        put("চারশো", 400); put("পাঁচশো", 500); put("পাচশো", 500); put("দেড়শো", 150); put("দেড়শো", 150)
        put("আড়াইশো", 250); put("আড়াইশো", 250)

        // Bengali & Hindi Romanized words
        put("ek", 1); put("dui", 2); put("do", 2); put("tin", 3); put("teen", 3); put("char", 4)
        put("paanch", 5); put("paach", 5); put("panch", 5); put("chhah", 6); put("chhoy", 6); put("chhay", 6)
        put("saat", 7); put("sat", 7); put("aath", 8); put("aat", 8); put("nau", 9); put("noy", 9); put("das", 10); put("dosh", 10)
        put("egaro", 11); put("gyarah", 11); put("baro", 12); put("barah", 12); put("tero", 13); put("terah", 13)
        put("chouddo", 14); put("chaudah", 14); put("ponero", 15); put("pandrah", 15); put("sholo", 16); put("solah", 16)
        put("sotero", 17); put("satrah", 17); put("atharo", 18); put("atharah", 18); put("unish", 19); put("unnis", 19)
        put("kuri", 20); put("bish", 20); put("bees", 20); put("pachish", 25); put("pachis", 25)
        put("tirish", 30); put("tees", 30); put("chollish", 40); put("chalis", 40); put("ponchash", 50); put("pachas", 50)
        put("shat", 60); put("saath", 60); put("shottor", 70); put("sattar", 70); put("ashi", 80); put("assi", 80)
        put("nobboi", 90); put("nabbe", 90); put("eksho", 100); put("sau", 100); put("panchsho", 500)

        // Hindi Devanagari 0..50 & common numbers
        put("शून्य", 0); put("जीरो", 0); put("ओ", 0)
        put("एक", 1); put("दो", 2); put("तीन", 3); put("चार", 4); put("पांच", 5); put("पाँच", 5)
        put("छह", 6); put("छः", 6); put("सात", 7); put("आठ", 8); put("नौ", 9); put("दस", 10)
        put("ग्यारह", 11); put("बारह", 12); put("तेरह", 13); put("चौदह", 14); put("पंद्रह", 15)
        put("सोलह", 16); put("सत्रह", 17); put("अठारह", 18); put("उन्नीस", 19); put("बीस", 20)
        put("पच्चीस", 25); put("तीस", 30); put("चालीस", 40); put("पचास", 50); put("साठ", 60)
        put("सत्तर", 70); put("अस्सी", 80); put("नब्बे", 90); put("सौ", 100); put("डेढ़सौ", 150); put("ढाईसौ", 250)
    }

    private val decimalWordMap = mapOf(
        "point" to ".", "dot" to ".", "decimal" to ".",
        "दशमलव" to ".", "पॉइंट" to ".",
        "দশমিক" to ".", "পয়েন্ট" to ".", "পয়েন্ট" to ".", "doshmik" to "."
    )

    private val standaloneFractionMap = mapOf(
        "দেড়" to "1.5", "দেড়" to "1.5", "dedh" to "1.5", "der" to "1.5", "डेढ़" to "1.5",
        "আড়াই" to "2.5", "আড়াই" to "2.5", "dhai" to "2.5", "adhai" to "2.5", "arhai" to "2.5", "ढाई" to "2.5",
        "হাফ" to "0.5", "half" to "0.5", "আধা" to "0.5", "आधा" to "0.5", "adha" to "0.5"
    )

    private val multiplierMap = mapOf(
        "double" to 2, "ডাবল" to 2, "ডবল" to 2, "डबल" to 2,
        "triple" to 3, "ট্রিপল" to 3, "ट्रिपल" to 3,
        "quadruple" to 4
    )

    private fun normalizeIndicDigitsToAscii(rawText: String): String {
        val sb = StringBuilder(rawText.length)
        for (ch in rawText) {
            when (ch) {
                in '०'..'९' -> sb.append(('0'.code + (ch.code - '०'.code)).toChar())
                in '০'..'৯' -> sb.append(('0'.code + (ch.code - '০'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Extracts English numeric digits from Hindi, Bengali, or English spoken text.
     * Supports:
     * - "double zero" -> "00", "triple one" -> "111"
     * - Bengali & Hindi spoken numbers (1..99, 100, 150, 250, 500)
     * - Spoken fractions ("দেড়" -> "1.5", "আড়াই" -> "2.5", "সওয়া দুই" -> "2.25", "পৌনে তিন" -> "2.75", "হাফ" -> "0.5")
     */
    fun extractDigitsAndNumbers(rawText: String): String {
        if (rawText.isBlank()) return ""
        val text = normalizeIndicDigitsToAscii(rawText.trim())
            .lowercase(Locale.ROOT)
            .replace(Regex("\\b(kg|kilogram|gram|gm|g|কেজি|গ্রাম|किलो|ग्राम)\\b"), " ")
            .trim()

        val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        // Check for multi-word fractional expressions first (e.g. "সওয়া দুই" -> "2.25", "পৌনে তিন" -> "2.75", "দেড় শ" -> "150")
        if (tokens.size in 1..3) {
            val first = tokens[0]
            if (tokens.size == 1 && standaloneFractionMap.containsKey(first)) {
                return standaloneFractionMap[first]!!
            }
            if (tokens.size == 2) {
                val second = tokens[1]
                if ((first == "দেড়" || first == "দেড়" || first == "dedh" || first == "डेढ़") &&
                    (second == "শো" || second == "শ" || second == "sau" || second == "सौ")
                ) {
                    return "150"
                }
                if ((first == "আড়াই" || first == "আড়াই" || first == "dhai" || first == "ढाई") &&
                    (second == "শো" || second == "শ" || second == "sau" || second == "सौ")
                ) {
                    return "250"
                }
                val secondNum = spokenWordToNumber[second] ?: second.toIntOrNull()
                if (secondNum != null) {
                    if (first in setOf("সওয়া", "সোয়া", "sawa", "सवा")) {
                        return "$secondNum.25"
                    }
                    if (first in setOf("সাড়ে", "সাড়ে", "sade", "sadhe", "साढ़े")) {
                        return "$secondNum.5"
                    }
                    if (first in setOf("পৌনে", "paune", "पौने") && secondNum >= 1) {
                        return "${secondNum - 1}.75"
                    }
                }
            }
        }

        val resultBuilder = StringBuilder()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]

            // 1. Handle "double X" / "triple X" multiplier
            val repeatCount = multiplierMap[token]
            if (repeatCount != null && idx + 1 < tokens.size) {
                val nextToken = tokens[idx + 1]
                val nextDigit = spokenWordToNumber[nextToken]?.toString()
                    ?: nextToken.filter { it.isDigit() }.takeIf { it.isNotEmpty() }
                if (nextDigit != null) {
                    repeat(repeatCount) { resultBuilder.append(nextDigit) }
                    idx += 2
                    continue
                }
            }

            // 2. Handle standalone fraction token ("দেড়" -> "1.5", "আড়াই" -> "2.5")
            val fractionVal = standaloneFractionMap[token]
            if (fractionVal != null) {
                resultBuilder.append(fractionVal)
                idx++
                continue
            }

            // 3. Handle decimal point word ("point", "দশমিক", "दशमलव")
            val decDot = decimalWordMap[token]
            if (decDot != null) {
                if (!resultBuilder.endsWith(".")) {
                    resultBuilder.append(".")
                }
                idx++
                continue
            }

            // 4. Handle English compound tens ("twenty five" -> "25") when not part of a 6-digit digit-by-digit stream
            val tensMap = mapOf(
                "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
                "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
            )
            val tensVal = tensMap[token]
            if (tensVal != null) {
                if (idx + 1 < tokens.size) {
                    val unitVal = spokenWordToNumber[tokens[idx + 1]]
                    if (unitVal != null && unitVal in 1..9) {
                        resultBuilder.append((tensVal + unitVal).toString())
                        idx += 2
                        continue
                    }
                }
                resultBuilder.append(tensVal.toString())
                idx++
                continue
            }

            // 5. Handle mapped number word (0..99, 100, 150, 250, 500)
            val mappedNumber = spokenWordToNumber[token]
            if (mappedNumber != null) {
                resultBuilder.append(mappedNumber.toString())
                idx++
                continue
            }

            // 6. Extract ASCII digits / decimal points directly
            val filtered = token.filter { it.isDigit() || it == '.' }
            resultBuilder.append(filtered)
            idx++
        }

        val result = resultBuilder.toString()
        if (result.isBlank()) {
            return text.filter { it.isDigit() || it == '.' }
        }
        return result
    }

    /**
     * Extracts a list of separate numeric values from spoken text.
     * Used for Dimensions where user speaks multiple values like:
     * - "12 23 4", "12 by 23 by 4", "12 x 23 x 4"
     * - "বারো বাই পনেরো বাই আট" -> ["12", "15", "8"]
     * - "দেড় বাই আড়াই বাই চার" -> ["1.5", "2.5", "4"]
     */
    fun extractMultipleNumbers(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        val text = normalizeIndicDigitsToAscii(rawText)

        val tensMap = mapOf(
            "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
            "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
        )

        // Replace dimension words / separators like "x", "by", "বাই", "গুণ", "into", "*", "," with space
        val sanitized = text.lowercase(Locale.ROOT)
            .replace(Regex("[xX\\*,×]"), " ")
            .replace(
                Regex("\\b(by|into|cross|gun|guna|and|thaka|taka|length|breadth|height|l|b|h|বাই|গুণ|গুন|ইনটু|ক্রস|এবং|আর|बाय|गुणा|और)\\b"),
                " "
            )

        val tokens = sanitized.split(Regex("\\s+")).filter { it.isNotBlank() }
        val numbersList = mutableListOf<String>()

        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]

            // Handle standalone fractions ("দেড়" -> "1.5", "আড়াই" -> "2.5", "হাফ" -> "0.5")
            val fractionVal = standaloneFractionMap[token]
            if (fractionVal != null) {
                numbersList.add(fractionVal)
                idx++
                continue
            }

            // Handle English compound tens ("twenty three" -> "23")
            val tensVal = tensMap[token]
            if (tensVal != null) {
                if (idx + 1 < tokens.size) {
                    val nextVal = spokenWordToNumber[tokens[idx + 1]]
                    if (nextVal != null && nextVal in 1..9) {
                        numbersList.add((tensVal + nextVal).toString())
                        idx += 2
                        continue
                    }
                }
                numbersList.add(tensVal.toString())
                idx++
                continue
            }

            // Handle mapped number words (0..99, 100, etc.) with optional decimal ("বারো দশমিক পাঁচ" -> "12.5")
            val wordVal = spokenWordToNumber[token]
            if (wordVal != null) {
                if (idx + 2 < tokens.size && decimalWordMap.containsKey(tokens[idx + 1])) {
                    val decPart = spokenWordToNumber[tokens[idx + 2]]?.toString()
                        ?: tokens[idx + 2].filter { it.isDigit() }
                    if (decPart.isNotEmpty()) {
                        numbersList.add("$wordVal.$decPart")
                        idx += 3
                        continue
                    }
                }
                numbersList.add(wordVal.toString())
                idx++
                continue
            }

            // Handle raw numeric tokens ("12", "12.5", or "12" followed by "point 5")
            val digitsOnly = token.filter { it.isDigit() || it == '.' }
            if (digitsOnly.isNotBlank()) {
                if (!digitsOnly.contains(".") && idx + 2 < tokens.size && decimalWordMap.containsKey(tokens[idx + 1])) {
                    val decPart = spokenWordToNumber[tokens[idx + 2]]?.toString()
                        ?: tokens[idx + 2].filter { it.isDigit() }
                    if (decPart.isNotEmpty()) {
                        numbersList.add("$digitsOnly.$decPart")
                        idx += 3
                        continue
                    }
                }
                numbersList.add(digitsOnly)
            }
            idx++
        }

        return numbersList
    }
}
