package com.kakar.autotallypay.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kakar.autotallypay.BuildConfig
import com.kakar.autotallypay.domain.Money
import com.kakar.autotallypay.ui.MainViewModel
import com.kakar.autotallypay.ui.components.BigButton
import com.kakar.autotallypay.ui.components.PaymentRow
import com.kakar.autotallypay.ui.theme.StatusColors

private fun hasSmsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
        PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenInbox: () -> Unit,
    onOpenCheck: () -> Unit,
    onAddBill: () -> Unit,
    onAddCustomer: () -> Unit
) {
    val strings by vm.strings.collectAsState()
    val dashboard by vm.dashboard.collectAsState()
    val payments by vm.payments.collectAsState()
    val customers by vm.customers.collectAsState()
    val context = LocalContext.current
    val isFullFlavor = BuildConfig.FLAVOR == "full"
    var smsGranted by remember { mutableStateOf(hasSmsPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> smsGranted = result.values.all { it } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                strings.appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(strings.offlineNote, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }

        // SMS permission card (full flavor only).
        if (isFullFlavor && !smsGranted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusColors.warnAmberBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            strings.permissionNeeded,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(strings.permissionHint, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        BigButton(strings.grantPermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = StatusColors.paidGreenBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(strings.todayReceived, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            Money.formatRupees(dashboard.todayReceived),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StatusColors.paidGreen
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (dashboard.totalOutstanding > 0) {
                            StatusColors.unpaidRedBg
                        } else {
                            StatusColors.paidGreenBg
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(strings.totalOutstanding, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            Money.formatRupees(dashboard.totalOutstanding),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (dashboard.totalOutstanding > 0) {
                                StatusColors.unpaidRed
                            } else {
                                StatusColors.paidGreen
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${strings.pendingBills}: ${dashboard.pendingBills}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (dashboard.totalOutstanding == 0L && dashboard.pendingBills == 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            strings.allCaughtUp,
                            style = MaterialTheme.typography.bodyLarge,
                            color = StatusColors.paidGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (dashboard.fakeCount > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusColors.unpaidRedBg),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenInbox
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${strings.fakeSmsAlert}: ${dashboard.fakeCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusColors.unpaidRed
                        )
                        Text(strings.fakeSmsFlagged, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        if (dashboard.unmatchedCount > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusColors.warnAmberBg),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenInbox
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${strings.unmatchedPayments}: ${dashboard.unmatchedCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(strings.tapToAssign, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BigButton(strings.addBill, onClick = onAddBill)
                }
                Column(modifier = Modifier.weight(1f)) {
                    BigButton(strings.didMoneyCome, onClick = onOpenCheck)
                }
            }
        }

        if (customers.isEmpty()) {
            item {
                BigButton(strings.addCustomer, onClick = onAddCustomer)
            }
        }

        if (payments.isNotEmpty()) {
            item {
                Text(
                    strings.payments,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(payments.take(5)) { payment ->
                PaymentRow(
                    payment = payment,
                    customerName = payment.customerId?.let { id ->
                        customers.firstOrNull { it.id == id }?.name
                    },
                    strings = strings
                )
            }
        }
    }
}
