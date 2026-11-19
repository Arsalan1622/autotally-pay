package com.kakar.autotallypay.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kakar.autotallypay.data.Bill
import com.kakar.autotallypay.domain.Money
import com.kakar.autotallypay.ui.MainViewModel
import com.kakar.autotallypay.ui.components.BigButton
import com.kakar.autotallypay.ui.components.BillRow
import com.kakar.autotallypay.ui.components.PaymentRow
import com.kakar.autotallypay.ui.theme.StatusColors

@Composable
fun LedgerScreen(
    vm: MainViewModel,
    customerId: Long,
    onAddBill: (Long) -> Unit,
    onCustomerDeleted: () -> Unit
) {
    val strings by vm.strings.collectAsState()
    val customer = vm.customerById(customerId)
    val bills by vm.bills.collectAsState()
    val payments by vm.payments.collectAsState()
    val justPaid by vm.justPaidBillIds.collectAsState()
    var billToDelete by remember { mutableStateOf<Bill?>(null) }
    var confirmDeleteCustomer by remember { mutableStateOf(false) }

    if (customer == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(strings.noCustomersYet, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val myBills = bills.filter { it.customerId == customerId }
    val myPayments = payments.filter { it.customerId == customerId }
    val outstanding = vm.customerOutstanding(customerId)
    val credit = vm.unallocatedCredit(customerId)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                customer.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (customer.phone.isNotBlank()) {
                Text(customer.phone, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (outstanding > 0) StatusColors.unpaidRedBg else StatusColors.paidGreenBg
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.totalOutstanding, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        Money.formatRupees(outstanding),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (outstanding > 0) StatusColors.unpaidRed else StatusColors.paidGreen
                    )
                    if (credit > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${strings.unallocatedCredit}: ${Money.formatRupees(credit)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = StatusColors.paidGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item {
            BigButton(strings.addBill) { onAddBill(customerId) }
        }
        item {
            Text(strings.bills, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (myBills.isEmpty()) {
            item { Text(strings.noBillsYet, style = MaterialTheme.typography.bodyLarge) }
        } else {
            items(myBills) { bill ->
                BillRow(
                    bill = bill,
                    paidPaisa = vm.paidForBill(bill.id),
                    strings = strings,
                    justPaid = bill.id in justPaid,
                    onDelete = { billToDelete = bill }
                )
            }
        }
        if (myPayments.isNotEmpty()) {
            item {
                Text(strings.payments, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(myPayments) { payment ->
                PaymentRow(payment = payment, customerName = customer.name, strings = strings)
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { confirmDeleteCustomer = true }) {
                Text(strings.delete, color = StatusColors.unpaidRed)
            }
        }
    }

    billToDelete?.let { bill ->
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            title = { Text(strings.deleteBillConfirm) },
            text = { Text("${bill.description} — ${Money.formatRupees(bill.amountPaisa)}") },
            confirmButton = {
                TextButton(onClick = { vm.deleteBill(bill); billToDelete = null }) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) { Text(strings.cancel) }
            }
        )
    }

    if (confirmDeleteCustomer) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCustomer = false },
            title = { Text(strings.deleteCustomerConfirm) },
            text = { Text(customer.name) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCustomer(customer)
                    confirmDeleteCustomer = false
                    onCustomerDeleted()
                }) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCustomer = false }) { Text(strings.cancel) }
            }
        )
    }
}
