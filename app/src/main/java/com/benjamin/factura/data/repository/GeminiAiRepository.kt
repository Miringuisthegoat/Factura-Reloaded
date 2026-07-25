package com.benjamin.factura.data.repository

import com.benjamin.factura.BuildConfig
import com.benjamin.factura.data.model.Invoice
import com.benjamin.factura.data.model.InvoiceItem
import com.benjamin.factura.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiAiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

    /**
     * Gemini Auto-Fill: Suggest line items based on past invoices for a returning client.
     */
    suspend fun suggestInvoiceItemsForClient(
        clientName: String,
        recentInvoices: List<Invoice>
    ): List<InvoiceItem> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || recentInvoices.isEmpty()) return@withContext emptyList()

        val historySummary = StringBuilder()
        recentInvoices.take(5).forEachIndexed { idx, inv ->
            historySummary.append("Invoice #${inv.invoiceNumber} (${Formatters.formatDate(inv.issueDateMillis)}):\n")
            inv.items.forEach { item ->
                historySummary.append(" - ${item.description}: qty ${item.quantity}, price KES ${item.unitPrice}\n")
            }
        }

        val prompt = """
            You are an AI invoicing assistant for Factura in Kenya.
            Client Name: "$clientName".
            Past Invoice History for this client:
            $historySummary

            Based on the history above, suggest the 2 to 4 most likely invoice line items for a new invoice.
            Return ONLY a JSON array of items with no markdown or formatting outside JSON.
            JSON Format:
            [
              {
                "description": "Item description",
                "quantity": 1.0,
                "unitPrice": 5000.0,
                "taxRatePercent": 16.0
              }
            ]
        """.trimIndent()

        try {
            val responseText = callGeminiApi(prompt) ?: return@withContext emptyList()
            parseSuggestedItemsJson(responseText)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gemini Smart Reminder: Draft a friendly, professional Kenyan SMS/Email payment reminder.
     */
    suspend fun generatePaymentReminder(
        clientName: String,
        invoiceNumber: String,
        amountDue: Double,
        currencyCode: String,
        daysOverdue: Long
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "Dear $clientName, friendly reminder that invoice #$invoiceNumber for $currencyCode $amountDue is overdue by $daysOverdue days. Kindly arrange payment at your earliest convenience. Thank you!"
        }

        val prompt = """
            Generate a short, polite, professional, and friendly SMS/Email payment reminder message for a Kenyan business client.
            Client Name: $clientName
            Invoice Number: $invoiceNumber
            Amount Due: $currencyCode ${Formatters.formatCurrency(amountDue, currencyCode)}
            Days Overdue: $daysOverdue days

            Keep it concise, clear, polite, and include bank or M-Pesa payment context.
        """.trimIndent()

        try {
            callGeminiApi(prompt) ?: "Dear $clientName, friendly reminder regarding overdue Invoice #$invoiceNumber for $currencyCode $amountDue."
        } catch (e: Exception) {
            "Dear $clientName, friendly reminder regarding overdue Invoice #$invoiceNumber."
        }
    }

    private fun callGeminiApi(promptText: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", promptText) })
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null
            val responseObj = JSONObject(bodyString)
            val candidates = responseObj.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            return parts.getJSONObject(0).optString("text", "")
        }
    }

    private fun parseSuggestedItemsJson(rawText: String): List<InvoiceItem> {
        val items = mutableListOf<InvoiceItem>()
        try {
            val cleaned = rawText.replace("```json", "").replace("```", "").trim()
            val array = JSONArray(cleaned)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                items.add(
                    InvoiceItem(
                        description = obj.optString("description", "Services"),
                        quantity = obj.optDouble("quantity", 1.0),
                        unitPrice = obj.optDouble("unitPrice", 1000.0),
                        taxRatePercent = obj.optDouble("taxRatePercent", 16.0)
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return items
    }
}
