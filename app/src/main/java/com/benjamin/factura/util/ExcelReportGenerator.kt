package com.benjamin.factura.util

import android.content.Context
import com.benjamin.factura.data.model.Expense
import com.benjamin.factura.data.model.Invoice
import java.io.File
import java.io.FileOutputStream

object ExcelReportGenerator {
    fun generateFinancialReportXlsx(
        context: Context,
        invoices: List<Invoice>,
        expenses: List<Expense>
    ): File {
        // Generates structured CSV/XLS report file suitable for opening in Excel / Sheets
        val file = File(context.cacheDir, "Factura_Financial_Report.csv")
        val sb = StringBuilder()

        sb.append("FACTURA FINANCIAL REPORT\n")
        sb.append("Generated Date: ${Formatters.formatDate(System.currentTimeMillis())}\n\n")

        sb.append("=== INVOICES SUMMARY ===\n")
        sb.append(CsvExporter.exportInvoicesToCsv(invoices))

        sb.append("\n=== EXPENSES SUMMARY ===\n")
        sb.append(CsvExporter.exportExpensesToCsv(expenses))

        val fos = FileOutputStream(file)
        fos.write(sb.toString().toByteArray())
        fos.close()

        return file
    }
}
