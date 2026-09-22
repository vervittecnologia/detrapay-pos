package com.detrapay.ui.payment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult
import com.detrapay.data.Result
import com.detrapay.data.ConflictException
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.local.PendingPaymentCompletion
import com.detrapay.data.model.remote.PaymentAttempt
import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.data.repositories.PendingPaymentRepository
import com.detrapay.testing.MainDispatcherRule
import com.detrapay.testing.getOrAwaitValueMatching
import com.detrapay.ui.home.orders.OrderPaymentRequest
import com.detrapay.ui.home.orders.TestOrderFixtures
import com.detrapay.ui.state.UIState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
    private val pendingPaymentRepository = mockk<PendingPaymentRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>()
    private lateinit var operationCoordinator: PaymentOperationCoordinator
    private lateinit var viewModel: PaymentDialogViewModel

    @Before
    fun setUp() {
        operationCoordinator = PaymentOperationCoordinator()
        viewModel = PaymentDialogViewModel(
            plugPag,
            paymentRepository,
            orderRepository,
            pendingPaymentRepository,
            authRepository,
            operationCoordinator,
        )
        coEvery { authRepository.getLoggedUser(any()) } returns loggedUser()
        coEvery { pendingPaymentRepository.findForAttempt(any(), any()) } returns null
        coEvery {
            paymentRepository.saveTransaction(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } just runs
    }

    @Test
    fun `process recreation completes stored approval without charging again`() {
        coEvery { pendingPaymentRepository.listPending("user-a") } returns
            listOf(pendingCompletion())
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.resumePendingPayments()

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        assertEquals("txn-1", state.data?.transactionId)
        verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
        coVerify(exactly = 1) { pendingPaymentRepository.deleteCompleted("attempt-1") }
    }

    @Test
    fun `local audit log failure does not retry terminal`() {
        arrangePreparedOnline()
        every { plugPag.doPayment(any()) } returns approvedTransaction()
        coEvery {
            paymentRepository.saveTransaction(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } throws IOException("disk")
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(request("credito", online = true), "SER123")

        viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        verify(exactly = 1) { plugPag.doPayment(any<PlugPagPaymentData>()) }
        coVerify(exactly = 1) { pendingPaymentRepository.saveApproved(any()) }
        coVerify(exactly = 1) { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) }
        coVerifyOrder {
            pendingPaymentRepository.saveApproved(any())
            paymentRepository.saveTransaction(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
            orderRepository.recordApprovedOnlinePayment("attempt-1", any())
        }
    }

    @Test
    fun `completion conflict keeps durable pending row`() {
        arrangePreparedOnline()
        every { plugPag.doPayment(any()) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Error(ConflictException("external_transaction_conflict"))

        viewModel.payOrder(request("credito", online = true), "SER123")

        viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
        coVerify(exactly = 1) { pendingPaymentRepository.saveApproved(any()) }
        coVerify(exactly = 0) { pendingPaymentRepository.deleteCompleted("attempt-1") }
    }

    @Test
    fun `approval racing with abort is persisted and blocks a second charge`() {
        val terminalEntered = CountDownLatch(1)
        val releaseTerminal = CountDownLatch(1)
        arrangePreparedOnline()
        every { plugPag.doPayment(any()) } answers {
            terminalEntered.countDown()
            releaseTerminal.await(2, TimeUnit.SECONDS)
            approvedTransaction()
        }
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(request("credito", online = true), "SER123")
        assertTrue(terminalEntered.await(2, TimeUnit.SECONDS))
        viewModel.abortPayment()
        viewModel.payOrder(
            request("credito", online = true, idempotencyKey = "second-payment"),
            "SER123",
        )
        releaseTerminal.countDown()

        viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        verify(exactly = 1) { plugPag.doPayment(any<PlugPagPaymentData>()) }
        verify(exactly = 1) { plugPag.abort() }
        coVerify(exactly = 1) { pendingPaymentRepository.saveApproved(any()) }
        coVerify(exactly = 1) { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) }
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
    fun `A011 checks last approved transaction before allowing retry`() {
        arrangePreparedOnline()
        every { plugPag.doPayment(any()) } returns timeoutTransaction()
        every { plugPag.getLastApprovedTransaction() } returns declinedTransaction()

        viewModel.payOrder(request("pix", online = true), "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
        assertTrue(state.message.orEmpty().contains("ultima transacao foi consultada"))
        verify(exactly = 1) { plugPag.getLastApprovedTransaction() }
        coVerify(exactly = 0) { orderRepository.recordApprovedOnlinePayment(any(), any()) }
    }

    @Test
    fun `A011 recovers only the matching approved transaction`() {
        arrangePreparedOnline()
        every { plugPag.doPayment(any()) } returns timeoutTransaction()
        every { plugPag.getLastApprovedTransaction() } returns approvedTransaction(
            userReference = "PABC123456",
            amount = "000000002567",
            paymentType = PlugPag.TYPE_PIX,
        )
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(request("pix", online = true), "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        assertEquals("txn-1", state.data?.transactionId)
        verify(exactly = 1) { plugPag.doPayment(any<PlugPagPaymentData>()) }
        verify(exactly = 1) { plugPag.getLastApprovedTransaction() }
        coVerify(exactly = 1) { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) }
    }

    @Test
    fun `A011 does not recover a previous transaction with different amount`() {
        arrangePreparedOnline()
        every { plugPag.doPayment(any()) } returns timeoutTransaction()
        every { plugPag.getLastApprovedTransaction() } returns approvedTransaction(
            userReference = "PABC123456",
            amount = "000000009999",
            paymentType = PlugPag.TYPE_PIX,
        )

        viewModel.payOrder(request("pix", online = true), "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
        assertTrue(state.message.orEmpty().contains("nenhuma aprovacao deste pedido"))
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
        assertEquals("PABC123456", readString(paymentSlot.captured, "userReference"))
        coVerify(exactly = 1) { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) }
    }

    @Test
    fun `PagBank user reference uses prepared terminal reference`() {
        val paymentSlot = slot<PlugPagPaymentData>()
        arrangePreparedOnline()
        every { plugPag.doPayment(capture(paymentSlot)) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(request("pix", online = true, orderId = 1_234_567_890), "SER123")

        viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }
        assertEquals("PABC123456", readString(paymentSlot.captured, "userReference"))
    }

    @Test
    fun `prepared total mismatch never opens PlugPag`() {
        coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
            Result.Success(attempt(amountOriginal = 25.67, amountFinal = 99.99))
        coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
            Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
        every { plugPag.doPayment(any()) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(
            request("credito", online = true, installments = 3, amountFinal = 27.00),
            "SER123",
        )

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
        assertTrue(state.message.orEmpty().contains("total diferente"))
        coVerify(exactly = 0) { orderRepository.updatePaymentAttemptSplitConfig(any(), any()) }
        verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
        coVerify(exactly = 0) { orderRepository.recordApprovedOnlinePayment(any(), any()) }
    }

    @Test
    fun `confirmed amount is rounded once and used as canonical total`() {
        val terminalSlot = slot<PlugPagPaymentData>()
        val approvalSlot = slot<PaymentData>()
        coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
            Result.Success(attempt(amountOriginal = 25.67, amountFinal = 27.006))
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
    fun `invalid prepared final amount blocks confirmed payment`() {
        coEvery { orderRepository.prepareOnlinePayment(any(), any(), any(), any(), any()) } returns
            Result.Success(attempt(amountFinal = Double.NaN))
        coEvery { orderRepository.updatePaymentAttemptSplitConfig("attempt-1", "SER123") } returns
            Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
        every { plugPag.doPayment(any()) } returns approvedTransaction()
        coEvery { orderRepository.recordApprovedOnlinePayment("attempt-1", any()) } returns
            Result.Success(TestOrderFixtures.order())

        viewModel.payOrder(request("credito", online = true), "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }
        assertTrue(state.message.orEmpty().contains("total diferente"))
        coVerify(exactly = 0) { orderRepository.updatePaymentAttemptSplitConfig(any(), any()) }
        verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
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
        val durableCompletion = slot<PendingPaymentCompletion>()
        coEvery { pendingPaymentRepository.saveApproved(capture(durableCompletion)) } just runs
        coEvery { pendingPaymentRepository.findForAttempt("attempt-1", "user-a") } answers {
            if (durableCompletion.isCaptured) durableCompletion.captured else null
        }
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
            Result.Success(attempt(amountOriginal = 25.67, amountFinal = 26.40))
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
        assertEquals("PABC123456", readString(terminalSlot.captured, "userReference"))
        assertEquals(26.40, approvalSlot.captured.amountFinal ?: 0.0, 0.0)
    }

    private fun request(
        type: String,
        online: Boolean,
        installments: Int = 1,
        amountFinal: Double = 25.67,
        orderId: Int = 10,
        idempotencyKey: String = "stable-key",
    ) = OrderPaymentRequest(
        order = TestOrderFixtures.order().copy(id = orderId),
        paymentMethod = PaymentMethod(1, type, installments, 0.0, type, online),
        amount = 25.67,
        amountFinal = amountFinal,
        installments = installments,
        idempotencyKey = idempotencyKey,
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
        terminalReference = "PABC123456",
    )

    private fun pendingCompletion() = PendingPaymentCompletion(
        attemptId = "attempt-1",
        sessionUserId = "user-a",
        idempotencyKey = "stable-key",
        terminalReference = "PABC123456",
        orderId = 10,
        transactionId = "txn-1",
        transactionCode = "code-1",
        date = "28/07/2026",
        time = "10:20:30",
        result = PlugPag.RET_OK,
        paymentType = PlugPag.TYPE_CREDITO,
        installments = 1,
        cardBrand = "VISA",
        cardLast4 = "1234",
        cardHolder = "Cliente",
        pixTxIdCode = null,
        transactionLog = "{}",
        amountOriginal = 25.67,
        amountFinal = 25.67,
        status = PendingPaymentCompletion.STATUS_APPROVED,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun loggedUser() = LoggedInUser(
        id = "user-a",
        sessionToken = "token",
        displayName = "Usuario",
        username = "user",
        cpfCnpj = "12345678000190",
        email = "user@example.com",
        companies = listOf(Company(id = 37, name = "Detrapay")),
        dispatchers = listOf(Dispatcher(id = 35, name = "Despachante")),
        salesmen = emptyList(),
    )

    private fun approvedTransaction(
        userReference: String? = null,
        amount: String? = null,
        paymentType: Int? = null,
    ) = mockk<PlugPagTransactionResult> {
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
        every { this@mockk.userReference } returns userReference
        every { this@mockk.amount } returns amount
        every { this@mockk.paymentType } returns paymentType
    }

    private fun timeoutTransaction() = mockk<PlugPagTransactionResult> {
        every { result } returns -1019
        every { transactionId } returns null
        every { transactionCode } returns null
        every { date } returns "22/09/2026"
        every { time } returns "14:48:34"
        every { cardBrand } returns null
        every { holder } returns null
        every { holderName } returns null
        every { pixTxIdCode } returns null
        every { message } returns "Tempo de resposta excedido"
        every { errorCode } returns "A011"
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

    private fun readString(target: Any, vararg candidateNames: String): String? {
        val field = target.javaClass.declaredFields.firstOrNull { field ->
            candidateNames.any { it.equals(field.name, ignoreCase = true) }
        } ?: throw AssertionError("Field not found: ${target.javaClass.declaredFields.map { it.name }}")
        field.isAccessible = true
        return field.get(target) as String?
    }
}
