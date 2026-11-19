package com.kakar.autotallypay.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Customer::class,
        Bill::class,
        Payment::class,
        PaymentAllocation::class,
        SenderMapping::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customers(): CustomerDao
    abstract fun bills(): BillDao
    abstract fun payments(): PaymentDao
    abstract fun allocations(): AllocationDao
    abstract fun mappings(): SenderMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autotally.db"
                ).build().also { INSTANCE = it }
            }
    }
}
