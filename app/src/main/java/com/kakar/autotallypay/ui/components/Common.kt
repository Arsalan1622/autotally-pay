package com.kakar.autotallypay.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kakar.autotallypay.data.Bill
import com.kakar.autotallypay.data.Payment
import com.kakar.autotallypay.domain.Money
import com.kakar.autotallypay.domain.PaidCalc
import com.kakar.autotallypay.sms.VerificationStatus
import com.kakar.autotallypay.ui.UiStrings
import com.kakar.autotallypay.ui.theme.StatusColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(millis))

/** Big shopkeeper-friendly button (min 56dp tall). */
@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
        onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * One bill row. Background animates red -> green the moment the bill
 * becomes fully paid, with a popping check mark.
 */
@Composable
fun BillRow(
    bill: Bill,
    paidPaisa: Long,
    strings: UiStrings,
    justPaid: Boolean,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fullyPaid = PaidCalc.isFullyPaid(bill.amountPaisa, paidPaisa)
    val remaining = PaidCalc.outstanding(bill.amountPaisa, paidPaisa)
    val bg by animateColorAsState(
        targetValue = if (fullyPaid) StatusColors.paidGreenBg else StatusColors.unpaidRedBg,
        label = "billRowBg"
    )
    val accent = if (fullyPaid) StatusColors.paidGreen else StatusColors.unpaidRed
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        bill.description.ifBlank { strings.bills },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatDate(bill.dateMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Text(
                    Money.formatRupees(bill.amountPaisa),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = strings.delete)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (fullyPaid) strings.fullyPaid else "${strings.paid}: ${Money.formatRupees(paidPaisa)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                if (!fullyPaid) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${strings.remaining}: ${Money.formatRupees(remaining)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = accent
                    )
                }
            }
            // Red-to-green celebration when this bill was just fully paid.
            AnimatedVisibility(
                visible = justPaid,
                enter = fadeIn() + expandVertically() + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = StatusColors.paidGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        strings.fullyPaid,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.paidGreen
                    )
                }
            }
        }
    }
}

/** One payment row in the SMS Inbox. Fake payments are shown in red. */
@Composable
fun PaymentRow(
    payment: Payment,
    customerName: String?,
    strings: UiStrings,
    onAssignClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isFake = payment.status == VerificationStatus.FAKE.name
    val isUnknown = payment.status == VerificationStatus.UNKNOWN_SENDER.name
    val bg = when {
        isFake -> StatusColors.unpaidRedBg
        isUnknown -> StatusColors.warnAmberBg
        else -> Color.White
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    payment.source.replace('_', ' '),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    Money.formatRupees(payment.amountPaisa),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isFake) StatusColors.unpaidRed else Color.Unspecified
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                payment.senderName ?: payment.senderId,
                style = MaterialTheme.typography.bodyLarge
            )
            payment.trxId?.let {
                Text("${strings.trxId}: $it", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                formatDate(payment.receivedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isFake) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = StatusColors.unpaidRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        strings.fake,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.unpaidRed
                    )
                } else if (isUnknown) {
                    Text(
                        strings.unknownSender,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.warnAmber
                    )
                } else {
                    Text(
                        strings.verified,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.paidGreen
                    )
                }
                if (payment.matchedAutomatically) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        strings.autoMatched,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
            if (isFake || isUnknown) {
                payment.fakeReason?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${strings.fakeReasonLabel}: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusColors.unpaidRed
                    )
                }
            }
            if (customerName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${strings.customer}: $customerName",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            } else if (!isFake && !isUnknown && onAssignClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onAssignClick, modifier = Modifier.height(52.dp)) {
                    Text(strings.tapToAssign, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
