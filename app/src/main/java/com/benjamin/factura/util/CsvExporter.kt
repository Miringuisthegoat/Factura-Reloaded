package com.benjamin.factura.util

import com.benjamin.factura.data.model.Expense
import com.benjamin.factura.data.model.Invoice

object CsvExporter {
    fun exportInvoicesToCsv(invoices: List<Invoice>): String {
        val sb = StringBuilder()
        sb.append("Invoice Number,Client Name,Issue Date,Due Date,Status,Subtotal,Tax,Total,Amount Paid,Balance Due,Currency\n")

        for (inv in invoices) {
            sb.append("\"${inv.invoiceNumber}\",")
            sb.append("\"${inv.clientName}\",")
            sb.append("\"${Formatters.formatDate(inv.issueDateMillis)}\",")
            sb.append("\"${Formatters.formatDate(inv.dueDateMillis)}\",")
            sb.append("\"${inv.status.name}\",")
            sb.append("${inv.subtotal},")
            sb.append("${inv.taxTotal},")
            sb.append("${inv.total},")
            sb.append("${inv.amountPaid},")
            sb.append("${inv.balanceDue},")
            sb.append("\"${inv.currencyCode}\"\n")
        }
        return sb.toString()
    }

    fun exportExpensesToCsv(expenses: List<Expense>): String {
        val sb = StringBuilder()
        sb.append("Vendor,Category,Amount,Date,Notes\n")

        for (exp in expenses) {
            sb.append("\"${exp.vendor}\",")
            sb.append("\"${exp.category.label}\",")
            sb.append("${exp.amount},")
            sb.append("\"${Formatters.formatDate(exp.expenseDateMillis)}\",")
            sb.append("\"${exp.notes.replace("\"", "'")}\"\n")
        }
        return sb.toString()
    }
}
