package com.benjamin.factura.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class InvoiceStatus {
    DRAFT,
    SENT,
    VIEWED,
    PARTIAL,
    PAID,
    OVERDUE
}

enum class InvoiceType {
    INVOICE,
    ESTIMATE
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    BANK_TRANSFER("Bank Transfer"),
    MPESA("M-Pesa"),
    CARD("Card"),
    CHEQUE("Cheque")
}

enum class ExpenseCategory(val label: String) {
    OFFICE_SUPPLIES("Office Supplies"),
    TRAVEL("Travel & Mileage"),
    MEALS_ENTERTAINMENT("Meals & Entertainment"),
    UTILITIES("Utilities & Internet"),
    RENT("Rent & Operations"),
    MARKETING("Marketing & Ads"),
    PROFESSIONAL_SERVICES("Professional Services"),
    EQUIPMENT("Equipment & Tech"),
    INSURANCE("Insurance & Licenses")
}

enum class SyncStatus {
    SYNCED,
    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE
}

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = ""
)

data class BusinessProfile(
    val id: String = "default_business",
    val businessName: String = "My Business",
    val logo: String = "",
    val address: String = "Nairobi, Kenya",
    val phone: String = "+254 700 000000",
    val email: String = "info@business.co.ke",
    val taxNumber: String = "P051234567Z",
    val bankName: String = "KCB Bank / M-Pesa Paybill",
    val bankAccountName: String = "My Business Ltd",
    val bankAccountNumber: String = "Paybill: 522522, Acc: 1234567",
    val defaultCurrency: String = "KES",
    val defaultTaxRatePercent: Float = 16.0f,
    val defaultPaymentTermsDays: Int = 30,
    val invoiceNumberPrefix: String = "INV-"
)

data class Client(
    val id: String = UUID.randomUUID().toString(),
    val businessId: String = "default_business",
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val taxNumber: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT
)

data class InvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxRatePercent: Double = 16.0
) {
    val subtotal: Double get() = quantity * unitPrice - discountAmount
    val taxAmount: Double get() = subtotal * (taxRatePercent / 100.0)
    val total: Double get() = subtotal + taxAmount

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("description", description)
            put("quantity", quantity)
            put("unitPrice", unitPrice)
            put("discountAmount", discountAmount)
            put("taxRatePercent", taxRatePercent)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): InvoiceItem {
            return InvoiceItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                description = json.optString("description", ""),
                quantity = json.optDouble("quantity", 1.0),
                unitPrice = json.optDouble("unitPrice", 0.0),
                discountAmount = json.optDouble("discountAmount", 0.0),
                taxRatePercent = json.optDouble("taxRatePercent", 16.0)
            )
        }
    }
}

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val businessId: String = "default_business",
    val invoiceNumber: String,
    val clientId: String,
    val clientName: String,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val type: InvoiceType = InvoiceType.INVOICE,
    val issueDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val items: List<InvoiceItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountTotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val total: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val currencyCode: String = "KES",
    val notes: String = "",
    val terms: String = "Payment due within 30 days.",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT
) {
    companion object {
        fun serializeItems(items: List<InvoiceItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun deserializeItems(jsonString: String): List<InvoiceItem> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<InvoiceItem>()
                for (i in 0 until array.length()) {
                    list.add(InvoiceItem.fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val businessId: String = "default_business",
    val invoiceId: String,
    val invoiceNumber: String = "",
    val amount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.MPESA,
    val referenceNumber: String = "",
    val notes: String = "",
    val paidAtMillis: Long = System.currentTimeMillis(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT
)

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val businessId: String = "default_business",
    val vendor: String,
    val category: ExpenseCategory = ExpenseCategory.OFFICE_SUPPLIES,
    val amount: Double,
    val expenseDateMillis: Long = System.currentTimeMillis(),
    val receiptImageUrl: String? = null,
    val notes: String = "",
    val ocrText: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT
)

data class InvoiceSummaryStats(
    val totalRevenue: Double = 0.0,
    val monthlyRevenue: Double = 0.0,
    val outstanding: Double = 0.0,
    val overdue: Double = 0.0,
    val invoiceCount: Int = 0,
    val clientCount: Int = 0
)
