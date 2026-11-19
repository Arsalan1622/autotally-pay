package com.kakar.autotallypay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kakar.autotallypay.domain.Money
import com.kakar.autotallypay.ui.MainViewModel
import com.kakar.autotallypay.ui.components.BigButton
import com.kakar.autotallypay.ui.theme.StatusColors

@Composable
fun KhataScreen(vm: MainViewModel, onOpenCustomer: (Long) -> Unit) {
    val strings by vm.strings.collectAsState()
    val customers by vm.customers.collectAsState()
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }

    val filtered = customers.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) ||
            it.phone.contains(query)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            strings.khata,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(strings.customerName) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        BigButton(strings.addCustomer) { showAdd = true }
        Spacer(modifier = Modifier.height(16.dp))
        if (filtered.isEmpty()) {
            Text(strings.noCustomersYet, style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { customer ->
                    val outstanding = vm.customerOutstanding(customer.id)
                    val billCount = vm.billsForCustomer(customer.id).size
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (outstanding > 0) {
                                StatusColors.unpaidRedBg
                            } else {
                                StatusColors.paidGreenBg
                            }
                        ),
                        onClick = { onOpenCustomer(customer.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    customer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (customer.phone.isNotBlank()) {
                                    Text(
                                        customer.phone,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    "${strings.bills}: $billCount",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                if (outstanding > 0) Money.formatRupees(outstanding) else strings.allCaughtUp,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (outstanding > 0) {
                                    StatusColors.unpaidRed
                                } else {
                                    StatusColors.paidGreen
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(strings.addCustomer, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(strings.customerName) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text(strings.phone) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        vm.addCustomer(newName, newPhone)
                        newName = ""
                        newPhone = ""
                        showAdd = false
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text(strings.cancel) }
            }
        )
    }
}
