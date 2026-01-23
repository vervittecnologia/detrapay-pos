package com.detrapay.di

import com.detrapay.data.database.AppDatabase
import com.detrapay.data.datasources.local.PaymentDAO
import com.detrapay.data.datasources.local.UsersDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDatasourceModule {
    @Singleton
    @Provides
    fun providelLoginLocalDataSource(appDatabase: AppDatabase) : UsersDao {
        return appDatabase.userDao()
    }

    @Singleton
    @Provides
    fun providelPaymentLocalDataSource(appDatabase: AppDatabase) : PaymentDAO {
        return appDatabase.paymentDao()
    }
}