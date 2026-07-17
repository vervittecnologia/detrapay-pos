package com.detrapay.data.repositories

import com.detrapay.data.Result
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.remote.PaymentMethodResponse
import com.detrapay.data.model.remote.VehicleTypeItemResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegistrationRepositoryTest {

    private val remoteDataSource = mockk<DetrapayRemoteDataSource>()

    private lateinit var repository: RegistrationRepository

    @Before
    fun setUp() {
        repository = RegistrationRepository(
            detrapayRemoteDataSource = remoteDataSource
        )
    }

    @Test
    fun `loadVehicleTypes supports flat contract response`() = runTest {
        coEvery { remoteDataSource.getVehicleTypes() } returns Result.Success(
            listOf(
                VehicleTypeItemResponse(
                    id = 1,
                    name = "Carro",
                    attributes = null
                ),
                VehicleTypeItemResponse(
                    id = 2,
                    name = "Moto",
                    attributes = null
                )
            )
        )

        val result = repository.loadVehicleTypes()

        assertTrue(result is Result.Success)
        assertEquals("Carro", (result as Result.Success).data.first().name)
        assertEquals("Moto", result.data.last().name)
    }

    @Test
    fun `loadPaymentMethods supports flat contract response`() = runTest {
        coEvery { remoteDataSource.getPaymentMethods() } returns Result.Success(
            listOf(
                PaymentMethodResponse(
                    id = 1,
                    name = "Credito",
                    installments = 12,
                    interestTax = 0.02,
                    paymentType = "credit"
                )
            )
        )

        val result = repository.loadPaymentMethods()

        assertTrue(result is Result.Success)
        val method = (result as Result.Success).data.single()
        assertEquals("Credito", method.name)
        assertEquals(12, method.installments)
        assertEquals("credit", method.paymentType)
    }
}
