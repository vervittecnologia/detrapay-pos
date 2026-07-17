package com.detrapay.di

import android.content.Context
import com.detrapay.data.datasources.local.PaymentDAO
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.repositories.LoginRepository
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
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
    fun provideAuthRepository(
        userLocalDatasource: UsersDao,
        context: Context,
    ): AuthRepository {
        return AuthRepository(userLocalDatasource, context)
    }

    @Singleton
    @Provides
    fun provideOrderRepository(
        detrapayRemoteDataSource: DetrapayRemoteDataSource,
        authRepository: AuthRepository
    ): OrderRepository {
        return OrderRepository(detrapayRemoteDataSource, authRepository)
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

    @Singleton
    @Provides
    fun provideSalesmanRepository(
        detrapayRemoteDataSource: DetrapayRemoteDataSource,
        authRepository: AuthRepository
    ): SalesmanRepository {
        return SalesmanRepository(detrapayRemoteDataSource, authRepository)
    }
}
