package com.detrapay.ui.home.direct_checkout

import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderCustomer
import com.detrapay.data.model.OrderItem
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DirectCheckoutReducerTest {

    @Test
    fun `startPayment selects order and opens keypad with missing amount digits`() {
        val order = order(total = 2570.18, paidAmount = 1200.0)

        val state = DirectCheckoutReducer.startPayment(DirectCheckoutLocalState(), order)

        assertEquals(order, state.selectedOrder)
        assertEquals(DirectCheckoutStep.Keypad, state.step)
        assertEquals("137018", state.paymentDigits)
        assertEquals("", state.selectedPaymentType)
        assertEquals(1, state.selectedInstallment)
        assertFalse(state.showSimulator)
    }

    @Test
    fun `showDetail selects order and opens detail`() {
        val order = order()

        val state = DirectCheckoutReducer.showDetail(DirectCheckoutLocalState(), order)

        assertEquals(order, state.selectedOrder)
        assertEquals(DirectCheckoutStep.Detail, state.step)
    }

    @Test
    fun `payment key delegates to presentation rules`() {
        val state = DirectCheckoutLocalState(paymentDigits = "12")

        val next = DirectCheckoutReducer.applyPaymentKey(state, "DEL")

        assertEquals("1", next.paymentDigits)
    }

    @Test
    fun `open methods moves from keypad to method`() {
        val state = DirectCheckoutLocalState(step = DirectCheckoutStep.Keypad)

        val next = DirectCheckoutReducer.openMethods(state)

        assertEquals(DirectCheckoutStep.Method, next.step)
    }

    @Test
    fun `select credit resets installment and opens credit`() {
        val state = DirectCheckoutLocalState(selectedInstallment = 4)

        val next = DirectCheckoutReducer.selectPaymentType(state, "credito")

        assertEquals("credito", next.selectedPaymentType)
        assertEquals(1, next.selectedInstallment)
        assertEquals(DirectCheckoutStep.Credit, next.step)
    }

    @Test
    fun `select debit opens debit`() {
        val state = DirectCheckoutLocalState()

        val next = DirectCheckoutReducer.selectPaymentType(state, "debito")

        assertEquals("debito", next.selectedPaymentType)
        assertEquals(DirectCheckoutStep.Debit, next.step)
    }

    @Test
    fun `select manual method opens waiting`() {
        val state = DirectCheckoutLocalState()

        val next = DirectCheckoutReducer.selectPaymentType(state, "pix")

        assertEquals("pix", next.selectedPaymentType)
        assertEquals(DirectCheckoutStep.Waiting, next.step)
    }

    @Test
    fun `back follows current direct checkout step order`() {
        assertEquals(
            DirectCheckoutStep.Orders,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Detail)).step,
        )
        assertEquals(
            DirectCheckoutStep.Orders,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Keypad)).step,
        )
        assertEquals(
            DirectCheckoutStep.Keypad,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Method)).step,
        )
        assertEquals(
            DirectCheckoutStep.Method,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Credit)).step,
        )
        assertEquals(
            DirectCheckoutStep.Method,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Debit)).step,
        )
        assertEquals(
            DirectCheckoutStep.Method,
            DirectCheckoutReducer.back(DirectCheckoutLocalState(step = DirectCheckoutStep.Waiting)).step,
        )
    }

    @Test
    fun `simulator state opens closes and updates amount`() {
        val opened = DirectCheckoutReducer.openSimulator(
            DirectCheckoutLocalState(simulatorAmountDigits = "123", simulatorSelectedInstallment = 2),
        )

        assertEquals(true, opened.showSimulator)
        assertEquals("", opened.simulatorAmountDigits)
        assertNull(opened.simulatorSelectedInstallment)

        val updated = DirectCheckoutReducer.updateSimulatorAmount(opened, "R$ 45,67")

        assertEquals("4567", updated.simulatorAmountDigits)
        assertEquals(emptyList<Any>(), updated.simulatorInstallments)
        assertNull(updated.simulatorSelectedInstallment)

        val closed = DirectCheckoutReducer.closeSimulator(updated)

        assertFalse(closed.showSimulator)
        assertFalse(closed.simulatorRequestActive)
    }

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
                    paymentMethod = PaymentMethod(1, "Pix", 1, 0.0, "pix"),
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
}
