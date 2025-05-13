package com.detrapay.di

import android.app.Application
import androidx.room.Room
import com.detrapay.data.database.DbConstant
import com.detrapay.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Provides
    @Singleton
    internal fun provideAppDatabase(application: Application) = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        DbConstant.DB_NAME
    ).build()
}