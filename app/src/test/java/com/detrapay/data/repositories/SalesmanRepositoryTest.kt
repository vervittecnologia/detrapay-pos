package com.detrapay.data.repositories

import com.detrapay.data.Result
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.Company
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.remote.SalespersonAttributesResponse
import com.detrapay.data.model.remote.SalespersonResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SalesmanRepositoryTest {

    private val remoteDataSource = mockk<DetrapayRemoteDataSource>()
    private val authRepository = mockk<AuthRepository>()

    private lateinit var repository: SalesmanRepository

    @Before
    fun setUp() {
        repository = SalesmanRepository(
            detrapayRemoteDataSource = remoteDataSource,
            authRepository = authRepository,
        )
    }

    @Test
    fun `getSalesmen uses cached salesmen when api returns empty list`() = runTest {
        val cachedSalesmen = listOf(Salesman(id = 7, name = "Vendedor local"))
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser(cachedSalesmen)
        coEvery { remoteDataSource.getSalespeople(10) } returns Result.Success(emptyList())

        val result = repository.getSalesmen()

        assertTrue(result is Result.Success)
        assertEquals(cachedSalesmen, (result as Result.Success).data)
    }

    @Test
    fun `getSalesmen uses cached salesmen when api fails`() = runTest {
        val cachedSalesmen = listOf(Salesman(id = 7, name = "Vendedor local"))
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser(cachedSalesmen)
        coEvery { remoteDataSource.getSalespeople(10) } returns Result.Error(Exception("network"))

        val result = repository.getSalesmen()

        assertTrue(result is Result.Success)
        assertEquals(cachedSalesmen, (result as Result.Success).data)
    }

    @Test
    fun `getSalesmen returns only active salesmen from api when available`() = runTest {
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser(emptyList())
        coEvery { remoteDataSource.getSalespeople(10) } returns Result.Success(
            listOf(
                SalespersonResponse(
                    id = 1,
                    attributes = SalespersonAttributesResponse(
                        name = "Ativo",
                        companyId = 10,
                        isActive = true,
                    ),
                ),
                SalespersonResponse(
                    id = 2,
                    attributes = SalespersonAttributesResponse(
                        name = "Inativo",
                        companyId = 10,
                        isActive = false,
                    ),
                ),
            )
        )

        val result = repository.getSalesmen()

        assertTrue(result is Result.Success)
        assertEquals(listOf(Salesman(id = 1, name = "Ativo")), (result as Result.Success).data)
    }

    @Test
    fun `getSalesmen supports flat contract response`() = runTest {
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser(emptyList())
        coEvery { remoteDataSource.getSalespeople(10) } returns Result.Success(
            listOf(
                SalespersonResponse(
                    id = 3,
                    attributes = null,
                    name = "Maria",
                    phoneNumber = "5511999999999",
                    email = "maria@email.com",
                    isActive = true
                ),
                SalespersonResponse(
                    id = 4,
                    attributes = null,
                    name = "Inativo",
                    isActive = false
                )
            )
        )

        val result = repository.getSalesmen()

        assertTrue(result is Result.Success)
        assertEquals(listOf(Salesman(id = 3, name = "Maria", phoneNumber = "5511999999999", email = "maria@email.com")), (result as Result.Success).data)
    }

    private fun loggedUser(salesmen: List<Salesman>) = LoggedInUser(
        id = "user-1",
        sessionToken = "token",
        displayName = "Usuario",
        username = "user",
        cpfCnpj = "12345678000190",
        email = "user@example.com",
        companies = listOf(Company(id = 10, name = "Detrapay")),
        dispatchers = emptyList(),
        salesmen = salesmen,
    )
}
