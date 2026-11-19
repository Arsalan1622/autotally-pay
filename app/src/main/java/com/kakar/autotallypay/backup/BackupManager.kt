package com.kakar.autotallypay.backup

import androidx.room.withTransaction
import com.kakar.autotallypay.data.AppDatabase
import com.kakar.autotallypay.data.Bill
import com.kakar.autotallypay.data.Customer
import com.kakar.autotallypay.data.Payment
import com.kakar.autotallypay.data.PaymentAllocation
import com.kakar.autotallypay.data.SenderMapping
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encrypted backup: the whole database is dumped to JSON, then
 * encrypted with the user's password (AES-256-GCM). The file is
 * the user's to keep — nothing leaves the phone.
 */
object BackupManager {

    suspend fun exportEncrypted(db: AppDatabase, password: String): ByteArray {
        val json = exportJson(db)
        return BackupCrypto.encrypt(password, json.toByteArray(Charsets.UTF_8))
    }

    suspend fun importEncrypted(db: AppDatabase, password: String, data: ByteArray) {
        val json = String(BackupCrypto.decrypt(password, data), Charsets.UTF_8)
        importJson(db, json)
    }

    suspend fun exportJson(db: AppDatabase): String {
        val root = JSONObject()
        root.put("app", "AutoTally Pay")
        root.put("version", 1)
        root.put("customers", JSONArray().apply {
            for (c in db.customers().allOnce()) {
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("phone", c.phone)
                    put("notes", c.notes); put("createdAt", c.createdAt)
                })
            }
        })
        root.put("bills", JSONArray().apply {
            for (b in db.bills().allOnce()) {
                put(JSONObject().apply {
                    put("id", b.id); put("customerId", b.customerId)
                    put("amountPaisa", b.amountPaisa); put("description", b.description)
                    put("dateMillis", b.dateMillis)
                })
            }
        })
        root.put("payments", JSONArray().apply {
            for (p in db.payments().allOnce()) {
                put(JSONObject().apply {
                    put("id", p.id)
                    if (p.customerId == null) put("customerId", JSONObject.NULL) else put("customerId", p.customerId)
                    put("amountPaisa", p.amountPaisa); put("source", p.source)
                    put("status", p.status); put("senderId", p.senderId)
                    put("senderName", p.senderName ?: JSONObject.NULL)
                    put("accountHint", p.accountHint ?: JSONObject.NULL)
                    put("trxId", p.trxId ?: JSONObject.NULL)
                    put("rawSms", p.rawSms)
                    put("fakeReason", p.fakeReason ?: JSONObject.NULL)
                    put("receivedAt", p.receivedAt)
                    put("matchedAutomatically", p.matchedAutomatically)
                })
            }
        })
        root.put("allocations", JSONArray().apply {
            for (a in db.allocations().allOnce()) {
                put(JSONObject().apply {
                    put("id", a.id); put("paymentId", a.paymentId)
                    put("billId", a.billId); put("amountPaisa", a.amountPaisa)
                })
            }
        })
        root.put("mappings", JSONArray().apply {
            for (m in db.mappings().allOnce()) {
                put(JSONObject().apply {
                    put("id", m.id); put("senderKey", m.senderKey)
                    put("customerId", m.customerId); put("label", m.label)
                    put("createdAt", m.createdAt)
                })
            }
        })
        return root.toString()
    }

    suspend fun importJson(db: AppDatabase, json: String) {
        val root = JSONObject(json)
        require(root.optString("app") == "AutoTally Pay") { "Not an AutoTally Pay backup" }
        val customers = mutableListOf<Customer>()
        root.optJSONArray("customers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                customers += Customer(
                    id = o.getLong("id"), name = o.getString("name"),
                    phone = o.optString("phone"), notes = o.optString("notes"),
                    createdAt = o.optLong("createdAt")
                )
            }
        }
        val bills = mutableListOf<Bill>()
        root.optJSONArray("bills")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                bills += Bill(
                    id = o.getLong("id"), customerId = o.getLong("customerId"),
                    amountPaisa = o.getLong("amountPaisa"),
                    description = o.getString("description"),
                    dateMillis = o.optLong("dateMillis")
                )
            }
        }
        val payments = mutableListOf<Payment>()
        root.optJSONArray("payments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                payments += Payment(
                    id = o.getLong("id"),
                    customerId = if (o.isNull("customerId")) null else o.getLong("customerId"),
                    amountPaisa = o.getLong("amountPaisa"),
                    source = o.getString("source"), status = o.getString("status"),
                    senderId = o.getString("senderId"),
                    senderName = o.optString("senderName").takeIf { !o.isNull("senderName") },
                    accountHint = o.optString("accountHint").takeIf { !o.isNull("accountHint") },
                    trxId = o.optString("trxId").takeIf { !o.isNull("trxId") },
                    rawSms = o.getString("rawSms"),
                    fakeReason = o.optString("fakeReason").takeIf { !o.isNull("fakeReason") },
                    receivedAt = o.optLong("receivedAt"),
                    matchedAutomatically = o.optBoolean("matchedAutomatically")
                )
            }
        }
        val allocations = mutableListOf<PaymentAllocation>()
        root.optJSONArray("allocations")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                allocations += PaymentAllocation(
                    id = o.getLong("id"), paymentId = o.getLong("paymentId"),
                    billId = o.getLong("billId"), amountPaisa = o.getLong("amountPaisa")
                )
            }
        }
        val mappings = mutableListOf<SenderMapping>()
        root.optJSONArray("mappings")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                mappings += SenderMapping(
                    id = o.getLong("id"), senderKey = o.getString("senderKey"),
                    customerId = o.getLong("customerId"), label = o.optString("label"),
                    createdAt = o.optLong("createdAt")
                )
            }
        }
        db.withTransaction {
            db.allocations().deleteAll()
            db.payments().deleteAll()
            db.bills().deleteAll()
            db.mappings().deleteAll()
            db.customers().deleteAll()
            for (c in customers) db.customers().insert(c)
            for (b in bills) db.bills().insert(b)
            for (p in payments) db.payments().insert(p)
            db.allocations().insertAll(allocations)
            for (m in mappings) db.mappings().insert(m)
        }
    }
}
