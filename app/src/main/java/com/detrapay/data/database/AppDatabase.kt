package com.detrapay.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.detrapay.data.model.local.User
import com.detrapay.data.datasources.local.UsersDao

@Database(entities = [User::class], version = 1, exportSchema = false)
@TypeConverters(DataConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UsersDao
}
