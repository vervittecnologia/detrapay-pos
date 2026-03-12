package com.detrapay.data.api

import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.model.remote.RefreshSessionRequest
import kotlinx.coroutines.runBlocking
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

        val refreshed = runBlocking {
            val refreshToken = authRepository.currentRefreshToken()?.takeIf { it.isNotBlank() }
                ?: return@runBlocking false

            val refreshResponse = noAuthDetrapayService.refresh(
                refreshTokenHeader = refreshToken,
                payload = RefreshSessionRequest(refreshToken = refreshToken),
            )

            if (!refreshResponse.isSuccessful) {
                return@runBlocking false
            }

            authRepository.updateSessionFromRefresh(
                refreshResponse.body() ?: return@runBlocking false
            )
        }

        if (!refreshed) return null

        val refreshedToken = authRepository.currentAccessToken() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "${authRepository.currentTokenType()} $refreshedToken")
            .build()
    }

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
