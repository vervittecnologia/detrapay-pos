package com.detrapay.data.api

import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.model.remote.RefreshSessionRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SessionAuthenticator @Inject constructor(
    private val authRepository: AuthRepository,
    @Named("NoAuthDetrapayService") private val noAuthDetrapayService: DetrapayService,
) : Authenticator {
    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val path = response.request.url.encodedPath
        if (path.endsWith("/auth/local") || path.endsWith("/auth/refresh")) return null

        val requestToken = response.request.header("Authorization")
            ?.substringAfter(' ', "")
            ?.trim()

        val latestToken = authRepository.currentAccessToken()
        if (!latestToken.isNullOrBlank() && latestToken != requestToken) {
            return response.request.newBuilder()
                .header("Authorization", "${authRepository.currentTokenType()} $latestToken")
                .build()
        }

        return runBlocking {
            refreshMutex.withLock {
                val tokenAfterLock = authRepository.currentAccessToken()
                if (!tokenAfterLock.isNullOrBlank() && tokenAfterLock != requestToken) {
                    return@withLock authorizedRequest(response.request, tokenAfterLock)
                }

                val refreshToken = authRepository.currentRefreshToken()?.takeIf { it.isNotBlank() }
                    ?: return@withLock null
                val sessionGeneration = authRepository.currentSessionGeneration()
                val refreshResponse = noAuthDetrapayService.refresh(
                    refreshTokenHeader = refreshToken,
                    payload = RefreshSessionRequest(refreshToken = refreshToken),
                )
                if (!refreshResponse.isSuccessful) return@withLock null
                val refreshBody = refreshResponse.body() ?: return@withLock null
                if (!authRepository.updateSessionFromRefresh(refreshBody, sessionGeneration)) return@withLock null

                val refreshedToken = authRepository.currentAccessToken() ?: return@withLock null
                authorizedRequest(response.request, refreshedToken)
            }
        }
    }

    private fun authorizedRequest(request: Request, token: String): Request = request.newBuilder()
        .header("Authorization", "${authRepository.currentTokenType()} $token")
        .build()

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }
}
