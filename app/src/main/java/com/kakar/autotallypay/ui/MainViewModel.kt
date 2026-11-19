package com.kakar.autotallypay.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kakar.autotallypay.backup.BackupManager
import com.kakar.autotallypay.data.AppDatabase
import com.kakar.autotallypay.data.Bill
import com.kakar.autotallypay.data.Customer
import com.kakar.autotallypay.data.Payment
import com.kakar.autotallypay.data.PaymentAllocation
import com.kakar.autotallypay.data.Repository
import com.kakar.autotallypay.data.SenderMapping
import com.kakar.autotallypay.domain.PaidCalc
import com.kakar.autotallypay.sms.ParsedPayment
import com.kakar.autotallypay.sms.SmsParser
import com.kakar.autotallypay.sms.VerificationStatus
import com.kakar.autotallypay.work.RematchWorker
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val todayReceived: Long = 0,
    val totalOutstanding: Long = 0,
    val pendingBills: Int = 0,
    val fakeCount: Int = 0,
    val unmatchedCount: Int = 0
)

data class CheckResult(val amountPaisa: Long, val matches: List<Payment>)

enum class BackupResult { SUCCESS, WRONG_PASSWORD, FAILED }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val repo = Repository(db)
    private val prefs = app.getSharedPreferences("autotally_prefs", Context.MODE_PRIVATE)

    val customers: StateFlow<List<Customer>> = repo.customers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val bills: StateFlow<List<Bill>> = repo.bills.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val payments: StateFlow<List<Payment>> = repo.payments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allocations: StateFlow<List<PaymentAllocation>> = repo.allocations.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val mappings: StateFlow<List<SenderMapping>> = repo.mappings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<AppLanguage> = _language
    val strings: StateFlow<UiStrings> = _language.map { stringsFor(it) }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), stringsFor(loadLanguage())
    )

    val dashboard: StateFlow<DashboardState> =
        combine(bills, allocations, payments) { billList, allocList, paymentList ->
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            var outstanding = 0L
            var pending = 0
            for (bill in billList) {
                val paid = PaidCalc.paidForBill(
                    allocList.map { com.kakar.autotallypay.domain.Allocation(it.billId, it.amountPaisa) },
                    bill.id
                )
                val left = PaidCalc.outstanding(bill.amountPaisa, paid)
                outstanding += left
                if (left > 0) pending++
            }
            DashboardState(
                todayReceived = paymentList
                    .filter { it.status == VerificationStatus.VERIFIED.name && it.receivedAt >= startOfToday }
                    .sumOf { it.amountPaisa },
                totalOutstanding = outstanding,
                pendingBills = pending,
                fakeCount = paymentList.count { it.status == VerificationStatus.FAKE.name },
                unmatchedCount = paymentList.count {
                    it.status == VerificationStatus.VERIFIED.name && it.customerId == null
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    /** Bills that just turned fully paid — drives the red-to-green celebration. */
    private val _justPaidBillIds = MutableStateFlow<Set<Long>>(emptySet())
    val justPaidBillIds: StateFlow<Set<Long>> = _justPaidBillIds

    private val _notice = MutableStateFlow<Notice?>(null)
    val notice: StateFlow<Notice?> = _notice

    /** SMS text shared into the app (Lite) or pasted, awaiting sender confirmation. */
    private val _sharedSmsText = MutableStateFlow<String?>(null)
    val sharedSmsText: StateFlow<String?> = _sharedSmsText
    val sharedParsed: StateFlow<ParsedPayment?> = _sharedSmsText.map { text ->
        text?.let { SmsParser.parseSms("", it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _checkResult = MutableStateFlow<CheckResult?>(null)
    val checkResult: StateFlow<CheckResult?> = _checkResult

    private var justPaidClearJob: Job? = null

    // ---- derived helpers (paid is always calculated, never stored) ----

    fun paidForBill(billId: Long): Long =
        allocations.value.filter { it.billId == billId }.sumOf { it.amountPaisa }

    fun outstandingForBill(bill: Bill): Long =
        PaidCalc.outstanding(bill.amountPaisa, paidForBill(bill.id))

    fun billsForCustomer(customerId: Long): List<Bill> =
        bills.value.filter { it.customerId == customerId }

    fun customerOutstanding(customerId: Long): Long =
        billsForCustomer(customerId).sumOf { outstandingForBill(it) }

    fun customerById(id: Long): Customer? = customers.value.firstOrNull { it.id == id }

    fun unallocatedCredit(customerId: Long): Long {
        val paid = payments.value
            .filter { it.customerId == customerId && it.status == VerificationStatus.VERIFIED.name }
            .sumOf { it.amountPaisa }
        val allocated = billsForCustomer(customerId).sumOf { paidForBill(it.id) }
        return (paid - allocated).coerceAtLeast(0)
    }

    // ---- language ----

    private fun loadLanguage(): AppLanguage =
        if (prefs.getString("language", "en") == "ur") AppLanguage.URDU else AppLanguage.ENGLISH

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("language", if (lang == AppLanguage.URDU) "ur" else "en").apply()
        _language.value = lang
    }

    // ---- customers & bills ----

    fun addCustomer(name: String, phone: String, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.addCustomer(name, phone)
            _notice.value = Notice("customerAdded")
            onDone(id)
        }
    }

    fun addBill(customerId: Long, amountPaisa: Long, description: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.addBill(customerId, amountPaisa, description)
            _notice.value = Notice("billAdded")
            onDone()
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch { repo.deleteBill(bill) }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch { repo.deleteCustomer(customer) }
    }

    fun deleteMapping(mapping: SenderMapping) {
        viewModelScope.launch { repo.deleteMapping(mapping) }
    }

    // ---- SMS processing ----

    /** Paste path (both flavors): sender typed in by the user. */
    fun processSmsText(sender: String, body: String) {
        viewModelScope.launch {
            val result = repo.processSms(sender.trim(), body)
            handleProcessResult(result)
        }
    }

    fun setSharedSms(text: String?) {
        _sharedSmsText.value = text?.takeIf { it.isNotBlank() }
    }

    /** Lite confirm path: user picked the sender from the original message. */
    fun confirmSharedSms(sender: String) {
        val text = _sharedSmsText.value ?: return
        _sharedSmsText.value = null
        viewModelScope.launch {
            val result = repo.processSms(sender.trim(), text)
            handleProcessResult(result)
        }
    }

    private suspend fun handleProcessResult(result: com.kakar.autotallypay.data.ProcessResult) {
        val payment = result.payment
        if (payment == null) {
            _notice.value = Notice("notAPaymentSms")
            return
        }
        if (result.duplicate) {
            _notice.value = Notice("alreadyRecorded", payment.amountPaisa)
            return
        }
        when (payment.status) {
            VerificationStatus.FAKE.name, VerificationStatus.UNKNOWN_SENDER.name ->
                _notice.value = Notice("fakeSmsFlagged", payment.amountPaisa)
            else -> {
                val name = payment.customerId?.let { customerById(it)?.name }
                if (payment.customerId != null) {
                    _notice.value = Notice("paymentRecorded", payment.amountPaisa, name)
                } else {
                    _notice.value = Notice("paymentRecordedUnmatched", payment.amountPaisa)
                }
            }
        }
        if (result.newlyPaidBillIds.isNotEmpty()) {
            _justPaidBillIds.value = result.newlyPaidBillIds.toSet()
            justPaidClearJob?.cancel()
            justPaidClearJob = viewModelScope.launch {
                delay(5000)
                _justPaidBillIds.value = emptySet()
            }
        }
    }

    fun assignPayment(paymentId: Long, customerId: Long) {
        viewModelScope.launch {
            val newlyPaid = repo.assignPaymentToCustomer(paymentId, customerId)
            _notice.value = Notice("assigned", customerName = customerById(customerId)?.name)
            if (newlyPaid.isNotEmpty()) {
                _justPaidBillIds.value = newlyPaid.toSet()
                justPaidClearJob?.cancel()
                justPaidClearJob = viewModelScope.launch {
                    delay(5000)
                    _justPaidBillIds.value = emptySet()
                }
            }
            RematchWorker.enqueueOnce(getApplication())
        }
    }

    // ---- "Did the money come?" ----

    fun runCheck(amountPaisa: Long) {
        viewModelScope.launch {
            val matches = repo.findVerifiedByAmount(amountPaisa)
            _checkResult.value = CheckResult(amountPaisa, matches)
        }
    }

    fun clearCheck() {
        _checkResult.value = null
    }

    // ---- backup ----

    fun exportBackup(password: String, uri: Uri, onDone: (BackupResult) -> Unit) {
        viewModelScope.launch {
            try {
                val bytes = BackupManager.exportEncrypted(db, password)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IllegalStateException("Cannot open output")
                onDone(BackupResult.SUCCESS)
            } catch (e: Exception) {
                onDone(BackupResult.FAILED)
            }
        }
    }

    fun importBackup(password: String, uri: Uri, onDone: (BackupResult) -> Unit) {
        viewModelScope.launch {
            try {
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Cannot open input")
                BackupManager.importEncrypted(db, password, bytes)
                onDone(BackupResult.SUCCESS)
            } catch (e: javax.crypto.AEADBadTagException) {
                onDone(BackupResult.WRONG_PASSWORD)
            } catch (e: Exception) {
                // Wrong password can also surface as a generic crypto error.
                val msg = (e.message ?: "").lowercase()
                if (msg.contains("tag mismatch") || msg.contains("mac check failed")) {
                    onDone(BackupResult.WRONG_PASSWORD)
                } else {
                    onDone(BackupResult.FAILED)
                }
            }
        }
    }

    fun clearNotice() {
        _notice.value = null
    }
}
