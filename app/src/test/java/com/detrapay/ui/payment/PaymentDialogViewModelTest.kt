package com.detrapay.ui.payment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult
import com.detrapay.data.Result
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.PixCharge
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.testing.MainDispatcherRule
import com.detrapay.testing.getOrAwaitValueMatching
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

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val plugPag = mockk<IPlugPagWrapper>(relaxed = true)
    private val paymentRepository = mockk<PaymentRepository>()
    private val orderRepository = mockk<OrderRepository>()

    private lateinit var viewModel: PaymentDialogViewModel

    @Before
    fun setUp() {
        viewModel = PaymentDialogViewModel(
            plugPag = plugPag,
            paymentRepository = paymentRepository,
            orderRepository = orderRepository,
        )
        coEvery {
            paymentRepository.saveTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } just runs
    }

    @Test
    fun `init configures maquininha listener and print layout`() {
        viewModel.init()

        verify(exactly = 1) { plugPag.setEventListener(viewModel) }
        verify(exactly = 1) { plugPag.setPlugPagCustomPrinterLayout(any()) }
    }

    @Test
    fun `payOrder fails when split config update fails`() {
        coEvery { orderRepository.updateSplitConfig(1, "SER123") } returns Result.Error(Exception("split failure"))

        viewModel.payOrder(orderId = 10, receivable = receivable(), serial = "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }

        assertTrue(state is UIState.Error)
        assertTrue(state.message?.contains("split failure") == true)
        verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
    }

    @Test
    fun `payOrder fails when maquininha is not authenticated`() {
        coEvery { orderRepository.updateSplitConfig(1, "SER123") } returns Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns false

        viewModel.payOrder(orderId = 10, receivable = receivable(), serial = "SER123")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }

        assertTrue(state is UIState.Error)
        assertEquals("Nenhum usuario autenticado, contate o suporte.", state.message)
    }

    @Test
    fun `payOrder sends rounded amount to PlugPag and confirms order on success`() {
        val paymentDataSlot = slot<PlugPagPaymentData>()
        val transactionResult = successfulTransactionResult()

        coEvery { orderRepository.updateSplitConfig(1, "SER123") } returns Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
        every { plugPag.doPayment(capture(paymentDataSlot)) } returns transactionResult
        coEvery { orderRepository.payOrder(10, any(), any()) } returns Result.Success(mockk(relaxed = true))

        viewModel.payOrder(
            orderId = 10,
            receivable = receivable(
                amountFinal = 25.679,
                installments = 3,
                paymentMethod = creditMethod(name = "VISA", installments = 3)
            ),
            serial = "SER123"
        )

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }

        assertTrue(state is UIState.Success)
        assertEquals(2568, readInt(paymentDataSlot.captured, "amount"))
        assertEquals(3, readInt(paymentDataSlot.captured, "installments"))
        assertEquals(PlugPag.TYPE_CREDITO, readInt(paymentDataSlot.captured, "paymentType", "type"))
        assertEquals(PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR, readInt(paymentDataSlot.captured, "installmentType"))
        coVerify(exactly = 1) { paymentRepository.saveTransaction(orderId = 10, amount = 25.679, installments = 3, paymentType = "VISA", transactionId = any(), transactionCode = any(), date = any(), result = PlugPag.RET_OK, cardBrand = any(), cardLast4 = any(), cardHolder = any(), pixTxIdCode = any(), message = any(), errorCode = any()) }
        coVerify(exactly = 1) { orderRepository.payOrder(10, any(), any()) }
    }

    @Test
    fun `payOrder persists failure when transaction is declined`() {
        val transactionResult = mockk<PlugPagTransactionResult> {
            every { result } returns 5
            every { errorCode } returns "DECLINED"
            every { message } returns "Operacao negada"
            every { transactionId } returns null
            every { transactionCode } returns null
            every { date } returns "2026-03-06"
            every { cardBrand } returns "VISA"
            every { holder } returns "1234"
            every { holderName } returns "Cliente"
            every { pixTxIdCode } returns null
        }

        coEvery { orderRepository.updateSplitConfig(1, "SER123") } returns Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
        every { plugPag.doPayment(any()) } returns transactionResult

        viewModel.payOrder(
            orderId = 10,
            receivable = receivable(paymentMethod = creditMethod(name = "VISA")),
            serial = "SER123"
        )

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }

        assertTrue(state is UIState.Error)
        assertEquals("DECLINED - Operacao negada", state.message)
        coVerify(exactly = 1) { paymentRepository.saveTransaction(orderId = 10, amount = 25.67, installments = 1, paymentType = "VISA", transactionId = null, transactionCode = null, date = "2026-03-06", result = 5, cardBrand = "VISA", cardLast4 = "1234", cardHolder = "Cliente", pixTxIdCode = null, message = "Operacao negada", errorCode = "DECLINED") }
        coVerify(exactly = 0) { orderRepository.payOrder(any(), any(), any()) }
    }

    @Test
    fun `payOrder generates pix qr without using PlugPag`() {
        val pixReceivable = receivable(
            paymentMethod = PaymentMethod(
                id = 7,
                name = "Pix",
                installments = 1,
                interestTax = 0.0,
                paymentType = "pix",
            )
        )

        coEvery { orderRepository.generatePixCharge(pixReceivable) } returns Result.Success(
            PixCharge(
                qrCodeContent = "0002012633pix.example/abc",
                copyPasteCode = "0002012633pix.example/abc",
                qrCodeBase64 = null,
                txId = "pix-123",
                expiresAt = "2026-03-06T18:30:00"
            )
        )

        viewModel.payOrder(orderId = 10, receivable = pixReceivable, serial = "IGNORED")

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Success<*> }

        assertTrue(state is UIState.Success)
        assertTrue((state as UIState.Success).data?.pendingConfirmation == true)
        verify(exactly = 0) { plugPag.doPayment(any<PlugPagPaymentData>()) }
        coVerify(exactly = 0) { orderRepository.updateSplitConfig(any(), any()) }
        coVerify(exactly = 1) { orderRepository.generatePixCharge(pixReceivable) }
    }

    @Test
    fun `payOrder surfaces backend confirmation failure after approved card transaction`() {
        val transactionResult = successfulTransactionResult()
        val paymentDataSlot = slot<PaymentData>()

        coEvery { orderRepository.updateSplitConfig(1, "SER123") } returns Result.Success(Unit)
        every { plugPag.isAuthenticated() } returns true
        every { plugPag.doPayment(any()) } returns transactionResult
        coEvery { orderRepository.payOrder(10, any(), capture(paymentDataSlot)) } returns Result.Error(Exception("Erro ao confirmar pagamento no servidor"))

        viewModel.payOrder(
            orderId = 10,
            receivable = receivable(paymentMethod = creditMethod(name = "VISA")),
            serial = "SER123"
        )

        val state = viewModel.paymentState.getOrAwaitValueMatching { it is UIState.Error<*> }

        assertTrue(state is UIState.Error)
        assertEquals("Erro ao confirmar pagamento no servidor", state.message)
        assertEquals("txn-1", paymentDataSlot.captured.transactionId)
        assertEquals("code-1", paymentDataSlot.captured.transactionCode)
        assertEquals("06/03/2026", paymentDataSlot.captured.date)
        assertEquals("10:20:30", paymentDataSlot.captured.time)
        assertEquals("VISA", paymentDataSlot.captured.cardBrand)
        assertEquals("1234", paymentDataSlot.captured.cardLast4)
        assertEquals("Cliente", paymentDataSlot.captured.cardHolder)
        assertTrue(paymentDataSlot.captured.transactionLog?.contains("txn-1") == true)
        coVerify(exactly = 1) { paymentRepository.saveTransaction(orderId = 10, amount = 25.67, installments = 1, paymentType = "VISA", transactionId = "txn-1", transactionCode = "code-1", date = "06/03/2026", result = PlugPag.RET_OK, cardBrand = "VISA", cardLast4 = "1234", cardHolder = "Cliente", pixTxIdCode = null, message = "OK", errorCode = null) }
        coVerify(exactly = 1) { orderRepository.payOrder(10, any(), any()) }
    }

    private fun creditMethod(
        name: String = "VISA",
        installments: Int = 1
    ) = PaymentMethod(
        id = 9,
        name = name,
        installments = installments,
        interestTax = 0.0,
        paymentType = "credito",
    )

    private fun receivable(
        amountFinal: Double = 25.67,
        installments: Int = 1,
        paymentMethod: PaymentMethod = creditMethod(installments = installments),
    ) = OrderReceivableItem(
        id = 1,
        documentId = "doc-1",
        amountOriginal = 25.67,
        amountFinal = amountFinal,
        installments = installments,
        status = OrderReceivableItemStatus.PENDING,
        paymentMethod = paymentMethod,
        paymentDate = null,
        refundDate = null,
        cardLast4 = null,
        cardHolder = null,
        tax = null,
        cardBrand = null,
        authorizationId = null,
        authorizationCode = null,
        pixTxIdCode = null,
    )

    private fun successfulTransactionResult() = mockk<PlugPagTransactionResult> {
        every { result } returns PlugPag.RET_OK
        every { transactionId } returns "txn-1"
        every { transactionCode } returns "code-1"
        every { date } returns "06/03/2026"
        every { time } returns "10:20:30"
        every { cardBrand } returns "VISA"
        every { holder } returns "1234"
        every { holderName } returns "Cliente"
        every { pixTxIdCode } returns null
        every { message } returns "OK"
        every { errorCode } returns null
    }

    private fun readInt(target: Any, vararg candidateNames: String): Int {
        val field = target.javaClass.declaredFields.firstOrNull { field ->
            candidateNames.any { it.equals(field.name, ignoreCase = true) }
        } ?: throw AssertionError("Field not found. Available fields: ${target.javaClass.declaredFields.map { it.name }}")

        field.isAccessible = true
        return field.get(target) as Int
    }
}
