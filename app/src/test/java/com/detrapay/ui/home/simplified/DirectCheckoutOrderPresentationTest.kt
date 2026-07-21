package com.detrapay.ui.home.simplified

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
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectCheckoutOrderPresentationTest {

    @Test
    fun `pending orders excludes cancelled completed and settled orders`() {
        val orders = listOf(
            order(id = 1, total = 100.0, status = OrderStatus.PENDING),
            order(id = 2, total = 100.0, status = OrderStatus.CANCELLED),
            order(id = 3, total = 100.0, status = OrderStatus.COMPLETED),
            order(
                id = 4,
                total = 100.0,
                receivables = listOf(receivable(amount = 100.0, status = OrderReceivableItemStatus.PAID)),
            ),
            order(
                id = 5,
                total = 100.0,
                receivables = listOf(receivable(amount = 60.0, status = OrderReceivableItemStatus.CANCELLED)),
            ),
        )

        val pending = DirectCheckoutOrderPresentation.pendingOrders(orders)

        assertEquals(listOf(5, 1), pending.map { it.id })
    }

    @Test
    fun `summary uses registered label and ignores cancelled receivables`() {
        val summary = DirectCheckoutOrderPresentation.summary(
            order(
                id = 7,
                total = 200.0,
                receivables = listOf(
                    receivable(amount = 80.0, status = OrderReceivableItemStatus.PENDING),
                    receivable(amount = 50.0, status = OrderReceivableItemStatus.CANCELLED),
                ),
            )
        )

        assertEquals(80.0, summary.registeredAmount, 0.0)
        assertEquals(120.0, summary.missingAmount, 0.0)
        assertEquals("Registrado R$ 80,00", summary.registeredLabel)
        assertEquals("Valor pendente R$ 120,00", summary.pendingValueLabel)
        assertEquals("Falta R$ 120,00", summary.missingLabel)
        assertTrue(summary.hasPendingBalance)
    }

    @Test
    fun `summary reports settled order without pending balance`() {
        val summary = DirectCheckoutOrderPresentation.summary(
            order(
                id = 8,
                total = 100.0,
                receivables = listOf(receivable(amount = 100.0, status = OrderReceivableItemStatus.PAID)),
            )
        )

        assertEquals("Fechado", summary.missingLabel)
        assertFalse(summary.hasPendingBalance)
    }

    @Test
    fun `created receivable returns only a distinguishably new matching receivable`() {
        val oldOrder = order(
            id = 9,
            total = 200.0,
            receivables = listOf(receivable(id = 11, amount = 80.0, status = OrderReceivableItemStatus.PENDING)),
        )
        val newOrder = oldOrder.copy(
            receivables = oldOrder.receivables + receivable(
                id = 12,
                documentId = "rec-12",
                amount = 80.0,
                status = OrderReceivableItemStatus.PENDING,
            ),
        )

        val created = DirectCheckoutOrderPresentation.createdReceivable(
            oldOrder = oldOrder,
            newOrder = newOrder,
            paymentMethod = paymentMethod(),
            amount = 80.0,
        )

        assertEquals(12, created?.id)
    }

    @Test
    fun `created receivable returns null when backend response is ambiguous`() {
        val oldOrder = order(
            id = 10,
            total = 200.0,
            receivables = listOf(receivable(id = 11, amount = 80.0, status = OrderReceivableItemStatus.PENDING)),
        )

        val created = DirectCheckoutOrderPresentation.createdReceivable(
            oldOrder = oldOrder,
            newOrder = oldOrder,
            paymentMethod = paymentMethod(),
            amount = 80.0,
        )

        assertEquals(null, created)
    }

    @Test
    fun `payment method selection requires exact installment match`() {
        val methods = listOf(
            paymentMethod(id = 1, installments = 1),
            paymentMethod(id = 2, installments = 3),
        )

        assertEquals(2, DirectCheckoutOrderPresentation.exactPaymentMethod(methods, "pix", 3)?.id)
        assertEquals(null, DirectCheckoutOrderPresentation.exactPaymentMethod(methods, "pix", 6))
    }

    private fun order(
        id: Int,
        total: Double,
        status: OrderStatus = OrderStatus.PENDING,
        receivables: List<OrderReceivableItem> = emptyList(),
    ) = Order(
        id = id,
        serviceName = "Servico",
        status = status,
        creationDate = "2026-07-17T10:00:00",
        vehiclePrice = total,
        billingDate = "2026-07-17",
        originalAmount = total,
        currentAmount = total,
        isVehicleFinanced = false,
        isVehicleSpecialPlate = false,
        customer = OrderCustomer(
            id = id,
            name = "Cliente $id",
            cpfCnpj = "12345678901",
            phoneNumber = "11999999999",
            email = null,
        ),
        vehicleType = VehicleType(1, "Carro"),
        items = listOf(OrderItem(1, total, 0.0, null, "Item", total)),
        receivables = receivables,
        salesman = Salesman(1, "Vendedor"),
    )

    private fun receivable(
        id: Int = 1,
        documentId: String = "rec-1",
        amount: Double,
        status: OrderReceivableItemStatus,
    ) = OrderReceivableItem(
        id = id,
        documentId = documentId,
        amountOriginal = amount,
        amountFinal = amount,
        installments = 1,
        status = status,
        paymentMethod = paymentMethod(),
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

    private fun paymentMethod(
        id: Int = 1,
        installments: Int = 1,
        paymentType: String = "pix",
    ) = PaymentMethod(
        id = id,
        name = "Pix",
        installments = installments,
        interestTax = 0.0,
        paymentType = paymentType,
    )
}
