package com.detrapay.di

import com.detrapay.BuildConfig
import com.detrapay.data.api.AuthInterceptor
import com.detrapay.data.api.DetrapayService
import com.detrapay.data.repositories.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    @Provides
    @Singleton
    fun provideRetrofit(authInterceptor: AuthInterceptor): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient().newBuilder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .addInterceptor(authInterceptor)
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS)
                .writeTimeout(10000L, TimeUnit.MILLISECONDS).build()
        ).build()

    @Provides
    @Singleton
    fun provideDetrapayService(retrofit: Retrofit): DetrapayService= retrofit.create(DetrapayService::class.java)

    @Provides
    @Singleton
    fun provideAuthInterceptor(authRepository: AuthRepository): AuthInterceptor  = AuthInterceptor(authRepository)
}
