package com.detrapay.data.datasources.remote

import com.detrapay.data.Result
import com.detrapay.data.api.DetrapayService
import com.detrapay.data.api.SupabaseService
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.remote.AddOrderReceivableRequest
import com.detrapay.data.model.remote.ConfirmPaymentRequest
import com.detrapay.data.model.remote.CreateOrderResponse
import com.detrapay.data.model.remote.FlatCustomerResponse
import com.detrapay.data.model.remote.FlatPaymentMethodResponse
import com.detrapay.data.model.remote.FlatSalesmanResponse
import com.detrapay.data.model.remote.FlatVehicleTypeResponse
import com.detrapay.data.model.remote.OrderResponse
import com.detrapay.data.model.remote.OrderReceivableMutationResponse
import com.detrapay.data.model.remote.PaymentMethodResponse
import com.detrapay.data.model.remote.RefundOrderReceivableRequest
import com.detrapay.data.model.remote.UpdateOrderSalesmanRequest
import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.slot
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class DetrapayRemoteDataSourceTest {

    private val detrapayService = mockk<DetrapayService>()
    private val supabaseService = mockk<SupabaseService>()

    private lateinit var dataSource: DetrapayRemoteDataSource

    @Before
    fun setUp() {
        dataSource = DetrapayRemoteDataSource(
            detrapayService = detrapayService,
            supabaseService = supabaseService
        )
    }

    @Test
    fun `getPaymentMethods supports flat contract response`() = runTest {
        coEvery { detrapayService.getPaymentMethods() } returns Response.success(
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

        val result = dataSource.getPaymentMethods()

        assertTrue(result is Result.Success)
        val methods = (result as Result.Success).data
        assertEquals(1, methods.size)
        assertEquals("Credito", methods.single().name)
    }

    @Test
    fun `payOrderReceivable unwraps updated order from backend`() = runTest {
        coEvery {
            detrapayService.confirmPayment("20", any<ConfirmPaymentRequest>())
        } returns Response.success(CreateOrderResponse(updatedOrderResponse()))

        val result = dataSource.payOrderReceivable(receivable(), paymentData())

        assertTrue(result is Result.Success)
        assertEquals(123, (result as Result.Success).data.id)
        assertEquals("paid", result.data.status)
    }

    @Test
    fun `payOrderReceivable forwards manual amount final without overwriting original`() = runTest {
        val requestSlot = slot<ConfirmPaymentRequest>()
        coEvery {
            detrapayService.confirmPayment("20", capture(requestSlot))
        } returns Response.success(CreateOrderResponse(updatedOrderResponse()))

        val result = dataSource.payOrderReceivable(
            receivable().copy(
                paymentMethod = receivable().paymentMethod.copy(
                    installments = 1,
                    paymentType = "cash"
                )
            ),
            paymentData().copy(
                amountFinal = 100.0
            )
        )

        assertTrue(result is Result.Success)
        assertNull(requestSlot.captured.amountOriginal)
        assertEquals(100.0, requestSlot.captured.amountFinal)
    }

    @Test
    fun `addOrderReceivable unwraps updated order from backend`() = runTest {
        coEvery {
            detrapayService.addOrderReceivable(123, any<AddOrderReceivableRequest>())
        } returns Response.success(
            OrderReceivableMutationResponse(
                data = JsonObject(),
                updatedOrder = CreateOrderResponse(updatedOrderResponse(status = "pending"))
            )
        )

        val result = dataSource.addOrderReceivable(
            orderId = 123,
            paymentMethodId = 168,
            amountOriginal = 1000.0,
            installments = 1,
            paymentDate = "2026-03-10"
        )

        assertTrue(result is Result.Success)
        assertEquals(123, (result as Result.Success).data.id)
        assertEquals("pending", result.data.status)
    }

    @Test
    fun `refundOrderReceivableItem unwraps updated order from backend`() = runTest {
        coEvery {
            detrapayService.refundOrderReceivableItem("abc", any<RefundOrderReceivableRequest>())
        } returns Response.success(
            OrderReceivableMutationResponse(
                data = JsonObject(),
                updatedOrder = CreateOrderResponse(updatedOrderResponse(status = "cancelled"))
            )
        )

        val result = dataSource.refundOrderReceivableItem("abc", "2026-03-06T10:20:30")

        assertTrue(result is Result.Success)
        assertEquals("cancelled", (result as Result.Success).data.status)
    }

    @Test
    fun `deleteOrderReceivableItem unwraps updated order from backend`() = runTest {
        coEvery {
            detrapayService.deleteOrderReceivableItem("abc")
        } returns Response.success(
            OrderReceivableMutationResponse(
                data = JsonObject(),
                updatedOrder = CreateOrderResponse(updatedOrderResponse(status = "cancelled"))
            )
        )

        val result = dataSource.deleteOrderReceivableItem("abc")

        assertTrue(result is Result.Success)
        assertEquals("cancelled", (result as Result.Success).data.status)
    }

    @Test
    fun `updateOrderSalesman unwraps updated order from backend`() = runTest {
        coEvery {
            detrapayService.updateOrderSalesman(123, any<UpdateOrderSalesmanRequest>())
        } returns Response.success(CreateOrderResponse(updatedOrderResponse(salesmanName = "Novo vendedor")))

        val result = dataSource.updateOrderSalesman(123, 55)

        assertTrue(result is Result.Success)
        assertEquals("Novo vendedor", (result as Result.Success).data.salesman?.name)
    }

    private fun updatedOrderResponse(
        status: String = "paid",
        salesmanName: String = "Maria"
    ) = OrderResponse(
        id = 123,
        status = status,
        createdAt = "2026-03-06T10:30:00Z",
        billingDate = "2026-03-06",
        originalAmount = 1299.9,
        currentAmount = 1299.9,
        customer = FlatCustomerResponse(
            id = 88,
            name = "Joao Silva",
            cpfCnpj = "12345678901"
        ),
        vehicleType = FlatVehicleTypeResponse(
            id = 3,
            name = "Carro"
        ),
        salesman = FlatSalesmanResponse(
            id = 4,
            name = salesmanName
        ),
        receivables = listOf(
            com.detrapay.data.model.remote.FlatOrderReceivableResponse(
                id = 20,
                documentId = "abc",
                status = status,
                installments = 1,
                paymentDate = "2026-03-06T10:40:00Z",
                amountOriginal = 1299.9,
                amountFinal = 1299.9,
                authorizationCode = "9999",
                paymentMethod = FlatPaymentMethodResponse(
                    id = 1,
                    name = "Credito",
                    installments = 12,
                    interestTax = 0.02,
                    paymentType = "credit"
                )
            )
        )
    )

    private fun receivable() = OrderReceivableItem(
        id = 20,
        documentId = "abc",
        amountOriginal = 1299.9,
        amountFinal = 1299.9,
        installments = 1,
        status = OrderReceivableItemStatus.PENDING,
        paymentMethod = PaymentMethod(
            id = 1,
            name = "Credito",
            installments = 12,
            interestTax = 0.02,
            paymentType = "credit"
        ),
        paymentDate = null,
        refundDate = null,
        cardLast4 = null,
        cardHolder = null,
        tax = null,
        cardBrand = null,
        authorizationId = null,
        authorizationCode = null,
        pixTxIdCode = null
    )

    private fun paymentData() = PaymentData(
        transactionId = "txn-1",
        transactionCode = "code-1",
        date = "2026-03-06",
        time = "10:20:30",
        cardBrand = "VISA",
        cardLast4 = "1234",
        cardHolder = "JOAO"
    )
}
