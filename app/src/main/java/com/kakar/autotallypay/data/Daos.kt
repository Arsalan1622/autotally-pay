package com.kakar.autotallypay.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun byId(id: Long): Customer?

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    suspend fun allOnce(): List<Customer>

    @Insert
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dateMillis ASC")
    fun all(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY dateMillis ASC, id ASC")
    fun forCustomer(customerId: Long): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY dateMillis ASC, id ASC")
    suspend fun forCustomerOnce(customerId: Long): List<Bill>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun byId(id: Long): Bill?

    @Query("SELECT * FROM bills ORDER BY dateMillis ASC")
    suspend fun allOnce(): List<Bill>

    @Insert
    suspend fun insert(bill: Bill): Long

    @Delete
    suspend fun delete(bill: Bill)

    @Query("DELETE FROM bills")
    suspend fun deleteAll()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY receivedAt DESC")
    fun all(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun byId(id: Long): Payment?

    @Query("SELECT * FROM payments WHERE trxId = :trxId AND amountPaisa = :amountPaisa LIMIT 1")
    suspend fun byTrxIdAndAmount(trxId: String, amountPaisa: Long): Payment?

    @Query("SELECT * FROM payments WHERE status = 'VERIFIED' AND amountPaisa = :amountPaisa ORDER BY receivedAt DESC")
    suspend fun verifiedByAmount(amountPaisa: Long): List<Payment>

    @Query("SELECT * FROM payments WHERE status = 'VERIFIED' AND customerId IS NULL ORDER BY receivedAt DESC")
    suspend fun unmatchedVerified(): List<Payment>

    @Query("SELECT * FROM payments ORDER BY receivedAt DESC")
    suspend fun allOnce(): List<Payment>

    @Insert
    suspend fun insert(payment: Payment): Long

    @Update
    suspend fun update(payment: Payment)

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}

@Dao
interface AllocationDao {
    @Query("SELECT * FROM allocations")
    fun all(): Flow<List<PaymentAllocation>>

    @Query("SELECT * FROM allocations WHERE billId = :billId")
    suspend fun forBill(billId: Long): List<PaymentAllocation>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM allocations WHERE billId = :billId")
    fun paidForBill(billId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM allocations WHERE billId = :billId")
    suspend fun paidForBillOnce(billId: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM allocations WHERE paymentId = :paymentId")
    suspend fun allocatedForPayment(paymentId: Long): Long

    @Query("SELECT * FROM allocations")
    suspend fun allOnce(): List<PaymentAllocation>

    @Insert
    suspend fun insertAll(allocations: List<PaymentAllocation>)

    @Query("DELETE FROM allocations")
    suspend fun deleteAll()
}

@Dao
interface SenderMappingDao {
    @Query("SELECT * FROM sender_mappings ORDER BY label COLLATE NOCASE")
    fun all(): Flow<List<SenderMapping>>

    @Query("SELECT * FROM sender_mappings WHERE senderKey = :key LIMIT 1")
    suspend fun byKey(key: String): SenderMapping?

    @Query("SELECT * FROM sender_mappings")
    suspend fun allOnce(): List<SenderMapping>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: SenderMapping): Long

    @Delete
    suspend fun delete(mapping: SenderMapping)

    @Query("DELETE FROM sender_mappings")
    suspend fun deleteAll()
}
