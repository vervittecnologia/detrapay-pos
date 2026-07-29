package com.detrapay.ui.home.orders

import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderCustomer
import com.detrapay.data.model.OrderItem
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.VehicleType
import com.detrapay.data.model.remote.InstallmentFee
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderFlowReducerTest {

    @Test
    fun `start payment opens method before amount`() {
        val order = order(total = 2570.18, paidAmount = 1200.0)

        val state = OrderFlowReducer.startPayment(OrderFlowLocalState(), order)

        assertEquals(order, state.selectedOrder)
        assertEquals(OrderFlowStep.Method, state.step)
        assertEquals("", state.paymentDigits)
        assertNull(state.selectedPaymentMethod)
        assertNull(state.selectedInstallment)
        assertFalse(state.showSimulator)
    }

    @Test
    fun `pending amount fills digits only after explicit shortcut`() {
        val order = order(total = 2570.18, paidAmount = 1200.0)
        val started = OrderFlowReducer.startPayment(OrderFlowLocalState(), order)

        val filled = OrderFlowReducer.usePendingAmount(started, order)

        assertEquals("137018", filled.paymentDigits)
    }

    @Test
    fun `showDetail selects order and opens detail`() {
        val order = order()

        val state = OrderFlowReducer.showDetail(OrderFlowLocalState(), order)

        assertEquals(order, state.selectedOrder)
        assertEquals(OrderFlowStep.Detail, state.step)
    }

    @Test
    fun `payment key delegates to presentation rules`() {
        val state = OrderFlowLocalState(paymentDigits = "12")

        val next = OrderFlowReducer.applyPaymentKey(state, "DEL")

        assertEquals("1", next.paymentDigits)
    }

    @Test
    fun `open methods moves to method`() {
        val state = OrderFlowLocalState(step = OrderFlowStep.Detail)

        val next = OrderFlowReducer.openMethods(state)

        assertEquals(OrderFlowStep.Method, next.step)
    }

    @Test
    fun `selecting payment method opens amount and clears dependent quote`() {
        val state = OrderFlowLocalState(
            paymentDigits = "10000",
            selectedInstallment = 4,
            creditInstallments = listOf(installmentFee()),
        )

        val next = OrderFlowReducer.selectPaymentMethod(state, paymentMethod("credito", true))

        assertEquals("credito", next.selectedPaymentMethod?.paymentType)
        assertNull(next.selectedInstallment)
        assertEquals(OrderFlowStep.Amount, next.step)
        assertEquals("", next.paymentDigits)
        assertTrue(next.creditInstallments.isEmpty())
        assertNull(next.paymentReview)
        assertNull(next.feeRequestTarget)
    }

    @Test
    fun `select debit opens amount`() {
        val state = OrderFlowLocalState()

        val next = OrderFlowReducer.selectPaymentMethod(state, paymentMethod("debito", true))

        assertEquals("debito", next.selectedPaymentMethod?.paymentType)
        assertEquals(OrderFlowStep.Amount, next.step)
    }

    @Test
    fun `select online pix opens amount`() {
        val state = OrderFlowLocalState()

        val next = OrderFlowReducer.selectPaymentMethod(state, paymentMethod("pix", true))

        assertEquals("pix", next.selectedPaymentMethod?.paymentType)
        assertEquals(OrderFlowStep.Amount, next.step)
    }

    @Test
    fun `select record only method opens amount`() {
        val next = OrderFlowReducer.selectPaymentMethod(
            OrderFlowLocalState(),
            paymentMethod("pix_manual", false),
        )

        assertEquals(OrderFlowStep.Amount, next.step)
    }

    @Test
    fun `credit quote opens installments and simple quote opens review`() {
        val credit = OrderFlowReducer.quoteLoaded(
            OrderFlowReducer.startCheckoutQuote(
                OrderFlowLocalState(
                    step = OrderFlowStep.Amount,
                    paymentDigits = "10000",
                    selectedPaymentMethod = paymentMethod("credito", true),
                ),
            ),
            listOf(installmentFee()),
            "No installments",
        )
        assertEquals(OrderFlowStep.Installments, credit.step)
        assertNull(credit.selectedInstallment)
        assertEquals(OrderFlowStep.Installments, OrderFlowReducer.openInstallmentReview(credit).step)

        val pix = OrderFlowReducer.quoteLoaded(
            OrderFlowReducer.startCheckoutQuote(
                OrderFlowLocalState(
                    step = OrderFlowStep.Amount,
                    paymentDigits = "10000",
                    selectedPaymentMethod = paymentMethod("pix", true),
                ),
            ),
            listOf(installmentFee()),
            "No installments",
        )
        assertEquals(OrderFlowStep.Review, pix.step)
        assertEquals(100.0, pix.paymentReview?.amountFinal ?: 0.0, 0.0)
    }

    @Test
    fun `credit quote opens installments immediately while loading`() {
        val loading = OrderFlowReducer.startCheckoutQuote(
            OrderFlowLocalState(
                step = OrderFlowStep.Amount,
                paymentDigits = "10000",
                selectedPaymentMethod = paymentMethod("credito", true),
            ),
        )

        assertEquals(OrderFlowStep.Installments, loading.step)
        assertTrue(loading.feesLoading)
        assertTrue(loading.creditInstallments.isEmpty())
    }

    @Test
    fun `direct payment opens zero fee review`() {
        val state = OrderFlowLocalState(
            step = OrderFlowStep.Amount,
            selectedPaymentMethod = paymentMethod("cash", false),
            paymentDigits = "2567",
        )

        val next = OrderFlowReducer.openDirectReview(state)

        assertEquals(OrderFlowStep.Review, next.step)
        assertEquals(25.67, next.paymentReview?.amountFinal ?: 0.0, 0.0)
        assertEquals(0.0, next.paymentReview?.feeAmount ?: -1.0, 0.0)
    }

    @Test
    fun `selected credit installment opens matching review`() {
        val state = OrderFlowLocalState(
            step = OrderFlowStep.Installments,
            paymentDigits = "10000",
            selectedPaymentMethod = paymentMethod("credito", true),
            selectedInstallment = 2,
            creditInstallments = listOf(installmentFee()),
        )

        val next = OrderFlowReducer.openInstallmentReview(state)

        assertEquals(OrderFlowStep.Review, next.step)
        assertEquals(2, next.paymentReview?.installments)
    }

    @Test
    fun `back follows current order flow step order`() {
        assertEquals(
            OrderFlowStep.Orders,
            OrderFlowReducer.back(OrderFlowLocalState(step = OrderFlowStep.Detail)).step,
        )
        assertEquals(
            OrderFlowStep.Orders,
            OrderFlowReducer.back(OrderFlowLocalState(step = OrderFlowStep.Method)).step,
        )
        assertEquals(
            OrderFlowStep.Method,
            OrderFlowReducer.back(OrderFlowLocalState(step = OrderFlowStep.Amount)).step,
        )
        assertEquals(
            OrderFlowStep.Amount,
            OrderFlowReducer.back(OrderFlowLocalState(step = OrderFlowStep.Installments)).step,
        )
        assertEquals(
            OrderFlowStep.Installments,
            OrderFlowReducer.back(
                OrderFlowLocalState(
                    step = OrderFlowStep.Review,
                    selectedPaymentMethod = paymentMethod("credito", true),
                ),
            ).step,
        )
        assertEquals(
            OrderFlowStep.Review,
            OrderFlowReducer.back(OrderFlowLocalState(step = OrderFlowStep.Waiting)).step,
        )
    }

    @Test
    fun `simulator state opens closes and updates amount`() {
        val opened = OrderFlowReducer.openSimulator(
            OrderFlowLocalState(simulatorAmountDigits = "123", simulatorSelectedInstallment = 2),
        )

        assertEquals(true, opened.showSimulator)
        assertEquals("", opened.simulatorAmountDigits)
        assertNull(opened.simulatorSelectedInstallment)

        val updated = OrderFlowReducer.updateSimulatorAmount(opened, "R$ 45,67")

        assertEquals("4567", updated.simulatorAmountDigits)
        assertEquals(emptyList<Any>(), updated.simulatorInstallments)
        assertNull(updated.simulatorSelectedInstallment)

        val closed = OrderFlowReducer.closeSimulator(updated)

        assertFalse(closed.showSimulator)
        assertNull(closed.feeRequestTarget)
    }

    @Test
    fun `simulator fee request is marked and cleared when response is consumed`() {
        val loading = OrderFlowReducer.startSimulatorLoading(OrderFlowLocalState())

        assertEquals(OrderFeeRequestTarget.Simulator, loading.feeRequestTarget)

        val loaded = OrderFlowReducer.simulatorLoaded(loading, emptyList(), "No installments")

        assertNull(loaded.feeRequestTarget)
    }

    @Test
    fun `checkout credit fee request is cleared when response fails`() {
        val requested = OrderFlowReducer.startCheckoutQuote(
            OrderFlowReducer.selectPaymentMethod(
                OrderFlowLocalState(),
                paymentMethod("credito", true),
            ),
        )

        val failed = OrderFlowReducer.feesFailed(requested, "Unable to load installments")

        assertNull(failed.feeRequestTarget)
    }

    @Test
    fun `closing simulator discards its late fee response`() {
        val loading = OrderFlowReducer.startSimulatorLoading(OrderFlowLocalState(showSimulator = true))
        val closed = OrderFlowReducer.closeSimulator(loading)

        val staleResponse = OrderFlowReducer.simulatorLoaded(closed, emptyList(), "No installments")

        assertFalse(staleResponse.showSimulator)
        assertFalse(staleResponse.simulatorLoading)
        assertNull(staleResponse.feeRequestTarget)
        assertTrue(closed.feeRequestInFlight)
        assertFalse(staleResponse.feeRequestInFlight)
    }

    @Test
    fun `changing simulator amount discards its late fee error`() {
        val loading = OrderFlowReducer.startSimulatorLoading(OrderFlowLocalState(showSimulator = true))
        val changed = OrderFlowReducer.updateSimulatorAmount(loading, "R$ 45,67")

        val staleResponse = OrderFlowReducer.simulatorFailed(changed, "Unable to load installments")

        assertEquals("4567", staleResponse.simulatorAmountDigits)
        assertFalse(staleResponse.simulatorLoading)
        assertNull(staleResponse.feeRequestTarget)
        assertNull(staleResponse.simulatorError)
        assertTrue(changed.feeRequestInFlight)
        assertFalse(staleResponse.feeRequestInFlight)
    }

    @Test
    fun `simulator request is blocked while checkout credit fees are loading`() {
        val checkoutLoading = OrderFlowReducer.startCheckoutQuote(
            OrderFlowReducer.selectPaymentMethod(
                OrderFlowLocalState(),
                paymentMethod("credito", true),
            ),
        )

        val blocked = OrderFlowReducer.startSimulatorLoading(checkoutLoading)

        assertEquals(OrderFeeRequestTarget.CheckoutCredit, blocked.feeRequestTarget)
        assertTrue(blocked.feesLoading)
        assertFalse(blocked.simulatorLoading)
        assertEquals(
            OrderFlowReducer.SIMULATOR_REQUEST_BLOCKED_MESSAGE,
            blocked.simulatorError,
        )
    }

    @Test
    fun `checkout credit request is blocked while simulator fees are loading`() {
        val simulatorLoading = OrderFlowReducer.startSimulatorLoading(OrderFlowLocalState())

        val blocked = OrderFlowReducer.startCheckoutQuote(
            OrderFlowReducer.selectPaymentMethod(
                simulatorLoading,
                paymentMethod("credito", true),
            ),
        )

        assertEquals(OrderFeeRequestTarget.Simulator, blocked.feeRequestTarget)
        assertTrue(blocked.simulatorLoading)
        assertFalse(blocked.feesLoading)
        assertEquals(
            OrderFlowReducer.CHECKOUT_REQUEST_BLOCKED_MESSAGE,
            blocked.feesError,
        )
    }

    @Test
    fun `cancelling checkout fee request keeps it in flight and blocks simulator request`() {
        val requested = OrderFlowReducer.startCheckoutQuote(
            OrderFlowReducer.selectPaymentMethod(
                OrderFlowLocalState(),
                paymentMethod("credito", true),
            ),
        )
        val cancelled = OrderFlowReducer.back(requested)

        assertTrue(cancelled.feeRequestInFlight)
        assertNull(cancelled.feeRequestTarget)
        assertFalse(cancelled.feesLoading)

        val blocked = OrderFlowReducer.startSimulatorLoading(cancelled)

        assertTrue(blocked.feeRequestInFlight)
        assertNull(blocked.feeRequestTarget)
        assertFalse(blocked.simulatorLoading)
        assertEquals(OrderFlowReducer.SIMULATOR_REQUEST_BLOCKED_MESSAGE, blocked.simulatorError)
    }

    @Test
    fun `stale checkout fee success clears in flight without applying installments`() {
        val cancelled = OrderFlowReducer.back(
            OrderFlowReducer.startCheckoutQuote(
                OrderFlowReducer.selectPaymentMethod(
                    OrderFlowLocalState(),
                    paymentMethod("credito", true),
                ),
            ),
        )

        val consumed = OrderFlowReducer.feesLoaded(
            cancelled,
            installments = listOf(installmentFee()),
            emptyMessage = "No installments",
        )

        assertFalse(consumed.feeRequestInFlight)
        assertNull(consumed.feeRequestTarget)
        assertTrue(consumed.creditInstallments.isEmpty())
        assertNull(consumed.selectedInstallment)
    }

    @Test
    fun `exit payment returns to selected order detail and clears wizard data`() {
        val selectedOrder = order()
        val state = OrderFlowLocalState(
            step = OrderFlowStep.Installments,
            selectedOrder = selectedOrder,
            paymentDigits = "10000",
            selectedPaymentMethod = paymentMethod("credito", true),
            selectedInstallment = 2,
            creditInstallments = listOf(installmentFee()),
            feesLoading = true,
            feeRequestTarget = OrderFeeRequestTarget.CheckoutCredit,
        )

        val exited = OrderFlowReducer.exitPayment(state)

        assertEquals(OrderFlowStep.Detail, exited.step)
        assertEquals(selectedOrder, exited.selectedOrder)
        assertEquals("", exited.paymentDigits)
        assertNull(exited.selectedPaymentMethod)
        assertNull(exited.selectedInstallment)
        assertTrue(exited.creditInstallments.isEmpty())
        assertFalse(exited.feesLoading)
        assertNull(exited.feeRequestTarget)
    }

    @Test
    fun `amount cannot change while checkout quote is loading`() {
        val order = order(total = 200.0)
        val loading = OrderFlowReducer.startCheckoutQuote(
            OrderFlowLocalState(
                step = OrderFlowStep.Amount,
                paymentDigits = "10000",
                selectedPaymentMethod = paymentMethod("pix", true),
            ),
        )

        assertEquals("10000", OrderFlowReducer.applyPaymentKey(loading, "9").paymentDigits)
        assertEquals("10000", OrderFlowReducer.usePendingAmount(loading, order).paymentDigits)
    }

    @Test
    fun `stale fee error without target is detected for session expiration routing`() {
        val cancelled = OrderFlowReducer.back(
            OrderFlowReducer.startCheckoutQuote(
                OrderFlowReducer.selectPaymentMethod(
                    OrderFlowLocalState(),
                    paymentMethod("credito", true),
                ),
            ),
        )

        assertTrue(OrderFlowReducer.isStaleFeeResponse(cancelled))
    }

    private fun installmentFee() = InstallmentFee(
        installmentNumber = 2,
        installmentValue = "50.00",
        totalValue = "100.00",
        interestValue = "0.00",
        noInterest = true,
    )

    private fun order(
        total: Double = 100.0,
        paidAmount: Double = 0.0,
    ): Order {
        val receivables = if (paidAmount > 0.0) {
            listOf(
                OrderReceivableItem(
                    id = 1,
                    documentId = "rec-1",
                    amountOriginal = paidAmount,
                    amountFinal = paidAmount,
                    installments = 1,
                    status = OrderReceivableItemStatus.PAID,
                    paymentMethod = PaymentMethod(1, "Pix", 1, 0.0, "pix", true),
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
            )
        } else {
            emptyList()
        }

        return Order(
            id = 10,
            serviceName = "Servico",
            status = OrderStatus.PENDING,
            creationDate = "2026-07-22T10:00:00",
            vehiclePrice = total,
            billingDate = "2026-07-22",
            originalAmount = total,
            currentAmount = total - paidAmount,
            isVehicleFinanced = false,
            isVehicleSpecialPlate = false,
            customer = OrderCustomer(10, "Cliente", "12345678901", "11999999999", null),
            vehicleType = VehicleType(1, "Carro"),
            items = listOf(OrderItem(1, total, 0.0, null, "Item", total)),
            receivables = receivables,
            salesman = Salesman(1, "Vendedor"),
        )
    }

    private fun paymentMethod(type: String, online: Boolean) = PaymentMethod(
        id = 1,
        name = type,
        installments = 1,
        interestTax = 0.0,
        paymentType = type,
        isOnlinePayment = online,
    )
}
