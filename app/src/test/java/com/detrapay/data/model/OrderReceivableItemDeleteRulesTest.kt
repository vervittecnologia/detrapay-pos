package com.detrapay.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderReceivableItemDeleteRulesTest {

    @Test
    fun `paid offline payment can be deleted`() {
        assertTrue(receivable(online = false, status = OrderReceivableItemStatus.PAID).canBeDeleted())
    }

    @Test
    fun `pending offline payment can be deleted`() {
        assertTrue(receivable(online = false, status = OrderReceivableItemStatus.PENDING).canBeDeleted())
    }

    @Test
    fun `online payment cannot be deleted in any payable status`() {
        assertFalse(receivable(online = true, status = OrderReceivableItemStatus.PENDING).canBeDeleted())
        assertFalse(receivable(online = true, status = OrderReceivableItemStatus.PAID).canBeDeleted())
    }

    @Test
    fun `refunded or cancelled offline payment cannot be deleted`() {
        assertFalse(receivable(online = false, status = OrderReceivableItemStatus.REFUNDED).canBeDeleted())
        assertFalse(receivable(online = false, status = OrderReceivableItemStatus.CANCELLED).canBeDeleted())
    }

    private fun receivable(
        online: Boolean,
        status: OrderReceivableItemStatus,
    ) = OrderReceivableItem(
        id = 20,
        documentId = "receivable-20",
        amountOriginal = 100.0,
        amountFinal = 100.0,
        installments = 1,
        status = status,
        paymentMethod = PaymentMethod(
            id = 2,
            name = if (online) "Pix" else "Dinheiro",
            installments = 1,
            interestTax = 0.0,
            paymentType = if (online) "pix" else "cash",
            isOnlinePayment = online,
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
