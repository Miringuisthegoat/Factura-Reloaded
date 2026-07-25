package com.benjamin.factura.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.benjamin.factura.data.model.BusinessProfile
import com.benjamin.factura.data.model.Client
import com.benjamin.factura.data.model.Invoice
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {
    fun generateInvoicePdf(
        context: Context,
        invoice: Invoice,
        client: Client?,
        profile: BusinessProfile
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val primaryColor = Color.parseColor("#1D6A72")

        // Header Background
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Header Title
        textPaint.color = Color.WHITE
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(profile.businessName.ifBlank { "FACTURA INVOICE" }, 30f, 45f, textPaint)

        textPaint.textSize = 12f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("INVOICE #${invoice.invoiceNumber}", 400f, 45f, textPaint)

        // Reset text color for body
        textPaint.color = Color.DKGRAY

        // Business Details (Left)
        var y = 120f
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FROM:", 30f, y, textPaint)
        y += 18f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(profile.businessName, 30f, y, textPaint)
        y += 15f
        canvas.drawText(profile.address, 30f, y, textPaint)
        y += 15f
        canvas.drawText("Phone: ${profile.phone}", 30f, y, textPaint)
        y += 15f
        canvas.drawText("Tax PIN: ${profile.taxNumber}", 30f, y, textPaint)

        // Client Details (Right)
        var clientY = 120f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILL TO:", 330f, clientY, textPaint)
        clientY += 18f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(client?.name ?: invoice.clientName, 330f, clientY, textPaint)
        clientY += 15f
        if (!client?.email.isNull_or_empty()) canvas.drawText(client!!.email, 330f, clientY, textPaint)
        clientY += 15f
        if (!client?.phone.isNull_or_empty()) canvas.drawText(client!!.phone, 330f, clientY, textPaint)
        clientY += 15f
        if (!client?.address.isNull_or_empty()) canvas.drawText(client!!.address, 330f, clientY, textPaint)

        // Dates & Status
        y = (y.coerceAtLeast(clientY)) + 30f
        paint.color = Color.LTGRAY
        canvas.drawLine(30f, y, 565f, y, paint)
        y += 20f

        canvas.drawText("Issue Date: ${Formatters.formatDate(invoice.issueDateMillis)}", 30f, y, textPaint)
        canvas.drawText("Due Date: ${Formatters.formatDate(invoice.dueDateMillis)}", 200f, y, textPaint)
        canvas.drawText("Status: ${invoice.status.name}", 400f, y, textPaint)

        y += 30f

        // Table Header
        paint.color = Color.parseColor("#E2F3F5")
        canvas.drawRect(30f, y, 565f, y + 25f, paint)

        textPaint.color = Color.parseColor("#002023")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Description", 40f, y + 17f, textPaint)
        canvas.drawText("Qty", 320f, y + 17f, textPaint)
        canvas.drawText("Unit Price", 380f, y + 17f, textPaint)
        canvas.drawText("Total", 480f, y + 17f, textPaint)

        y += 30f
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT

        // Items
        for (item in invoice.items) {
            canvas.drawText(item.description, 40f, y + 15f, textPaint)
            canvas.drawText(item.quantity.toString(), 320f, y + 15f, textPaint)
            canvas.drawText(Formatters.formatCurrency(item.unitPrice, invoice.currencyCode), 380f, y + 15f, textPaint)
            canvas.drawText(Formatters.formatCurrency(item.total, invoice.currencyCode), 480f, y + 15f, textPaint)

            y += 25f
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(30f, y, 565f, y, paint)
            y += 5f
        }

        y += 20f

        // Total Summary
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Subtotal:", 350f, y, textPaint)
        canvas.drawText(Formatters.formatCurrency(invoice.subtotal, invoice.currencyCode), 480f, y, textPaint)
        y += 20f

        canvas.drawText("VAT Tax (${profile.defaultTaxRatePercent}%):", 350f, y, textPaint)
        canvas.drawText(Formatters.formatCurrency(invoice.taxTotal, invoice.currencyCode), 480f, y, textPaint)
        y += 20f

        textPaint.textSize = 14f
        textPaint.color = primaryColor
        canvas.drawText("Total Amount:", 350f, y, textPaint)
        canvas.drawText(Formatters.formatCurrency(invoice.total, invoice.currencyCode), 480f, y, textPaint)
        y += 25f

        textPaint.textSize = 12f
        textPaint.color = Color.DKGRAY
        canvas.drawText("Amount Paid:", 350f, y, textPaint)
        canvas.drawText(Formatters.formatCurrency(invoice.amountPaid, invoice.currencyCode), 480f, y, textPaint)
        y += 20f

        canvas.drawText("Balance Due:", 350f, y, textPaint)
        canvas.drawText(Formatters.formatCurrency(invoice.balanceDue, invoice.currencyCode), 480f, y, textPaint)

        // Payment Info & Footer
        y += 40f
        paint.color = Color.LTGRAY
        canvas.drawLine(30f, y, 565f, y, paint)
        y += 20f

        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PAYMENT DETAILS:", 30f, y, textPaint)
        y += 18f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Bank / Paybill: ${profile.bankName}", 30f, y, textPaint)
        y += 15f
        canvas.drawText("Account: ${profile.bankAccountNumber}", 30f, y, textPaint)
        y += 15f
        canvas.drawText("Account Name: ${profile.bankAccountName}", 30f, y, textPaint)

        y += 30f
        canvas.drawText("Thank you for your business!", 210f, y, textPaint)

        document.finishPage(page)

        val file = File(context.cacheDir, "Invoice_${invoice.invoiceNumber}.pdf")
        val outputStream = FileOutputStream(file)
        document.writeTo(outputStream)
        document.close()
        outputStream.close()

        return file
    }

    private fun String?.isNull_or_empty() = this == null || this.isBlank()
}
