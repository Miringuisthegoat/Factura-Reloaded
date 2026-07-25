package com.benjamin.factura.util

import android.graphics.Bitmap
import com.benjamin.factura.data.model.ExpenseCategory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ScannedExpenseResult(
    val vendor: String,
    val amount: Double,
    val category: ExpenseCategory,
    val fullText: String
)

object OcrScanner {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun scanReceiptBitmap(bitmap: Bitmap): ScannedExpenseResult = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                val lines = visionText.textBlocks.flatMap { it.lines.map { l -> l.text } }

                val vendor = extractVendor(lines)
                val amount = extractAmount(fullText)
                val category = guessCategory(fullText)

                continuation.resume(ScannedExpenseResult(vendor, amount, category, fullText))
            }
            .addOnFailureListener {
                continuation.resume(ScannedExpenseResult("", 0.0, ExpenseCategory.OFFICE_SUPPLIES, ""))
            }
    }

    private fun extractVendor(lines: List<String>): String {
        if (lines.isEmpty()) return "Scanned Merchant"
        // Most receipts have vendor name in first 1-3 lines
        for (line in lines.take(3)) {
            val trimmed = line.trim()
            if (trimmed.length in 3..30 && !trimmed.contains("TOTAL", ignoreCase = true) && !trimmed.contains("TAX", ignoreCase = true)) {
                return trimmed
            }
        }
        return lines.firstOrNull()?.take(25) ?: "Scanned Merchant"
    }

    private fun extractAmount(text: String): Double {
        // Find currency patterns like KES 1500, KSh 2,400.00, TOTAL 500, etc.
        val regex = Regex("""(?:KES|KSH|TOTAL|AMOUNT|\$)\s*[:=]?\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        if (match != null) {
            val numberStr = match.groupValues[1].replace(",", "")
            return numberStr.toDoubleOrNull() ?: 0.0
        }

        // Secondary fallback: find highest standalone number with decimal
        val numberRegex = Regex("""\b([0-9]{1,6}\.[0-9]{2})\b""")
        val numbers = numberRegex.findAll(text).mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }.toList()
        return numbers.maxOrNull() ?: 0.0
    }

    private fun guessCategory(text: String): ExpenseCategory {
        val lower = text.lowercase()
        return when {
            lower.contains("supermarket") || lower.contains("stationery") || lower.contains("paper") -> ExpenseCategory.OFFICE_SUPPLIES
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("uber") || lower.contains("taxi") -> ExpenseCategory.TRAVEL
            lower.contains("cafe") || lower.contains("restaurant") || lower.contains("coffee") || lower.contains("hotel") || lower.contains("food") -> ExpenseCategory.MEALS_ENTERTAINMENT
            lower.contains("power") || lower.contains("kplc") || lower.contains("water") || lower.contains("internet") || lower.contains("safaricom") -> ExpenseCategory.UTILITIES
            lower.contains("computer") || lower.contains("laptop") || lower.contains("hardware") || lower.contains("phone") -> ExpenseCategory.EQUIPMENT
            else -> ExpenseCategory.OFFICE_SUPPLIES
        }
    }
}
