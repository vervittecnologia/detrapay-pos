package com.detrapay.ui.home.orders

import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderPaymentTotalsTest {

    @Test
    fun `declared amount includes pending payments but paid amount does not`() {
        val totals = OrderPaymentTotals.from(
            listOf(
                receivable(status = OrderReceivableItemStatus.PENDING, amountOriginal = 80.0),
                receivable(status = OrderReceivableItemStatus.PAID, amountOriginal = 20.0),
                receivable(status = OrderReceivableItemStatus.CANCELLED, amountOriginal = 30.0),
                receivable(status = OrderReceivableItemStatus.REFUNDED, amountOriginal = 40.0),
            ),
        )

        assertEquals(100.0, totals.declaredAmount, 0.0)
        assertEquals(20.0, totals.paidAmount, 0.0)
    }

    @Test
    fun `received summary uses amount final for paid cash and store credit`() {
        val totals = OrderPaymentTotals.from(
            listOf(
                receivable(
                    status = OrderReceivableItemStatus.PAID,
                    amountOriginal = 80.0,
                    amountFinal = 100.0,
                    paymentType = "cash",
                ),
                receivable(
                    status = OrderReceivableItemStatus.PAID,
                    amountOriginal = 20.0,
                    amountFinal = 25.0,
                    paymentType = "credit",
                ),
            ),
        )

        assertEquals(100.0, totals.paidAmount, 0.0)
        assertEquals(120.0, totals.receivedDisplayAmount, 0.0)
    }

    private fun receivable(
        status: OrderReceivableItemStatus,
        amountOriginal: Double,
        amountFinal: Double = amountOriginal,
        paymentType: String = "credit",
    ) = OrderReceivableItem(
        id = 1,
        documentId = "rec-1",
        amountOriginal = amountOriginal,
        amountFinal = amountFinal,
        installments = 1,
        status = status,
        paymentMethod = PaymentMethod(
            id = 1,
            name = paymentType,
            installments = 1,
            interestTax = 0.0,
            paymentType = paymentType,
            isOnlinePayment = paymentType in setOf("credito", "debito", "pix"),
        ),
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
}
