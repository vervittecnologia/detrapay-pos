package com.detrapay.data.api

import com.detrapay.data.model.remote.SessionRefreshResponse
import com.detrapay.data.repositories.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response as RetrofitResponse
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class SessionAuthenticatorTest {

    private val authRepository = mockk<AuthRepository>()
    private val service = mockk<DetrapayService>()

    @Test
    fun `concurrent 401 responses refresh once`() {
        val currentToken = AtomicReference("old")
        every { authRepository.currentAccessToken() } answers { currentToken.get() }
        every { authRepository.currentRefreshToken() } returns "refresh"
        every { authRepository.currentTokenType() } returns "Bearer"
        coEvery { service.refresh(any(), any()) } answers {
            Thread.sleep(100)
            RetrofitResponse.success(SessionRefreshResponse(accessToken = "new"))
        }
        coEvery { authRepository.updateSessionFromRefresh(any()) } answers {
            currentToken.set("new")
            true
        }
        val authenticator = SessionAuthenticator(authRepository, service)
        val executor = Executors.newFixedThreadPool(5)

        val results = List(5) {
            executor.submit<Request?> { authenticator.authenticate(null, unauthorized("old")) }
        }.map { it.get(3, TimeUnit.SECONDS) }
        executor.shutdownNow()

        assertEquals(List(5) { "Bearer new" }, results.map { it?.header("Authorization") })
        coVerify(exactly = 1) { service.refresh(any(), any()) }
    }

    @Test
    fun `failed refresh returns null without retry loop`() {
        every { authRepository.currentAccessToken() } returns "old"
        every { authRepository.currentRefreshToken() } returns "refresh"
        coEvery { service.refresh(any(), any()) } returns
            RetrofitResponse.error(401, "unauthorized".toResponseBody())
        val authenticator = SessionAuthenticator(authRepository, service)

        assertNull(authenticator.authenticate(null, unauthorized("old")))
        coVerify(exactly = 1) { service.refresh(any(), any()) }
    }

    private fun unauthorized(token: String): Response {
        val request = Request.Builder()
            .url("https://example.test/orders")
            .header("Authorization", "Bearer $token")
            .build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }
}
