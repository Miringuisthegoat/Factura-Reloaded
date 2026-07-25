package com.benjamin.factura.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.benjamin.factura.data.model.BusinessProfile
import com.benjamin.factura.data.model.Client
import com.benjamin.factura.data.model.Expense
import com.benjamin.factura.data.model.ExpenseCategory
import com.benjamin.factura.data.model.Invoice
import com.benjamin.factura.data.model.InvoiceItem
import com.benjamin.factura.data.model.InvoiceStatus
import com.benjamin.factura.data.model.InvoiceType
import com.benjamin.factura.data.model.Payment
import com.benjamin.factura.data.model.PaymentMethod
import com.benjamin.factura.data.model.SyncStatus

@Entity(tableName = "business_profiles")
data class BusinessProfileEntity(
    @PrimaryKey val id: String,
    val businessName: String,
    val logo: String,
    val address: String,
    val phone: String,
    val email: String,
    val taxNumber: String,
    val bankName: String,
    val bankAccountName: String,
    val bankAccountNumber: String,
    val defaultCurrency: String,
    val defaultTaxRatePercent: Float,
    val defaultPaymentTermsDays: Int,
    val invoiceNumberPrefix: String
) {
    fun toDomain(): BusinessProfile = BusinessProfile(
        id = id,
        businessName = businessName,
        logo = logo,
        address = address,
        phone = phone,
        email = email,
        taxNumber = taxNumber,
        bankName = bankName,
        bankAccountName = bankAccountName,
        bankAccountNumber = bankAccountNumber,
        defaultCurrency = defaultCurrency,
        defaultTaxRatePercent = defaultTaxRatePercent,
        defaultPaymentTermsDays = defaultPaymentTermsDays,
        invoiceNumberPrefix = invoiceNumberPrefix
    )

    companion object {
        fun fromDomain(domain: BusinessProfile) = BusinessProfileEntity(
            id = domain.id,
            businessName = domain.businessName,
            logo = domain.logo,
            address = domain.address,
            phone = domain.phone,
            email = domain.email,
            taxNumber = domain.taxNumber,
            bankName = domain.bankName,
            bankAccountName = domain.bankAccountName,
            bankAccountNumber = domain.bankAccountNumber,
            defaultCurrency = domain.defaultCurrency,
            defaultTaxRatePercent = domain.defaultTaxRatePercent,
            defaultPaymentTermsDays = domain.defaultPaymentTermsDays,
            invoiceNumberPrefix = domain.invoiceNumberPrefix
        )
    }
}

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val taxNumber: String,
    val notes: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val isDeleted: Boolean,
    val syncStatus: SyncStatus
) {
    fun toDomain(): Client = Client(
        id = id,
        businessId = businessId,
        name = name,
        email = email,
        phone = phone,
        address = address,
        taxNumber = taxNumber,
        notes = notes,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        isDeleted = isDeleted,
        syncStatus = syncStatus
    )

    companion object {
        fun fromDomain(domain: Client) = ClientEntity(
            id = domain.id,
            businessId = domain.businessId,
            name = domain.name,
            email = domain.email,
            phone = domain.phone,
            address = domain.address,
            taxNumber = domain.taxNumber,
            notes = domain.notes,
            createdAtMillis = domain.createdAtMillis,
            updatedAtMillis = domain.updatedAtMillis,
            isDeleted = domain.isDeleted,
            syncStatus = domain.syncStatus
        )
    }
}

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val invoiceNumber: String,
    val clientId: String,
    val clientName: String,
    val status: InvoiceStatus,
    val type: InvoiceType,
    val issueDateMillis: Long,
    val dueDateMillis: Long,
    val items: List<InvoiceItem>,
    val subtotal: Double,
    val discountTotal: Double,
    val taxTotal: Double,
    val total: Double,
    val amountPaid: Double,
    val balanceDue: Double,
    val currencyCode: String,
    val notes: String,
    val terms: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val isDeleted: Boolean,
    val syncStatus: SyncStatus
) {
    fun toDomain(): Invoice = Invoice(
        id = id,
        businessId = businessId,
        invoiceNumber = invoiceNumber,
        clientId = clientId,
        clientName = clientName,
        status = status,
        type = type,
        issueDateMillis = issueDateMillis,
        dueDateMillis = dueDateMillis,
        items = items,
        subtotal = subtotal,
        discountTotal = discountTotal,
        taxTotal = taxTotal,
        total = total,
        amountPaid = amountPaid,
        balanceDue = balanceDue,
        currencyCode = currencyCode,
        notes = notes,
        terms = terms,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        isDeleted = isDeleted,
        syncStatus = syncStatus
    )

    companion object {
        fun fromDomain(domain: Invoice) = InvoiceEntity(
            id = domain.id,
            businessId = domain.businessId,
            invoiceNumber = domain.invoiceNumber,
            clientId = domain.clientId,
            clientName = domain.clientName,
            status = domain.status,
            type = domain.type,
            issueDateMillis = domain.issueDateMillis,
            dueDateMillis = domain.dueDateMillis,
            items = domain.items,
            subtotal = domain.subtotal,
            discountTotal = domain.discountTotal,
            taxTotal = domain.taxTotal,
            total = domain.total,
            amountPaid = domain.amountPaid,
            balanceDue = domain.balanceDue,
            currencyCode = domain.currencyCode,
            notes = domain.notes,
            terms = domain.terms,
            createdAtMillis = domain.createdAtMillis,
            updatedAtMillis = domain.updatedAtMillis,
            isDeleted = domain.isDeleted,
            syncStatus = domain.syncStatus
        )
    }
}

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val invoiceId: String,
    val invoiceNumber: String,
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val referenceNumber: String,
    val notes: String,
    val paidAtMillis: Long,
    val createdAtMillis: Long,
    val isDeleted: Boolean,
    val syncStatus: SyncStatus
) {
    fun toDomain(): Payment = Payment(
        id = id,
        businessId = businessId,
        invoiceId = invoiceId,
        invoiceNumber = invoiceNumber,
        amount = amount,
        paymentMethod = paymentMethod,
        referenceNumber = referenceNumber,
        notes = notes,
        paidAtMillis = paidAtMillis,
        createdAtMillis = createdAtMillis,
        isDeleted = isDeleted,
        syncStatus = syncStatus
    )

    companion object {
        fun fromDomain(domain: Payment) = PaymentEntity(
            id = domain.id,
            businessId = domain.businessId,
            invoiceId = domain.invoiceId,
            invoiceNumber = domain.invoiceNumber,
            amount = domain.amount,
            paymentMethod = domain.paymentMethod,
            referenceNumber = domain.referenceNumber,
            notes = domain.notes,
            paidAtMillis = domain.paidAtMillis,
            createdAtMillis = domain.createdAtMillis,
            isDeleted = domain.isDeleted,
            syncStatus = domain.syncStatus
        )
    }
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val vendor: String,
    val category: ExpenseCategory,
    val amount: Double,
    val expenseDateMillis: Long,
    val receiptImageUrl: String?,
    val notes: String,
    val ocrText: String,
    val createdAtMillis: Long,
    val isDeleted: Boolean,
    val syncStatus: SyncStatus
) {
    fun toDomain(): Expense = Expense(
        id = id,
        businessId = businessId,
        vendor = vendor,
        category = category,
        amount = amount,
        expenseDateMillis = expenseDateMillis,
        receiptImageUrl = receiptImageUrl,
        notes = notes,
        ocrText = ocrText,
        createdAtMillis = createdAtMillis,
        isDeleted = isDeleted,
        syncStatus = syncStatus
    )

    companion object {
        fun fromDomain(domain: Expense) = ExpenseEntity(
            id = domain.id,
            businessId = domain.businessId,
            vendor = domain.vendor,
            category = domain.category,
            amount = domain.amount,
            expenseDateMillis = domain.expenseDateMillis,
            receiptImageUrl = domain.receiptImageUrl,
            notes = domain.notes,
            ocrText = domain.ocrText,
            createdAtMillis = domain.createdAtMillis,
            isDeleted = domain.isDeleted,
            syncStatus = domain.syncStatus
        )
    }
}
