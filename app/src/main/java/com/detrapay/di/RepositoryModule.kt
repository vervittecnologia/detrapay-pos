package com.detrapay.di

import com.detrapay.data.datasources.local.PaymentDAO
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.repositories.LoginRepository
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.data.repositories.RegistrationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideLoginRepository(
        userLocalDatasource: UsersDao,
        detrapayRemoteDataSource: DetrapayRemoteDataSource
    ): LoginRepository {
        return LoginRepository(
            userLocalDatasource,
            detrapayRemoteDataSource
        )
    }

    @Singleton
    @Provides
    fun provideAuthRepository(
        userLocalDatasource: UsersDao,
    ): AuthRepository {
        return AuthRepository(userLocalDatasource)
    }

    @Singleton
    @Provides
    fun provideOrderRepository(detrapayRemoteDataSource: DetrapayRemoteDataSource): OrderRepository {
        return OrderRepository(detrapayRemoteDataSource)
    }

    @Singleton
    @Provides
    fun provideRegistrationRepository(detrapayRemoteDataSource: DetrapayRemoteDataSource): RegistrationRepository {
        return RegistrationRepository(detrapayRemoteDataSource)
    }

    @Singleton
    @Provides
    fun providePaymentRepository(paymentLocalDataSource: PaymentDAO): PaymentRepository {
        return PaymentRepository(paymentLocalDataSource)
    }
}