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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.kakar.autotallypay.data.Payment
import com.kakar.autotallypay.domain.Money
import com.kakar.autotallypay.sms.SenderVerifier
import com.kakar.autotallypay.ui.MainViewModel
import com.kakar.autotallypay.ui.components.BigButton
import com.kakar.autotallypay.ui.components.PaymentRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(vm: MainViewModel) {
    val strings by vm.strings.collectAsState()
    val payments by vm.payments.collectAsState()
    val customers by vm.customers.collectAsState()
    var showPaste by remember { mutableStateOf(false) }
    var pasteSender by remember { mutableStateOf("") }
    var pasteBody by remember { mutableStateOf("") }
    var paymentToAssign by remember { mutableStateOf<Payment?>(null) }
    var assignCustomerId by remember { mutableStateOf<Long?>(null) }
    var assignExpanded by remember { mutableStateOf(false) }

    val sharedText by vm.sharedSmsText.collectAsState()
    val sharedParsed by vm.sharedParsed.collectAsState()
    var sharedSender by remember { mutableStateOf("") }
    var sharedSenderExpanded by remember { mutableStateOf(false) }
    val verifiedSenders = remember { SenderVerifier.VERIFIED_SENDERS.keys.sorted() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                strings.inbox,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            BigButton(strings.pasteSms) { showPaste = !showPaste }
        }
        if (showPaste) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = pasteSender,
                            onValueChange = { pasteSender = it },
                            label = { Text(strings.senderHint) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = pasteBody,
                            onValueChange = { pasteBody = it },
                            label = { Text(strings.smsText) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        BigButton(
                            strings.processSms,
                            enabled = pasteSender.isNotBlank() && pasteBody.isNotBlank()
                        ) {
                            vm.processSmsText(pasteSender, pasteBody)
                            pasteSender = ""
                            pasteBody = ""
                            showPaste = false
                        }
                    }
                }
            }
        }
        if (payments.isEmpty()) {
            item { Text(strings.noPaymentsYet, style = MaterialTheme.typography.bodyLarge) }
        } else {
            items(payments) { payment ->
                PaymentRow(
                    payment = payment,
                    customerName = payment.customerId?.let { id ->
                        customers.firstOrNull { it.id == id }?.name
                    },
                    strings = strings,
                    onAssignClick = if (payment.customerId == null) {
                        { paymentToAssign = payment; assignCustomerId = null }
                    } else null
                )
            }
        }
    }

    // Assign a payment to a customer (also teaches name memory).
    paymentToAssign?.let { payment ->
        AlertDialog(
            onDismissRequest = { paymentToAssign = null },
            title = { Text(strings.assignToCustomer, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${payment.source.replace('_', ' ')} — ${Money.formatRupees(payment.amountPaisa)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    ExposedDropdownMenuBox(
                        expanded = assignExpanded,
                        onExpandedChange = { assignExpanded = !assignExpanded }
                    ) {
                        OutlinedTextField(
                            value = assignCustomerId?.let { id ->
                                customers.firstOrNull { it.id == id }?.name
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.selectCustomer) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assignExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = assignExpanded,
                            onDismissRequest = { assignExpanded = false }
                        ) {
                            customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text(customer.name) },
                                    onClick = {
                                        assignCustomerId = customer.id
                                        assignExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = assignCustomerId != null,
                    onClick = {
                        assignCustomerId?.let { vm.assignPayment(payment.id, it) }
                        paymentToAssign = null
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { paymentToAssign = null }) { Text(strings.cancel) }
            }
        )
    }

    // Shared SMS (Lite): user confirms the original sender before it counts.
    if (sharedText != null) {
        AlertDialog(
            onDismissRequest = { vm.setSharedSms(null) },
            title = { Text(strings.confirmSharedSms, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.confirmSharedHint, style = MaterialTheme.typography.bodyLarge)
                    sharedParsed?.let { parsed ->
                        Text(
                            "${parsed.source.name.replace('_', ' ')} — ${Money.formatRupees(parsed.amountPaisa)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        parsed.trxId?.let {
                            Text("${strings.trxId}: $it", style = MaterialTheme.typography.bodyLarge)
                        }
                    } ?: Text(strings.notAPaymentSms, style = MaterialTheme.typography.bodyLarge)
                    if (sharedParsed != null) {
                        ExposedDropdownMenuBox(
                            expanded = sharedSenderExpanded,
                            onExpandedChange = { sharedSenderExpanded = !sharedSenderExpanded }
                        ) {
                            OutlinedTextField(
                                value = sharedSender,
                                onValueChange = { sharedSender = it },
                                label = { Text(strings.sender) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = sharedSenderExpanded)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = sharedSenderExpanded,
                                onDismissRequest = { sharedSenderExpanded = false }
                            ) {
                                verifiedSenders.forEach { sender ->
                                    DropdownMenuItem(
                                        text = { Text(sender) },
                                        onClick = {
                                            sharedSender = sender
                                            sharedSenderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (sharedParsed != null) {
                    TextButton(
                        enabled = sharedSender.isNotBlank(),
                        onClick = {
                            vm.confirmSharedSms(sharedSender)
                            sharedSender = ""
                        }
                    ) { Text(strings.confirmAndSave) }
                } else {
                    TextButton(onClick = { vm.setSharedSms(null) }) { Text(strings.ok) }
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.setSharedSms(null); sharedSender = "" }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
