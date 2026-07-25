package com.benjamin.factura.data.local

import androidx.room.TypeConverter
import com.benjamin.factura.data.model.ExpenseCategory
import com.benjamin.factura.data.model.Invoice
import com.benjamin.factura.data.model.InvoiceItem
import com.benjamin.factura.data.model.InvoiceStatus
import com.benjamin.factura.data.model.InvoiceType
import com.benjamin.factura.data.model.PaymentMethod
import com.benjamin.factura.data.model.SyncStatus

class Converters {
    @TypeConverter
    fun fromInvoiceItemList(value: List<InvoiceItem>?): String {
        return value?.let { Invoice.serializeItems(it) } ?: "[]"
    }

    @TypeConverter
    fun toInvoiceItemList(value: String?): List<InvoiceItem> {
        return value?.let { Invoice.deserializeItems(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromInvoiceStatus(status: InvoiceStatus): String = status.name

    @TypeConverter
    fun toInvoiceStatus(value: String): InvoiceStatus = try {
        InvoiceStatus.valueOf(value)
    } catch (e: Exception) {
        InvoiceStatus.DRAFT
    }

    @TypeConverter
    fun fromInvoiceType(type: InvoiceType): String = type.name

    @TypeConverter
    fun toInvoiceType(value: String): InvoiceType = try {
        InvoiceType.valueOf(value)
    } catch (e: Exception) {
        InvoiceType.INVOICE
    }

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String = method.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: Exception) {
        PaymentMethod.MPESA
    }

    @TypeConverter
    fun fromExpenseCategory(category: ExpenseCategory): String = category.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = try {
        ExpenseCategory.valueOf(value)
    } catch (e: Exception) {
        ExpenseCategory.OFFICE_SUPPLIES
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = try {
        SyncStatus.valueOf(value)
    } catch (e: Exception) {
        SyncStatus.PENDING_INSERT
    }
}
