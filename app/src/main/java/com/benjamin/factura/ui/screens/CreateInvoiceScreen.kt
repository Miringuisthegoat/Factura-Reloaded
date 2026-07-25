package com.benjamin.factura.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benjamin.factura.data.local.AppDatabase
import com.benjamin.factura.data.model.Client
import com.benjamin.factura.data.model.Invoice
import com.benjamin.factura.data.model.InvoiceItem
import com.benjamin.factura.data.model.InvoiceStatus
import com.benjamin.factura.data.repository.FacturaRepository
import com.benjamin.factura.data.repository.GeminiAiRepository
import com.benjamin.factura.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
    invoiceIdToEdit: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCreateClient: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FacturaRepository(AppDatabase.getInstance(context)) }
    val geminiRepo = remember { GeminiAiRepository() }

    val clients by repository.allClients.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    var invoiceNumber by remember { mutableStateOf("1001") }
    var currencyCode by remember { mutableStateOf("KES") }
    var defaultTaxRate by remember { mutableDoubleStateOf(16.0) }

    val lineItems = remember { mutableStateListOf<InvoiceItem>() }
    var isAutoFilling by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val profile = repository.getBusinessProfileOnce()
        currencyCode = profile.defaultCurrency
        defaultTaxRate = profile.defaultTaxRatePercent.toDouble()

        if (invoiceIdToEdit != null) {
            val existing = repository.getInvoiceById(invoiceIdToEdit)
            if (existing != null) {
                invoiceNumber = existing.invoiceNumber
                selectedClient = clients.find { it.id == existing.clientId }
                lineItems.clear()
                lineItems.addAll(existing.items)
            }
        } else {
            // Generate next invoice number
            val count = repository.allInvoices.first().size
            invoiceNumber = "${1001 + count}"
            lineItems.add(
                InvoiceItem(
                    description = "Professional Consulting Services",
                    quantity = 1.0,
                    unitPrice = 15000.0,
                    taxRatePercent = defaultTaxRate
                )
            )
        }
    }

    fun triggerGeminiAutoFill() {
        val client = selectedClient ?: return
        isAutoFilling = true
        scope.launch {
            val pastInvoices = repository.getRecentInvoicesForClientOnce(client.id)
            val suggestions = geminiRepo.suggestInvoiceItemsForClient(client.name, pastInvoices)
            if (suggestions.isNotEmpty()) {
                lineItems.clear()
                lineItems.addAll(suggestions)
            }
            isAutoFilling = false
        }
    }

    fun saveInvoice(status: InvoiceStatus) {
        val client = selectedClient ?: return
        val subtotal = lineItems.sumOf { it.subtotal }
        val taxTotal = lineItems.sumOf { it.taxAmount }
        val total = subtotal + taxTotal

        val newInvoice = Invoice(
            id = invoiceIdToEdit ?: java.util.UUID.randomUUID().toString(),
            businessId = "default_business",
            invoiceNumber = invoiceNumber,
            clientId = client.id,
            clientName = client.name,
            status = status,
            items = lineItems.toList(),
            subtotal = subtotal,
            taxTotal = taxTotal,
            total = total,
            amountPaid = 0.0,
            balanceDue = total,
            currencyCode = currencyCode
        )

        scope.launch {
            repository.saveInvoice(newInvoice)
            onNavigateBack()
        }
    }

    val subtotal = lineItems.sumOf { it.subtotal }
    val taxTotal = lineItems.sumOf { it.taxAmount }
    val total = subtotal + taxTotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (invoiceIdToEdit != null) "Edit Invoice" else "New Invoice", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Invoice Number & Currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Invoice Number") },
                    prefix = { Text("INV-") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = currencyCode,
                    onValueChange = { currencyCode = it },
                    label = { Text("Currency") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Client Picker
            Text(text = "CLIENT SELECTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = clientDropdownExpanded,
                onExpandedChange = { clientDropdownExpanded = !clientDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedClient?.name ?: "Select Client",
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = clientDropdownExpanded,
                    onDismissRequest = { clientDropdownExpanded = false }
                ) {
                    clients.forEach { client ->
                        DropdownMenuItem(
                            text = { Text(client.name) },
                            onClick = {
                                selectedClient = client
                                clientDropdownExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("+ Add New Client", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                        onClick = {
                            clientDropdownExpanded = false
                            onNavigateToCreateClient()
                        }
                    )
                }
            }

            // Gemini Auto-fill Button
            if (selectedClient != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { triggerGeminiAutoFill() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAutoFilling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini AI Auto-Fill Items")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Line Items Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "LINE ITEMS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TextButton(
                    onClick = {
                        lineItems.add(
                            InvoiceItem(
                                description = "",
                                quantity = 1.0,
                                unitPrice = 0.0,
                                taxRatePercent = defaultTaxRate
                            )
                        )
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            lineItems.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Item #${index + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (lineItems.size > 1) {
                                IconButton(onClick = { lineItems.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = item.description,
                            onValueChange = { desc -> lineItems[index] = item.copy(description = desc) },
                            label = { Text("Item Description") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = item.quantity.toString(),
                                onValueChange = { qtyStr ->
                                    val qty = qtyStr.toDoubleOrNull() ?: 1.0
                                    lineItems[index] = item.copy(quantity = qty)
                                },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = item.unitPrice.toString(),
                                onValueChange = { priceStr ->
                                    val price = priceStr.toDoubleOrNull() ?: 0.0
                                    lineItems[index] = item.copy(unitPrice = price)
                                },
                                label = { Text("Unit Price") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Totals Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Formatters.formatCurrency(subtotal, currencyCode))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("VAT Tax (${defaultTaxRate.toInt()}%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Formatters.formatCurrency(taxTotal, currencyCode))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(Formatters.formatCurrency(total, currencyCode), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { saveInvoice(InvoiceStatus.DRAFT) },
                    enabled = selectedClient != null && lineItems.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Draft")
                }

                Button(
                    onClick = { saveInvoice(InvoiceStatus.SENT) },
                    enabled = selectedClient != null && lineItems.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save & Send")
                }
            }
        }
    }
}
