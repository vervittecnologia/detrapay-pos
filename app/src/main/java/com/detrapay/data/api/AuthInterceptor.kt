package com.detrapay.data.api

import com.detrapay.data.repositories.AuthRepository
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
        if (loggedInUser != null) {
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer " + loggedInUser.sessionToken).build()
            return chain.proceed(request)
        } else {
            return chain.proceed(chain.request())
        }
    }
}