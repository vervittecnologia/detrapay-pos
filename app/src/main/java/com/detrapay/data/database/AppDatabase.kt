package com.detrapay.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.detrapay.data.datasources.local.PaymentDAO
import com.detrapay.data.model.local.User
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.model.local.Payment

@Database(
    entities = [User::class, Payment::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DataConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UsersDao
    abstract fun paymentDao(): PaymentDAO
}
