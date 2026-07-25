package com.benjamin.factura.data.repository

import com.benjamin.factura.data.local.AppDatabase
import com.benjamin.factura.data.local.entities.BusinessProfileEntity
import com.benjamin.factura.data.local.entities.ClientEntity
import com.benjamin.factura.data.local.entities.ExpenseEntity
import com.benjamin.factura.data.local.entities.InvoiceEntity
import com.benjamin.factura.data.local.entities.PaymentEntity
import com.benjamin.factura.data.model.BusinessProfile
import com.benjamin.factura.data.model.Client
import com.benjamin.factura.data.model.Expense
import com.benjamin.factura.data.model.ExpenseCategory
import com.benjamin.factura.data.model.Invoice
import com.benjamin.factura.data.model.InvoiceStatus
import com.benjamin.factura.data.model.InvoiceSummaryStats
import com.benjamin.factura.data.model.Payment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

class FacturaRepository(private val database: AppDatabase) {
    private val businessProfileDao = database.businessProfileDao()
    private val clientDao = database.clientDao()
    private val invoiceDao = database.invoiceDao()
    private val paymentDao = database.paymentDao()
    private val expenseDao = database.expenseDao()

    // Business Profiles
    val allBusinessProfiles: Flow<List<BusinessProfile>> = businessProfileDao.getAllBusinessProfiles().map { list -> list.map { it.toDomain() } }
    val businessProfile: Flow<BusinessProfile?> = businessProfileDao.getBusinessProfile().map { it?.toDomain() }

    fun getBusinessProfileById(id: String): Flow<BusinessProfile?> = businessProfileDao.getBusinessProfileById(id).map { it?.toDomain() }

    suspend fun getBusinessProfileOnce(id: String? = null): BusinessProfile {
        return if (id != null) {
            businessProfileDao.getBusinessProfileByIdOnce(id)?.toDomain()
                ?: businessProfileDao.getBusinessProfileOnce()?.toDomain()
                ?: BusinessProfile()
        } else {
            businessProfileDao.getBusinessProfileOnce()?.toDomain() ?: BusinessProfile()
        }
    }

    suspend fun saveBusinessProfile(profile: BusinessProfile) {
        businessProfileDao.insertOrUpdate(BusinessProfileEntity.fromDomain(profile))
    }

    suspend fun deleteBusinessProfile(id: String) {
        businessProfileDao.deleteBusinessProfile(id)
    }

    // Clients
    val allClients: Flow<List<Client>> = clientDao.getAllClients().map { list -> list.map { it.toDomain() } }

    fun searchClients(query: String): Flow<List<Client>> = clientDao.searchClients(query).map { list -> list.map { it.toDomain() } }

    suspend fun getClientById(id: String): Client? = clientDao.getClientById(id)?.toDomain()

    suspend fun saveClient(client: Client) {
        clientDao.insertClient(ClientEntity.fromDomain(client))
    }

    suspend fun softDeleteClient(id: String) {
        clientDao.softDeleteClient(id)
    }

    // Invoices
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices().map { list -> list.map { it.toDomain() } }

    fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<Invoice>> = invoiceDao.getInvoicesByStatus(status).map { list -> list.map { it.toDomain() } }

    fun searchInvoices(query: String): Flow<List<Invoice>> = invoiceDao.searchInvoices(query).map { list -> list.map { it.toDomain() } }

    fun getInvoicesForClient(clientId: String): Flow<List<Invoice>> = invoiceDao.getInvoicesForClient(clientId).map { list -> list.map { it.toDomain() } }

    suspend fun getRecentInvoicesForClientOnce(clientId: String, limit: Int = 5): List<Invoice> {
        return invoiceDao.getRecentInvoicesForClientOnce(clientId, limit).map { it.toDomain() }
    }

    suspend fun getInvoiceById(id: String): Invoice? = invoiceDao.getInvoiceById(id)?.toDomain()

    suspend fun saveInvoice(invoice: Invoice) {
        invoiceDao.insertInvoice(InvoiceEntity.fromDomain(invoice))
    }

    suspend fun softDeleteInvoice(id: String) {
        invoiceDao.softDeleteInvoice(id)
    }

    // Payments
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments().map { list -> list.map { it.toDomain() } }

    fun getPaymentsForInvoice(invoiceId: String): Flow<List<Payment>> = paymentDao.getPaymentsForInvoice(invoiceId).map { list -> list.map { it.toDomain() } }

    suspend fun recordPayment(payment: Payment) {
        paymentDao.insertPayment(PaymentEntity.fromDomain(payment))
        // Update corresponding invoice payment status & balances
        val invoice = invoiceDao.getInvoiceById(payment.invoiceId)?.toDomain()
        if (invoice != null) {
            val newAmountPaid = invoice.amountPaid + payment.amount
            val newBalanceDue = (invoice.total - newAmountPaid).coerceAtLeast(0.0)
            val newStatus = when {
                newBalanceDue <= 0.0 -> InvoiceStatus.PAID
                newAmountPaid > 0.0 -> InvoiceStatus.PARTIAL
                else -> invoice.status
            }
            invoiceDao.updateInvoicePaymentStatus(invoice.id, newStatus, newAmountPaid, newBalanceDue)
        }
    }

    // Expenses
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses().map { list -> list.map { it.toDomain() } }

    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>> = expenseDao.getExpensesByCategory(category).map { list -> list.map { it.toDomain() } }

    suspend fun getExpenseById(id: String): Expense? = expenseDao.getExpenseById(id)?.toDomain()

    suspend fun saveExpense(expense: Expense) {
        expenseDao.insertExpense(ExpenseEntity.fromDomain(expense))
    }

    suspend fun softDeleteExpense(id: String) {
        expenseDao.softDeleteExpense(id)
    }

    // Summary Statistics calculation
    val summaryStats: Flow<InvoiceSummaryStats> = combine(allInvoices, allClients) { invoices, clients ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfMonth = cal.timeInMillis

        var totalRev = 0.0
        var monthlyRev = 0.0
        var outstanding = 0.0
        var overdue = 0.0

        for (inv in invoices) {
            if (inv.status == InvoiceStatus.PAID || inv.status == InvoiceStatus.PARTIAL) {
                totalRev += inv.amountPaid
                if (inv.issueDateMillis >= startOfMonth) {
                    monthlyRev += inv.amountPaid
                }
            }
            if (inv.status != InvoiceStatus.PAID) {
                outstanding += inv.balanceDue
                if (inv.dueDateMillis < now || inv.status == InvoiceStatus.OVERDUE) {
                    overdue += inv.balanceDue
                }
            }
        }

        InvoiceSummaryStats(
            totalRevenue = totalRev,
            monthlyRevenue = monthlyRev,
            outstanding = outstanding,
            overdue = overdue,
            invoiceCount = invoices.size,
            clientCount = clients.size
        )
    }
}
