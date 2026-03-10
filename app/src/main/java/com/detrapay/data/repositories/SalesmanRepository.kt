package com.detrapay.data.repositories

import com.detrapay.data.Result
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.Salesman
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesmanRepository @Inject constructor(
    private val detrapayRemoteDataSource: DetrapayRemoteDataSource,
    private val authRepository: AuthRepository
) {

    private data class CacheEntry<T>(
        val value: T,
        val timestampMs: Long,
    )

    private val cacheTtlMs = 10 * 60 * 1000L
    private var salesmenCache: CacheEntry<List<Salesman>>? = null

    private fun <T> CacheEntry<T>.isValid(ttlMs: Long): Boolean {
        return System.currentTimeMillis() - timestampMs <= ttlMs
    }

    suspend fun getSalesmen(forceRefresh: Boolean = false): Result<List<Salesman>> {
        salesmenCache
            ?.takeIf { !forceRefresh && it.isValid(cacheTtlMs) }
            ?.let { return Result.Success(it.value) }

        val user = authRepository.getLoggedUser(false)
        val companyId = user?.companies?.firstOrNull()?.id
            ?: return Result.Error(Exception("ID da empresa nao encontrado."))
        val cachedSalesmen = user.salesmen

        return when (val result = detrapayRemoteDataSource.getSalespeople(companyId)) {
            is Result.Success -> {
                val activeSalesmen = result.data
                    .filter { it.attributes?.isActive ?: it.isActive ?: false }
                    .map {
                        Salesman(
                            id = it.id,
                            name = it.attributes?.name ?: it.name.orEmpty(),
                            phoneNumber = it.attributes?.phoneNumber ?: it.phoneNumber,
                            email = it.attributes?.email ?: it.email
                        )
                    }

                val resolvedSalesmen = activeSalesmen.ifEmpty { cachedSalesmen }
                salesmenCache = CacheEntry(
                    value = resolvedSalesmen,
                    timestampMs = System.currentTimeMillis()
                )
                Result.Success(resolvedSalesmen)
            }

            is Result.Error -> {
                if (cachedSalesmen.isNotEmpty()) {
                    salesmenCache = CacheEntry(
                        value = cachedSalesmen,
                        timestampMs = System.currentTimeMillis()
                    )
                    Result.Success(cachedSalesmen)
                } else {
                    result
                }
            }
        }
    }
}
