package com.detrapay.data.repositories

import com.detrapay.data.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLifecycleCoordinatorTest {

    private val orderRepository = mockk<OrderRepository>(relaxed = true)
    private val salesmanRepository = mockk<SalesmanRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>()
    private val coordinator = SessionLifecycleCoordinator(
        orderRepository,
        salesmanRepository,
        authRepository,
    )

    @Test
    fun `logout clears caches before deleting credentials`() = runTest {
        coEvery { authRepository.logout() } returns Unit

        val result = coordinator.logout()

        assertTrue(result is Result.Success)
        coVerifyOrder {
            orderRepository.clearCache()
            salesmanRepository.clearCache()
            authRepository.logout()
        }
        coVerify(exactly = 1) { authRepository.logout() }
    }
}
