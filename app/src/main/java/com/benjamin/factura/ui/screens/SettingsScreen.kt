package com.benjamin.factura.ui.screens

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benjamin.factura.data.local.AppDatabase
import com.benjamin.factura.data.local.PreferencesManager
import com.benjamin.factura.data.model.BusinessProfile
import com.benjamin.factura.data.repository.FacturaRepository
import com.benjamin.factura.util.CsvExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FacturaRepository(AppDatabase.getInstance(context)) }
    val preferencesManager = remember { PreferencesManager(context) }

    val businessProfiles by repository.allBusinessProfiles.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeBusinessId by preferencesManager.activeBusinessId.collectAsStateWithLifecycle(initialValue = "default_business")
    val clients by repository.allClients.collectAsStateWithLifecycle(initialValue = emptyList())

    var activeProfileId by remember { mutableStateOf("default_business") }
    var businessName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("Nairobi, Kenya") }
    var taxNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var defaultCurrency by remember { mutableStateOf("KES") }
    var defaultTaxRate by remember { mutableStateOf("16") }

    var showAddBusinessDialog by remember { mutableStateOf(false) }
    var newBusinessNameInput by remember { mutableStateOf("") }

    LaunchedEffect(activeBusinessId, businessProfiles) {
        val current = businessProfiles.find { it.id == activeBusinessId }
            ?: repository.getBusinessProfileOnce(activeBusinessId)
        
        activeProfileId = current.id
        businessName = current.businessName
        phone = current.phone
        email = current.email
        address = current.address
        taxNumber = current.taxNumber
        bankName = current.bankName
        bankAccount = current.bankAccountNumber
        defaultCurrency = current.defaultCurrency
        defaultTaxRate = current.defaultTaxRatePercent.toString()
    }

    fun saveSettings() {
        val profile = BusinessProfile(
            id = activeProfileId,
            businessName = businessName,
            phone = phone,
            email = email,
            address = address,
            taxNumber = taxNumber,
            bankName = bankName,
            bankAccountNumber = bankAccount,
            defaultCurrency = defaultCurrency,
            defaultTaxRatePercent = defaultTaxRate.toFloatOrNull() ?: 16f
        )

        scope.launch {
            repository.saveBusinessProfile(profile)
            preferencesManager.setActiveBusinessId(activeProfileId)
            onNavigateBack()
        }
    }

    fun createNewBusinessProfile(name: String) {
        if (name.isBlank()) return
        val newId = UUID.randomUUID().toString()
        val newProfile = BusinessProfile(
            id = newId,
            businessName = name,
            email = email,
            phone = phone,
            address = address,
            defaultCurrency = defaultCurrency
        )
        scope.launch {
            repository.saveBusinessProfile(newProfile)
            preferencesManager.setActiveBusinessId(newId)
            showAddBusinessDialog = false
            newBusinessNameInput = ""
        }
    }

    fun exportCsv() {
        scope.launch {
            val invoices = repository.allInvoices.first()
            val csvData = CsvExporter.exportInvoicesToCsv(invoices)

            val file = File(context.cacheDir, "Factura_Invoices_Export.csv")
            FileOutputStream(file).use { it.write(csvData.toByteArray()) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Invoices CSV"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Business Profile", fontWeight = FontWeight.Bold) },
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
            // MULTI-BUSINESS ENTITY SWITCHER SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "BUSINESS ENTITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { showAddBusinessDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Business", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val displayProfiles = if (businessProfiles.isEmpty()) {
                listOf(BusinessProfile(id = "default_business", businessName = businessName.ifBlank { "My Business" }))
            } else {
                businessProfiles
            }

            displayProfiles.forEach { profile ->
                val isSelected = profile.id == activeProfileId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            activeProfileId = profile.id
                            businessName = profile.businessName
                            phone = profile.phone
                            email = profile.email
                            address = profile.address
                            taxNumber = profile.taxNumber
                            bankName = profile.bankName
                            bankAccount = profile.bankAccountNumber
                            defaultCurrency = profile.defaultCurrency
                            defaultTaxRate = profile.defaultTaxRatePercent.toString()
                            scope.launch {
                                preferencesManager.setActiveBusinessId(profile.id)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(profile.businessName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (isSelected) "Active Profile" else "Tap to switch to this business",
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // MULTI-CLIENT OVERVIEW CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Client Directory Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${clients.size} clients registered • Multi-client billing enabled", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "ACTIVE BUSINESS PROFILE DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business / Trading Name") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Business Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Physical Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = taxNumber,
                onValueChange = { taxNumber = it },
                label = { Text("KRA Tax PIN / Registration Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "PAYMENT DETAILS & DEFAULTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank / Payment Paybill Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bankAccount,
                onValueChange = { bankAccount = it },
                label = { Text("Account / Paybill Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = defaultCurrency,
                    onValueChange = { defaultCurrency = it },
                    label = { Text("Currency") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = defaultTaxRate,
                    onValueChange = { defaultTaxRate = it },
                    label = { Text("Default VAT %") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Export Data Button
            OutlinedButton(
                onClick = { exportCsv() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Invoices to CSV")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { saveSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Profile Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddBusinessDialog) {
        AlertDialog(
            onDismissRequest = { showAddBusinessDialog = false },
            title = { Text("Add Business Entity", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Add an additional business or company profile to issue invoices under separate business branding.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newBusinessNameInput,
                        onValueChange = { newBusinessNameInput = it },
                        label = { Text("New Business Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { createNewBusinessProfile(newBusinessNameInput) },
                    enabled = newBusinessNameInput.isNotBlank()
                ) {
                    Text("Create Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBusinessDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

