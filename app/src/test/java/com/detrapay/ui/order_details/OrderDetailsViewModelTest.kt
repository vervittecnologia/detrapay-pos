package com.detrapay.ui.order_details

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.detrapay.data.Result
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderCustomer
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.RefundPaymentData
import com.detrapay.data.model.VehicleType
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.testing.MainDispatcherRule
import com.detrapay.testing.getOrAwaitValueMatching
import com.detrapay.ui.state.UIState
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OrderDetailsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val orderRepository = mockk<OrderRepository>()
    private val registrationRepository = mockk<RegistrationRepository>()
    private val salesmanRepository = mockk<SalesmanRepository>()

    private lateinit var viewModel: OrderDetailsViewModel

    @Before
    fun setUp() {
        viewModel = OrderDetailsViewModel(
            orderRepository = orderRepository,
            registrationRepository = registrationRepository,
            salesmanRepository = salesmanRepository
        )
    }

    @Test
    fun `updateOrderSalesman publishes updated order from repository`() {
        val updatedOrder = order(status = OrderStatus.PAID)
        coEvery { orderRepository.getOrder(123) } returns Result.Success(order(status = OrderStatus.PENDING))
        coEvery { orderRepository.updateOrderSalesman(123, 4) } returns Result.Success(updatedOrder)

        viewModel.loadScreenContent(123)
        viewModel.orderState.getOrAwaitValueMatching { it is UIState.Success<*> }
        viewModel.updateOrderSalesman(com.detrapay.data.model.Salesman(id = 4, name = "Maria"))

        val state = viewModel.orderState.getOrAwaitValueMatching {
            it is UIState.Success<*> && it.data?.status == OrderStatus.PAID
        }

        assertTrue(state is UIState.Success)
        assertEquals(OrderStatus.PAID, state.data?.status)
    }

    @Test
    fun `payOrder publishes retry payload when repository fails`() {
        val receivable = receivable()
        val paymentData = PaymentData(transactionId = "txn-1")
        coEvery { orderRepository.payOrder(0, receivable, paymentData) } returns Result.Error(Exception("server error"))

        viewModel.payOrder(receivable, paymentData)

        val state = viewModel.orderState.getOrAwaitValueMatching { it is UIState.Error<*> } as UIState.Error

        assertEquals("server error", state.message)
        assertTrue(state.retryData is RetryDataModel)
    }

    @Test
    fun `refundItem publishes updated order on success`() {
        val updatedOrder = order(status = OrderStatus.CANCELLED)
        val refund = RefundPaymentData(date = "06/03/2026", time = "10:20:30")
        coEvery { orderRepository.refundOrderPayment(0, any(), refund) } returns Result.Success(updatedOrder)

        viewModel.refundItem(receivable(), refund)

        val state = viewModel.orderState.getOrAwaitValueMatching { it is UIState.Success<*> }

        assertTrue(state is UIState.Success)
        assertEquals(OrderStatus.CANCELLED, state.data?.status)
    }

    @Test
    fun `cancelPendingItem publishes updated order on success`() {
        val updatedOrder = order(status = OrderStatus.PENDING)
        val receivable = receivable()
        coEvery { orderRepository.cancelPendingReceivable(0, receivable) } returns Result.Success(updatedOrder)

        viewModel.cancelPendingItem(receivable)

        val state = viewModel.orderState.getOrAwaitValueMatching { it is UIState.Success<*> }

        assertTrue(state is UIState.Success)
        assertEquals(OrderStatus.PENDING, state.data?.status)
    }

    private fun order(status: OrderStatus) = Order(
        id = 123,
        serviceName = "Detrapay",
        status = status,
        creationDate = "2026-03-06T10:30:00Z",
        vehiclePrice = 10000.0,
        billingDate = "2026-03-06",
        originalAmount = 1299.9,
        currentAmount = 1299.9,
        isVehicleFinanced = false,
        isVehicleSpecialPlate = false,
        customer = OrderCustomer(
            id = 1,
            name = "Joao",
            cpfCnpj = "12345678901",
            phoneNumber = "5511999999999",
            email = "joao@email.com"
        ),
        vehicleType = VehicleType(3, "Carro"),
        items = emptyList(),
        receivables = listOf(receivable()),
        salesman = com.detrapay.data.model.Salesman(id = 4, name = "Maria")
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
}
