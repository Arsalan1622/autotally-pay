package com.kakar.autotallypay

import android.app.Application
import com.kakar.autotallypay.work.RematchWorker

class AutoTallyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Daily re-match: unmatched verified payments are retried against
        // the name-memory table (in case a mapping was added later).
        RematchWorker.enqueuePeriodic(this)
    }
}
