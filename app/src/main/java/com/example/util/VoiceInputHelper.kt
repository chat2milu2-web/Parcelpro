package com.example.util

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

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
                arrayOf("hi-IN", "bn-IN", "en-US", "en-GB")
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptMessage)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
    }

    /**
     * Parses spoken voice results into clean English text or digits.
     * Supports Hindi, Bengali, and English spoken inputs.
     */
    fun parseVoiceResult(spokenText: String, isNumericField: Boolean = false): String {
        if (spokenText.isBlank()) return ""

        if (isNumericField) {
            return extractDigitsAndNumbers(spokenText)
        }

        // For text fields (like Addressee Name), convert to clean uppercase English text
        val cleaned = spokenText.trim()
            .replace(Regex("[^a-zA-Z0-9\\s\\.\\-]"), "")
            .replace(Regex("\\s+"), " ")
            .uppercase(Locale.ENGLISH)

        return cleaned
    }

    /**
     * Extracts English numeric digits from Hindi, Bengali, or English spoken text.
     * Maps Devanagari and Bengali numeral characters to ASCII 0-9, and handles spoken number words.
     */
    fun extractDigitsAndNumbers(rawText: String): String {
        var text = rawText.trim()

        // 1. Map Bengali and Devanagari digits to ASCII digits
        val convertedDigits = StringBuilder()
        for (ch in text) {
            when (ch) {
                // Devanagari digits ०-९
                '०' -> convertedDigits.append('0')
                '१' -> convertedDigits.append('1')
                '२' -> convertedDigits.append('2')
                '३' -> convertedDigits.append('3')
                '४' -> convertedDigits.append('4')
                '५' -> convertedDigits.append('5')
                '६' -> convertedDigits.append('6')
                '७' -> convertedDigits.append('7')
                '८' -> convertedDigits.append('8')
                '९' -> convertedDigits.append('9')
                // Bengali digits ০-৯
                '০' -> convertedDigits.append('0')
                '১' -> convertedDigits.append('1')
                '২' -> convertedDigits.append('2')
                '৩' -> convertedDigits.append('3')
                '৪' -> convertedDigits.append('4')
                '৫' -> convertedDigits.append('5')
                '৬' -> convertedDigits.append('6')
                '৭' -> convertedDigits.append('7')
                '৮' -> convertedDigits.append('8')
                '৯' -> convertedDigits.append('9')
                else -> convertedDigits.append(ch)
            }
        }
        text = convertedDigits.toString()

        // 2. Map spoken number words (Hindi, Bengali, English) to digits
        val wordToDigit = mapOf(
            // English digits
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
            "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
            "point" to ".", "dot" to ".",

            // Hindi spoken words (transliterated & devanagari)
            "शून्य" to "0", "एक" to "1", "दो" to "2", "तीन" to "3", "चार" to "4",
            "पांच" to "5", "छह" to "6", "सात" to "7", "आठ" to "8", "नौ" to "9",
            "दशमलव" to ".", "shunya" to "0", "ek" to "1", "do" to "2", "teen" to "3",
            "char" to "4", "paanch" to "5", "chhah" to "6", "saat" to "7", "aath" to "8", "nau" to "9",

            // Bengali spoken words (transliterated & bengali script)
            "শূন্য" to "0", "এক" to "1", "দুই" to "2", "তিন" to "3", "চার" to "4",
            "পাঁচ" to "5", "ছয়" to "6", "সাত" to "7", "আট" to "8", "নয়" to "9",
            "দশমিক" to ".", "doshmik" to ".", "dui" to "2", "tin" to "3", "paach" to "5", "chhoy" to "6"
        )

        // Lowercase for comparison
        val tokens = text.lowercase(Locale.ROOT).split(Regex("\\s+"))
        val resultBuilder = StringBuilder()

        for (token in tokens) {
            val mapped = wordToDigit[token]
            if (mapped != null) {
                resultBuilder.append(mapped)
            } else {
                // Extract any existing ASCII digits or decimal points directly in the token
                val filtered = token.filter { it.isDigit() || it == '.' }
                resultBuilder.append(filtered)
            }
        }

        val result = resultBuilder.toString()
        // If result is empty, return digits extracted from original string as fallback
        if (result.isBlank()) {
            return rawText.filter { it.isDigit() || it == '.' }
        }
        return result
    }

    /**
     * Extracts a list of separate numeric values from spoken text.
     * Used for Dimensions where user speaks multiple values like "12 23 4", "12 by 23 by 4", "12 x 23 x 4".
     */
    fun extractMultipleNumbers(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        // 1. Map Bengali and Devanagari digits to ASCII digits
        val converted = StringBuilder()
        for (ch in rawText) {
            when (ch) {
                '०' -> converted.append('0')
                '१' -> converted.append('1')
                '२' -> converted.append('2')
                '३' -> converted.append('3')
                '४' -> converted.append('4')
                '५' -> converted.append('5')
                '६' -> converted.append('6')
                '७' -> converted.append('7')
                '८' -> converted.append('8')
                '९' -> converted.append('9')
                '০' -> converted.append('0')
                '১' -> converted.append('1')
                '২' -> converted.append('2')
                '৩' -> converted.append('3')
                '৪' -> converted.append('4')
                '৫' -> converted.append('5')
                '৬' -> converted.append('6')
                '৭' -> converted.append('7')
                '৮' -> converted.append('8')
                '৯' -> converted.append('9')
                else -> converted.append(ch)
            }
        }

        val text = converted.toString()

        val tensMap = mapOf(
            "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
            "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
        )

        val unitsMap = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
            "शून्य" to 0, "एक" to 1, "दो" to 2, "तीन" to 3, "चार" to 4,
            "पांच" to 5, "छह" to 6, "सात" to 7, "आठ" to 8, "नौ" to 9, "दस" to 10,
            "শূন্য" to 0, "এক" to 1, "দুই" to 2, "তিন" to 3, "চার" to 4,
            "পাঁচ" to 5, "ছয়" to 6, "সাত" to 7, "আট" to 8, "নয়" to 9, "দশ" to 10
        )

        // Replace dimension words / separators like "x", "by", "cross", "into", "*", "," with space
        val sanitized = text.lowercase(Locale.ROOT)
            .replace(Regex("[xX\\*,]"), " ")
            .replace(Regex("\\b(by|into|cross|gun|guna|and|thaka|taka|length|breadth|height|l|b|h)\\b"), " ")

        val tokens = sanitized.split(Regex("\\s+")).filter { it.isNotBlank() }
        val numbersList = mutableListOf<String>()

        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            val tensVal = tensMap[token]
            if (tensVal != null) {
                // Check if next token is a unit (1..9)
                if (idx + 1 < tokens.size) {
                    val nextVal = unitsMap[tokens[idx + 1]]
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

            val unitVal = unitsMap[token]
            if (unitVal != null) {
                numbersList.add(unitVal.toString())
                idx++
                continue
            }

            // Check if token contains digits (e.g. "12", "23", "4", "12.5")
            val digitsOnly = token.filter { it.isDigit() || it == '.' }
            if (digitsOnly.isNotBlank()) {
                numbersList.add(digitsOnly)
            }
            idx++
        }

        return numbersList
    }
}
