package com.detrapay.ui.home.orders

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.detrapay.data.Result
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.OrderDocument
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.testing.MainDispatcherRule
import com.detrapay.testing.getOrAwaitValueMatching
import com.detrapay.ui.state.UIState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

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

    @Test
    fun `delete offline payment publishes updated order`() {
        val receivable = TestOrderFixtures.receivable("cash")
        val updatedOrder = TestOrderFixtures.order().copy(receivables = emptyList())
        coEvery { orderRepository.cancelPendingReceivable(10, receivable) } returns
            Result.Success(updatedOrder)
        val viewModel = OrdersViewModel(
            orderRepository,
            registrationRepository,
            salesmanRepository,
        )

        viewModel.deleteOfflinePayment(orderId = 10, receivable = receivable)

        val state = viewModel.deletePaymentState.getOrAwaitValueMatching { it is UIState.Success }
        assertEquals(updatedOrder, state.data)
        coVerify(exactly = 1) { orderRepository.cancelPendingReceivable(10, receivable) }
    }

    @Test
    fun `delete online payment is rejected before repository call`() {
        val receivable = TestOrderFixtures.receivable("pix")
        val viewModel = OrdersViewModel(
            orderRepository,
            registrationRepository,
            salesmanRepository,
        )

        viewModel.deleteOfflinePayment(orderId = 10, receivable = receivable)

        val state = viewModel.deletePaymentState.getOrAwaitValueMatching { it is UIState.Error }
        assertTrue(state.message?.contains("nao pode ser excluido") == true)
        coVerify(exactly = 0) { orderRepository.cancelPendingReceivable(any(), any()) }
    }

    @Test
    fun `captured photo is uploaded and added to order documents`() {
        val photo = File.createTempFile("order-photo", ".jpg").apply { writeText("jpeg") }
        val document = OrderDocument(
            id = 91,
            salesOrderId = 10,
            fileName = photo.name,
            fileUrl = "uploads/${photo.name}",
            previewUrl = "https://signed.example/${photo.name}",
            mimeType = "image/jpeg",
            fileSizeKb = 1,
            createdAt = "2026-07-29T12:00:00Z",
        )
        coEvery { orderRepository.uploadOrderDocument(10, photo) } returns Result.Success(document)
        val viewModel = OrdersViewModel(
            orderRepository,
            registrationRepository,
            salesmanRepository,
        )

        viewModel.uploadOrderPhoto(10, photo)

        val state = viewModel.orderDocumentsState.getOrAwaitValueMatching {
            it.documents.any { item -> item.id == document.id } && !it.isUploading
        }
        assertEquals(listOf(document), state.documents)
        assertTrue(state.pendingPhotoPath == null)
        assertTrue(!photo.exists())
        coVerify(exactly = 1) { orderRepository.uploadOrderDocument(10, photo) }
    }

    @Test
    fun `failed photo upload keeps captured file available for retry`() {
        val photo = File.createTempFile("order-photo", ".jpg").apply { writeText("jpeg") }
        coEvery { orderRepository.uploadOrderDocument(10, photo) } returns
            Result.Error(Exception("API de documentos indisponivel"))
        val viewModel = OrdersViewModel(
            orderRepository,
            registrationRepository,
            salesmanRepository,
        )

        viewModel.uploadOrderPhoto(10, photo)

        val state = viewModel.orderDocumentsState.getOrAwaitValueMatching {
            it.errorMessage?.contains("indisponivel") == true && !it.isUploading
        }
        assertEquals(photo.absolutePath, state.pendingPhotoPath)
        assertTrue(photo.exists())

        viewModel.discardPendingOrderPhoto()
        val discarded = viewModel.orderDocumentsState.getOrAwaitValueMatching {
            it.pendingPhotoPath == null && it.errorMessage == null
        }
        assertTrue(discarded.documents.isEmpty())
        assertTrue(!photo.exists())
    }
}
