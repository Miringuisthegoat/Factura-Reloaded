package com.benjamin.factura.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector

sealed class FacturaDestination(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Splash : FacturaDestination("splash")
    object Onboarding : FacturaDestination("onboarding")

    // Bottom Navigation Destinations
    object Dashboard : FacturaDestination("dashboard", "Dashboard", Icons.Default.Dashboard)
    object InvoiceList : FacturaDestination("invoice_list", "Invoices", Icons.Default.Receipt)
    object ClientList : FacturaDestination("client_list", "Clients", Icons.Default.People)
    object ExpenseList : FacturaDestination("expense_list", "Expenses", Icons.Default.ReceiptLong)
    object Reports : FacturaDestination("reports", "Reports", Icons.Default.Analytics)

    // Secondary / Detail Screens
    object InvoiceDetail : FacturaDestination("invoice_detail/{invoiceId}") {
        fun createRoute(invoiceId: String) = "invoice_detail/$invoiceId"
    }
    object CreateInvoice : FacturaDestination("create_invoice?invoiceId={invoiceId}") {
        fun createRoute(invoiceId: String? = null) = if (invoiceId != null) "create_invoice?invoiceId=$invoiceId" else "create_invoice"
    }
    object ClientDetail : FacturaDestination("client_detail/{clientId}") {
        fun createRoute(clientId: String) = "client_detail/$clientId"
    }
    object CreateEditClient : FacturaDestination("create_client?clientId={clientId}") {
        fun createRoute(clientId: String? = null) = if (clientId != null) "create_client?clientId=$clientId" else "create_client"
    }
    object CreateExpense : FacturaDestination("create_expense?expenseId={expenseId}") {
        fun createRoute(expenseId: String? = null) = if (expenseId != null) "create_expense?expenseId=$expenseId" else "create_expense"
    }
    object PaymentRecord : FacturaDestination("payment_record/{invoiceId}") {
        fun createRoute(invoiceId: String) = "payment_record/$invoiceId"
    }
    object Settings : FacturaDestination("settings")

    companion object {
        val bottomNavDestinations by lazy {
            listOf(Dashboard, InvoiceList, ClientList, ExpenseList, Reports)
        }
    }
}
