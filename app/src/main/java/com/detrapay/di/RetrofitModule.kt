package com.detrapay.di

import com.detrapay.BuildConfig
import com.detrapay.data.api.AuthInterceptor
import com.detrapay.data.api.DetrapayService
import com.detrapay.data.api.SessionAuthenticator
import com.detrapay.data.api.SupabaseService
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
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    private fun buildClient(
        timeoutMs: Long,
        authInterceptor: AuthInterceptor? = null,
        sessionAuthenticator: SessionAuthenticator? = null,
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder().apply {
            addInterceptor(loggingInterceptor)
            authInterceptor?.let { addInterceptor(it) }
            sessionAuthenticator?.let { authenticator(it) }
        }
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    @Named("DetrapayRetrofit")
    fun provideRetrofit(
        authInterceptor: AuthInterceptor,
        sessionAuthenticator: SessionAuthenticator,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(buildClient(timeoutMs = 10000L, authInterceptor = authInterceptor, sessionAuthenticator = sessionAuthenticator))
        .build()

    @Provides
    @Singleton
    @Named("NoAuthDetrapayRetrofit")
    fun provideNoAuthRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(buildClient(timeoutMs = 10000L))
        .build()

    @Provides
    @Singleton
    @Named("SupabaseRetrofit")
    fun provideSupabaseRetrofit(
        authInterceptor: AuthInterceptor,
        sessionAuthenticator: SessionAuthenticator,
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://ibulgxjbtpxratoodgtj.supabase.co/functions/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(buildClient(timeoutMs = 15000L, authInterceptor = authInterceptor, sessionAuthenticator = sessionAuthenticator))
        .build()

    @Provides
    @Singleton
    fun provideDetrapayService(@Named("DetrapayRetrofit") retrofit: Retrofit): DetrapayService= retrofit.create(DetrapayService::class.java)

    @Provides
    @Singleton
    @Named("NoAuthDetrapayService")
    fun provideNoAuthDetrapayService(@Named("NoAuthDetrapayRetrofit") retrofit: Retrofit): DetrapayService =
        retrofit.create(DetrapayService::class.java)

    @Provides
    @Singleton
    fun provideSupabaseService(@Named("SupabaseRetrofit") retrofit: Retrofit): SupabaseService = retrofit.create(SupabaseService::class.java)

    @Provides
    @Singleton
    fun provideAuthInterceptor(authRepository: AuthRepository): AuthInterceptor  = AuthInterceptor(authRepository)
}
