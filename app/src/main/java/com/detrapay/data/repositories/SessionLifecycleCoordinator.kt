package com.detrapay.data.repositories

import com.detrapay.data.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionLifecycleCoordinator @Inject constructor(
    private val orderRepository: OrderRepository,
    private val salesmanRepository: SalesmanRepository,
    private val authRepository: AuthRepository,
) {
    private val logoutMutex = Mutex()

    suspend fun logout(): Result<Unit> = logoutMutex.withLock {
        runCatching {
            orderRepository.clearCache()
            salesmanRepository.clearCache()
            authRepository.logout()
            Result.Success(Unit)
        }.getOrElse { Result.Error(it as? Exception ?: Exception(it)) }
    }
}
