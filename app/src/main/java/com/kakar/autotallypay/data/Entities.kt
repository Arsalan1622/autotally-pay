package com.kakar.autotallypay.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId")]
)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    /** Amount in paisa (Long). Never floats. */
    val amountPaisa: Long,
    val description: String,
    val dateMillis: Long = System.currentTimeMillis()
)

/**
 * One received payment SMS. Also the SMS Inbox record.
 * "Paid" is never stored on a bill — it is the sum of [PaymentAllocation]s.
 */
@Entity(
    tableName = "payments",
    indices = [Index("customerId"), Index("trxId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Null until matched to a customer (auto via name memory, or by hand). */
    val customerId: Long?,
    val amountPaisa: Long,
    /** PaymentSource.name, e.g. EASYPAISA */
    val source: String,
    /** VerificationStatus.name: VERIFIED, FAKE, UNKNOWN_SENDER */
    val status: String,
    val senderId: String,
    val senderName: String?,
    val accountHint: String?,
    val trxId: String?,
    val rawSms: String,
    val fakeReason: String?,
    val receivedAt: Long = System.currentTimeMillis(),
    val matchedAutomatically: Boolean = false
)

@Entity(
    tableName = "allocations",
    foreignKeys = [
        ForeignKey(
            entity = Payment::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("paymentId"), Index("billId")]
)
data class PaymentAllocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paymentId: Long,
    val billId: Long,
    val amountPaisa: Long
)

/** Name memory: a sender key (number / account hint / name) -> customer. */
@Entity(
    tableName = "sender_mappings",
    indices = [Index(value = ["senderKey"], unique = true)]
)
data class SenderMapping(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderKey: String,
    val customerId: Long,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
