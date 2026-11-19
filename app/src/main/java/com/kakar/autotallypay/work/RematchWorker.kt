package com.kakar.autotallypay.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kakar.autotallypay.data.AppDatabase
import com.kakar.autotallypay.data.Repository
import java.util.concurrent.TimeUnit

/**
 * Background re-match: when a new name-memory mapping is added, older
 * unmatched verified payments may now match a customer. Runs once on
 * demand and once a day. Fully offline — no network of any kind.
 */
class RematchWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.get(applicationContext)
            Repository(db).rematchUnmatched()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_NAME = "autotally-rematch"

        fun enqueueOnce(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<RematchWorker>().build())
        }

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<RematchWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
