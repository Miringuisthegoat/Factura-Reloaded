package com.benjamin.factura.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun formatCurrency(amount: Double, currencyCode: String = "KES"): String {
        return try {
            val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            "$currencyCode ${formatter.format(amount)}"
        } catch (e: Exception) {
            "$currencyCode ${String.format(Locale.US, "%.2f", amount)}"
        }
    }

    fun formatDate(timeMillis: Long, formatPattern: String = "dd MMM yyyy"): String {
        return try {
            val sdf = SimpleDateFormat(formatPattern, Locale.getDefault())
            sdf.format(Date(timeMillis))
        } catch (e: Exception) {
            ""
        }
    }

    fun parseDateToMillis(dateStr: String, formatPattern: String = "yyyy-MM-dd"): Long? {
        return try {
            val sdf = SimpleDateFormat(formatPattern, Locale.getDefault())
            sdf.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
}
