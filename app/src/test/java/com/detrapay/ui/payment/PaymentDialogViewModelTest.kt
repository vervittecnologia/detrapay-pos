package com.detrapay.ui.payment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult
import com.detrapay.data.Result
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.remote.PaymentAttempt
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.testing.MainDispatcherRule
import com.detrapay.testing.getOrAwaitValueMatching
import com.detrapay.ui.home.orders.OrderPaymentRequest
import com.detrapay.ui.home.orders.TestOrderFixtures
import com.detrapay.ui.state.UIState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PaymentDialogViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val plugPag = mockk<IPlugPagWrapper>(relaxed = true)
    private val paymentRepository = mockk<PaymentRepository>()
    private val orderRepository = mockk<OrderRepository>()
    private lateinit var viewModel: PaymentDialogViewModel

    @Before
    fun setUp() {
        viewModel = PaymentDialogViewModel(plugPag, paymentRepository, orderRepository)
        coEvery {
            paymentRepository.saveTransaction(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } just runs
    }

    @Test
    fun `init configures PagBank listener and print layout`() {
        viewModel.init()

        verify(exactly = 1) { plugPag.setEventListener(viewModel) }
        verify(exactly = 1) { plugPag.setPlugPagCustomPrinterLayout(any()) }
    }

    @Test
    fun `record only request never prepares attempt or invokes PagBank`() {
        viewModel.payOrder(request("pix_manual", online = false), "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }

        assertEquals("Este pagamento deve ser apenas registrado.", state.message)
        coVerify(exactly = 0) { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
    }

    @Test
    fun `split failure does not invoke PagBank or persist a payment`() {
        val request = request("credito", online = true)
        coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
            Result.Success(attempt())
        coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
            Result.Error(Exception("split failure"))

        viewModel.payOrder(request, "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
        assertTrue(state.message?.contains("split failure") == true)
        verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
        coVerify(exactly = 0) { orderRepository.recordApprovedOnlinePayment(any(), any()) }
    }

    @Test
    fun `declined transaction never records an order payment`() {
        arrangePreparedOnline()
        every { plugPag.doPayment(any()) } returns declinedTransaction()

        viewModel.payOrder(request("credito", online = true), "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
        assertEquals("DECLINED - Operacao negada", state.message)
        coVerify(exactly = 0) { orderRepository.recordApprovedOnlinePayment(any(), any()) }
    }

    @Test
    fun `approved credit records exactly once after PagBank success`() {
        val paymentSlot = slot<PlugPagPaymentData>()
        arrangePreparedOnline()
        every { plugPag.doPayment(capture(paymentSlot)) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(request("credito", online = true, installments = 3), "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        assertTrue(state is UIState.Success)
        assertEquals(PlugPag.TYPE_CREDITO, readInt(paymentSlot.captured, "paymentType", "type"))
        assertEquals(3, readInt(paymentSlot.captured, "installments"))
        coVerify(exactly = 1) { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) }
    }

    @Test
    fun `confirmed total wins when backend prepared total differs`() {
        val terminalSlot = slot<PlugPagPaymentData>()
        val approvalSlot = slot<PaymentData>()
        coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
            Result.Success(attempt(amountOriginal = 25.67, amountFinal = 99.99))
        coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
            Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
        every { plugPag.doPayment(capture(terminalSlot)) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", capture(approvalSlot)) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(
            request("credito", online = true, installments = 3, amountFinal = 27.00),
            "SER123",
        )

        viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        assertEquals(2700, readInt(terminalSlot.captured, "amount"))
        assertEquals(25.67, approvalSlot.captured.amountOriginal ?: 0.0, 0.0)
        assertEquals(27.00, approvalSlot.captured.amountFinal ?: 0.0, 0.0)
        coVerify(exactly = 1) {
            paymentRepository.saveTransaction(
                orderId = 10,
                amount = 27.00,
                installments = 3,
                paymentType = "credito",
                transactionId = any(),
                transactionCode = any(),
                date = any(),
                result = PlugPag.RET_OK,
                cardBrand = any(),
                cardLast4 = any(),
                cardHolder = any(),
                pixTxIdCode = any(),
                message = any(),
                errorCode = any(),
            )
        }
    }

    @Test
    fun `confirmed amount is rounded once and used as canonical total`() {
        val terminalSlot = slot<PlugPagPaymentData>()
        val approvalSlot = slot<PaymentData>()
        coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
            Result.Success(attempt(amountOriginal = 25.67, amountFinal = 99.99))
        coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
            Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
        every { plugPag.doPayment(capture(terminalSlot)) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", capture(approvalSlot)) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(
            request("credito", online = true, installments = 3, amountFinal = 27.006),
            "SER123",
        )

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        assertEquals(27.01, state.data?.amountFinal ?: 0.0, 0.0)
        assertEquals(27.01, approvalSlot.captured.amountFinal ?: 0.0, 0.0)
        assertEquals(2701, readInt(terminalSlot.captured, "amount"))
        assertEquals(
            PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR,
            readInt(terminalSlot.captured, "installmentType"),
        )
    }

    @Test
    fun `debit charges the confirmed final amount`() {
        assertConfirmedOnlineAmount(type = "debito", expectedPaymentType = PlugPag.TYPE_DEBITO)
    }

    @Test
    fun `pix charges the confirmed final amount`() {
        assertConfirmedOnlineAmount(type = "pix", expectedPaymentType = PlugPag.TYPE_PIX)
    }

    @Test
    fun `invalid prepared final amount does not block confirmed payment`() {
        coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
            Result.Success(attempt(amountFinal = Double.NaN))
        coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
            Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
        every { plugPag.doPayment(any()) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(request("credito", online = true), "SER123")

        viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        verify(exactly = 1) { plugPag.doPayment(any<PlugPagPaymentData>()) }
    }

    @Test
    fun `invalid confirmed final amount never opens PagBank`() {
        viewModel.payOrder(
            request("credito", online = true, amountFinal = Double.NaN),
            "SER123",
        )

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
        assertEquals("O valor confirmado para o pagamento e invalido.", state.message)
        coVerify(exactly = 0) { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
    }

    @Test
    fun `retry after approved persistence failure does not charge again`() {
        val request = request("credito", online = true)
        arrangePreparedOnline()
        every { plugPag.doPayment(any()) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returnsMany listOf(
            Result.Error(Exception("temporarily unavailable")),
            Result.Success(TestOrderFixtures.order()),
        )

        viewModel.payOrder(request, "SER123")
        viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
        viewModel.payOrder(request, "SER123")
        viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }

        verify(exactly = 1) { plugPag.doPayment(any<PlugPagPaymentData>()) }
        coVerify(exactly = 2) { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) }
    }

    private fun arrangePreparedOnline() {
        coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
            Result.Success(attempt())
        coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
            Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
    }

    private fun assertConfirmedOnlineAmount(type: String, expectedPaymentType: Int) {
        val terminalSlot = slot<PlugPagPaymentData>()
        val approvalSlot = slot<PaymentData>()
        coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
            Result.Success(attempt(amountOriginal = 25.67, amountFinal = 99.99))
        coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
            Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
        every { plugPag.doPayment(capture(terminalSlot)) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", capture(approvalSlot)) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(request(type, online = true, amountFinal = 26.40), "SER123")

        viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        assertEquals(expectedPaymentType, readInt(terminalSlot.captured, "paymentType", "type"))
        assertEquals(2640, readInt(terminalSlot.captured, "amount"))
        assertEquals(26.40, approvalSlot.captured.amountFinal ?: 0.0, 0.0)
    }

    private fun request(
        type: String,
        online: Boolean,
        installments: Int = 1,
        amountFinal: Double = 25.67,
    ) = OrderPaymentRequest(
        order = TestOrderFixtures.order(),
        paymentMethod = PaymentMethod(1, type, installments, 0.0, type, online),
        amount = 25.67,
        amountFinal = amountFinal,
        installments = installments,
        idempotencyKey = "stable-key",
    )

    private fun attempt(
        amountOriginal: Double = 25.67,
        amountFinal: Double = 25.67,
    ) = PaymentAttempt(
        id = "attempt-1",
        status = "prepared",
        orderId = 10,
        paymentMethodId = 1,
        amountOriginal = amountOriginal,
        amountFinal = amountFinal,
        installments = 3,
        expiresAt = "2026-07-29T03:30:00Z",
    )

    private fun approvedTransaction() = mockk<PlugPagTransactionResult> {
        every { result } returns PlugPag.RET_OK
        every { transactionId } returns "txn-1"
        every { transactionCode } returns "code-1"
        every { date } returns "28/07/2026"
        every { time } returns "10:20:30"
        every { cardBrand } returns "VISA"
        every { holder } returns "1234"
        every { holderName } returns "Cliente"
        every { pixTxIdCode } returns null
        every { message } returns "OK"
        every { errorCode } returns null
    }

    private fun declinedTransaction() = mockk<PlugPagTransactionResult> {
        every { result } returns 5
        every { transactionId } returns null
        every { transactionCode } returns null
        every { date } returns "28/07/2026"
        every { time } returns "10:20:30"
        every { cardBrand } returns "VISA"
        every { holder } returns "1234"
        every { holderName } returns "Cliente"
        every { pixTxIdCode } returns null
        every { message } returns "Operacao negada"
        every { errorCode } returns "DECLINED"
    }

    private fun readInt(target: Any, vararg candidateNames: String): Int {
        val field = target.javaClass.declaredFields.firstOrNull { field ->
            candidateNames.any { it.equals(field.name, ignoreCase = true) }
        } ?: throw AssertionError("Field not found: ${target.javaClass.declaredFields.map { it.name }}")
        field.isAccessible = true
        return field.get(target) as Int
    }
}
