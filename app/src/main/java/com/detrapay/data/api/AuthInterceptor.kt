package com.detrapay.data.api

import com.detrapay.data.repositories.AuthRepository
import com.detrapay.ui.util.DeviceUtils
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private var authRepository: AuthRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestUrl = chain.request().url.encodedPath
        if (requestUrl.endsWith("/auth/local") || requestUrl.endsWith("/auth/refresh")) {
            return chain.proceed(
                chain.request().newBuilder()
                    .addHeader("x-device-serial", DeviceUtils.getSerialNumber())
                    .build()
            )
        }

        val request = chain.request().newBuilder()
            .addHeader("x-device-serial", DeviceUtils.getSerialNumber())

        val accessToken = authRepository.currentAccessToken() ?: runBlocking {
            authRepository.getLoggedUser()?.sessionToken
        }
        if (!accessToken.isNullOrBlank()) {
            request.addHeader("Authorization", "${authRepository.currentTokenType()} $accessToken")
        }
        return chain.proceed(request.build())
    }
}
