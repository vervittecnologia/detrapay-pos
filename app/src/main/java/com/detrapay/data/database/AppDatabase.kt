package com.detrapay.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.detrapay.data.datasources.local.PaymentDAO
import com.detrapay.data.datasources.local.PendingPaymentCompletionDao
import com.detrapay.data.model.local.User
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.model.local.Payment
import com.detrapay.data.model.local.PendingPaymentCompletion

@Database(
    entities = [User::class, Payment::class, PendingPaymentCompletion::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2),
        AutoMigration (from = 2, to = 3),
        AutoMigration (from = 3, to = 4),
        AutoMigration (from = 4, to = 5)
    ]
)
@TypeConverters(DataConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UsersDao
    abstract fun paymentDao(): PaymentDAO
    abstract fun pendingPaymentCompletionDao(): PendingPaymentCompletionDao
}
