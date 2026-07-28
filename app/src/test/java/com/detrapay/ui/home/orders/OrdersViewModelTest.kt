package com.detrapay.ui.home.orders

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.detrapay.data.Result
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.testing.MainDispatcherRule
import com.detrapay.testing.getOrAwaitValueMatching
import com.detrapay.ui.state.UIState
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrdersViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val orderRepository = mockk<OrderRepository>()
    private val registrationRepository = mockk<RegistrationRepository>(relaxed = true)
    private val salesmanRepository = mockk<SalesmanRepository>(relaxed = true)

    @Test
    fun `load orders publishes every repository status newest first`() {
        val pending = TestOrderFixtures.order().copy(id = 20, status = OrderStatus.PENDING)
        val paid = TestOrderFixtures.order().copy(id = 22, status = OrderStatus.PAID)
        val cancelled = TestOrderFixtures.order().copy(id = 21, status = OrderStatus.CANCELLED)
        coEvery { orderRepository.getOrders(false) } returns
            Result.Success(listOf(pending, paid, cancelled))
        val viewModel = OrdersViewModel(
            orderRepository,
            registrationRepository,
            salesmanRepository,
        )

        viewModel.loadOrders()

        val state = viewModel.orderListState.getOrAwaitValueMatching { it is UIState.Success }
        assertEquals(listOf(22, 21, 20), (state as UIState.Success).data?.map { it.id })
    }
}
