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
        val loggedInUser = runBlocking {
            return@runBlocking authRepository.getLoggedUser()
        }

        val request = chain.request().newBuilder()
            .addHeader("x-device-serial", DeviceUtils.getSerialNumber())

        if (loggedInUser != null) {
            request.addHeader("Authorization", "Bearer " + loggedInUser.sessionToken)
        }
        return chain.proceed(request.build())
    }
}