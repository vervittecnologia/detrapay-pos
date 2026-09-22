package com.detrapay.data.api

import okhttp3.Authenticator
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object PublicMediaClientFactory {
    fun create(timeoutMs: Long = 10_000L): OkHttpClient = OkHttpClient.Builder()
        .authenticator(Authenticator.NONE)
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
