package com.benjamin.factura.ui.screens

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjamin.factura.data.local.AppDatabase
import com.benjamin.factura.data.model.BusinessProfile
import com.benjamin.factura.data.model.Client
import com.benjamin.factura.data.model.Invoice
import com.benjamin.factura.data.model.InvoiceStatus
import com.benjamin.factura.data.repository.FacturaRepository
import com.benjamin.factura.data.repository.GeminiAiRepository
import com.benjamin.factura.ui.theme.StatusDraft
import com.benjamin.factura.ui.theme.StatusOverdue
import com.benjamin.factura.ui.theme.StatusPaid
import com.benjamin.factura.ui.theme.StatusPartial
import com.benjamin.factura.ui.theme.StatusSent
import com.benjamin.factura.util.Formatters
import com.benjamin.factura.util.PdfReportGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRecordPayment: (String) -> Unit,
    onNavigateToEditInvoice: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FacturaRepository(AppDatabase.getInstance(context)) }
    val geminiRepo = remember { GeminiAiRepository() }

    var invoice by remember { mutableStateOf<Invoice?>(null) }
    var client by remember { mutableStateOf<Client?>(null) }
    var businessProfile by remember { mutableStateOf<BusinessProfile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var reminderText by remember { mutableStateOf<String?>(null) }
    var isGeneratingReminder by remember { mutableStateOf(false) }

    LaunchedEffect(invoiceId) {
        val inv = repository.getInvoiceById(invoiceId)
        invoice = inv
        if (inv != null) {
            client = repository.getClientById(inv.clientId)
            businessProfile = repository.getBusinessProfileOnce()
        }
    }

    fun sharePdf() {
        val inv = invoice ?: return
        val prof = businessProfile ?: BusinessProfile()
        try {
            val file = PdfReportGenerator.generateInvoicePdf(context, inv, client, prof)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF"))
        } catch (e: Exception) {
            // Error handling
        }
    }

    fun generateSmartReminder() {
        val inv = invoice ?: return
        isGeneratingReminder = true
        scope.launch {
            val daysOverdue = ((System.currentTimeMillis() - inv.dueDateMillis) / (24 * 60 * 60 * 1000)).coerceAtLeast(1)
            reminderText = geminiRepo.generatePaymentReminder(
                clientName = inv.clientName,
                invoiceNumber = inv.invoiceNumber,
                amountDue = inv.balanceDue,
                currencyCode = inv.currencyCode,
                daysOverdue = daysOverdue
            )
            isGeneratingReminder = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Invoice #${invoice?.invoiceNumber ?: ""}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { sharePdf() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF")
                    }
                    IconButton(onClick = { invoice?.let { onNavigateToEditInvoice(it.id) } }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (invoice == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val inv = invoice!!
            val statusColor = when (inv.status) {
                InvoiceStatus.PAID -> StatusPaid
                InvoiceStatus.OVERDUE -> StatusOverdue
                InvoiceStatus.PARTIAL -> StatusPartial
                InvoiceStatus.SENT -> StatusSent
                else -> StatusDraft
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Invoice Amount",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatCurrency(inv.total, inv.currencyCode),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = inv.status.name,
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (inv.balanceDue > 0 && inv.status != InvoiceStatus.PAID) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Balance Due:",
                                    fontSize = 13.sp,
                                    color = StatusOverdue,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = Formatters.formatCurrency(inv.balanceDue, inv.currencyCode),
                                    fontSize = 14.sp,
                                    color = StatusOverdue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (inv.status != InvoiceStatus.PAID) {
                        Button(
                            onClick = { onNavigateToRecordPayment(inv.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Payment")
                        }
                    }

                    OutlinedButton(
                        onClick = { sharePdf() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Invoice")
                    }
                }

                // Gemini Smart Reminder Button for Overdue/Outstanding
                if (inv.status == InvoiceStatus.OVERDUE || inv.balanceDue > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { generateSmartReminder() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGeneratingReminder) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Gemini Smart Reminder")
                    }
                }

                if (reminderText != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Payment Reminder Draft", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = reminderText!!, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Invoice Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "CLIENT & DATES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))

                        DetailRow("Client Name", inv.clientName)
                        DetailRow("Issue Date", Formatters.formatDate(inv.issueDateMillis))
                        DetailRow("Due Date", Formatters.formatDate(inv.dueDateMillis))

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "LINE ITEMS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))

                        inv.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.description, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text(
                                        text = "${item.quantity} x ${Formatters.formatCurrency(item.unitPrice, inv.currencyCode)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = Formatters.formatCurrency(item.total, inv.currencyCode),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        DetailRow("Subtotal", Formatters.formatCurrency(inv.subtotal, inv.currencyCode))
                        DetailRow("VAT Tax", Formatters.formatCurrency(inv.taxTotal, inv.currencyCode))
                        DetailRow("Total", Formatters.formatCurrency(inv.total, inv.currencyCode), isBold = true)
                        DetailRow("Amount Paid", Formatters.formatCurrency(inv.amountPaid, inv.currencyCode))
                        DetailRow("Balance Due", Formatters.formatCurrency(inv.balanceDue, inv.currencyCode), isBold = true, textColor = StatusOverdue)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Invoice") },
            text = { Text("Are you sure you want to delete Invoice #${invoice?.invoiceNumber}? This action can be undone from local sync.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            repository.softDeleteInvoice(invoiceId)
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, isBold: Boolean = false, textColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
