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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kakar.autotallypay.domain.Money
import com.kakar.autotallypay.ui.MainViewModel
import com.kakar.autotallypay.ui.components.BigButton
import com.kakar.autotallypay.ui.components.formatDate
import com.kakar.autotallypay.ui.theme.StatusColors

/**
 * "Did the money come?" — type an amount, get a yes/no in one second,
 * with the matching Trx ID when the money is there.
 */
@Composable
fun CheckerScreen(vm: MainViewModel) {
    val strings by vm.strings.collectAsState()
    val checkResult by vm.checkResult.collectAsState()
    val customers by vm.customers.collectAsState()
    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                strings.didMoneyCome,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(strings.didMoneyComeHint, style = MaterialTheme.typography.bodyLarge)
        }
        item {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it; amountError = false; vm.clearCheck() },
                label = { Text(strings.enterAmount) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            BigButton(strings.checkNow) {
                val paisa = Money.rupeesToPaisa(amountText)
                if (paisa == null || paisa <= 0) {
                    amountError = true
                } else {
                    vm.runCheck(paisa)
                }
            }
        }
        checkResult?.let { result ->
            val found = result.matches.isNotEmpty()
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (found) StatusColors.paidGreenBg else StatusColors.unpaidRedBg
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            if (found) strings.moneyYes else strings.moneyNo,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (found) StatusColors.paidGreen else StatusColors.unpaidRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            Money.formatRupees(result.amountPaisa),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            items(result.matches) { payment ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            payment.source.replace('_', ' '),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            payment.senderName ?: payment.senderId,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        payment.trxId?.let {
                            Text(
                                "${strings.trxId}: $it",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        payment.customerId?.let { id ->
                            customers.firstOrNull { it.id == id }?.let {
                                Text(
                                    "${strings.customer}: ${it.name}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Text(formatDate(payment.receivedAt), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
