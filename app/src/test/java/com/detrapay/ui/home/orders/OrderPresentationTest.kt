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
import com.detrapay.ui.home.orders.components.simulatorShareText
import com.detrapay.ui.util.PaymentTypeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderPresentationTest {

    @Test
    fun `review method label uses selected credit installments instead of configured name`() {
        val credit = PaymentMethod(
            id = 1,
            name = "Crédito 1x",
            installments = 1,
            interestTax = 0.0,
            paymentType = "credito",
            isOnlinePayment = true,
        )

        assertEquals("Crédito 5x", OrderPresentation.paymentReviewMethodLabel(credit, 5))
    }

    @Test
    fun `all orders includes every status sorted by newest id`() {
        val orders = listOf(
            order(id = 1, total = 100.0, status = OrderStatus.PENDING),
            order(id = 2, total = 100.0, status = OrderStatus.CANCELLED),
            order(id = 3, total = 100.0, status = OrderStatus.COMPLETED),
            order(id = 4, total = 100.0, status = OrderStatus.PAID),
            order(id = 5, total = 100.0, status = OrderStatus.AUTHORIZED),
        )

        val visible = OrderPresentation.allOrders(orders)

        assertEquals(listOf(5, 4, 3, 2, 1), visible.map { it.id })
    }

    @Test
    fun `summary uses registered label and ignores cancelled receivables`() {
        val summary = OrderPresentation.summary(
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
        val summary = OrderPresentation.summary(
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
    fun `seller status label uses settled and pending badges from balance`() {
        val settled = order(
            id = 11,
            total = 100.0,
            status = OrderStatus.PENDING,
            receivables = listOf(receivable(amount = 100.0, status = OrderReceivableItemStatus.PAID)),
        )
        val pending = order(id = 12, total = 100.0, status = OrderStatus.PENDING)

        assertEquals("Quitado", OrderPresentation.sellerStatusLabel(settled))
        assertEquals("Pendente", OrderPresentation.sellerStatusLabel(pending))
    }

    @Test
    fun `blank payment input is zero and never falls back to pending balance`() {
        assertEquals(0.0, OrderPresentation.paymentAmount(""), 0.0)
        assertEquals("R$ 0,00", OrderPresentation.paymentDisplayAmount(""))
    }

    @Test
    fun `typed payment input uses only entered digits`() {
        assertEquals(1_000.0, OrderPresentation.paymentAmount("100000"), 0.0)
        assertEquals("R$ 1.000,00", OrderPresentation.paymentDisplayAmount("100000"))
    }

    @Test
    fun `payment method selection requires exact installment match`() {
        val methods = listOf(
            paymentMethod(id = 1, installments = 1),
            paymentMethod(id = 2, installments = 3),
        )

        assertEquals(2, OrderPresentation.exactPaymentMethod(methods, "pix", 3)?.id)
        assertEquals(null, OrderPresentation.exactPaymentMethod(methods, "pix", 6))
    }

    @Test
    fun `payment method types keep one deterministic representative per normalized type`() {
        val methods = listOf(
            paymentMethod(id = 14, installments = 14, paymentType = "credito"),
            paymentMethod(id = 10, installments = 10, paymentType = "credit"),
            paymentMethod(id = 30, installments = 1, paymentType = "pix"),
            paymentMethod(id = 32, installments = 1, paymentType = "pix"),
            paymentMethod(id = 31, installments = 1, paymentType = "pix_manual"),
        )

        val types = OrderPresentation.paymentMethodTypes(methods)

        assertEquals(listOf(10, 30, 31), types.map(PaymentMethod::id))
        assertEquals(
            listOf("credito", "pix", "pix_manual"),
            types.map { PaymentTypeRules.normalize(it.paymentType) },
        )
    }

    @Test
    fun `payment method type label does not expose installment specific backend name`() {
        val method = paymentMethod(
            id = 1,
            installments = 1,
            paymentType = "credito",
        ).copy(name = "Crédito 1x")

        assertEquals("Crédito", OrderPresentation.paymentMethodTypeLabel(method))
    }

    @Test
    fun `store paid installment keeps displayed total equal to checkout amount`() {
        val presentation = OrderPresentation.storePaidInstallment(amount = 25.67, installments = 3)

        assertEquals("R$ 8,56", presentation.installmentValueLabel)
        assertEquals("R$ 25,67", presentation.totalValueLabel)
        assertEquals("Taxas por conta da loja", presentation.feePayerLabel)
    }

    @Test
    fun `share text includes original total and installments with interest`() {
        val text = simulatorShareText(
            amount = 100.0,
            installments = listOf(
                InstallmentFee(
                    installmentNumber = 3,
                    installmentValue = "36.00",
                    totalValue = "108.00",
                    interestValue = "8.00",
                    noInterest = false,
                ),
            ),
        )

        assertTrue(text.contains("Valor original: R$ 100,00"))
        assertTrue(text.contains("Total com juros: R$ 108,00"))
        assertTrue(text.contains("3x de R$ 36,00 com juros"))
    }

    @Test
    fun `review uses quoted total fee and installment`() {
        val review = OrderPresentation.paymentReview(
            amountOriginal = 100.0,
            installment = InstallmentFee(
                installmentNumber = 3,
                installmentValue = "36.00",
                totalValue = "108.00",
                interestValue = "8.00",
                noInterest = false,
            ),
        )

        assertEquals(108.0, review.amountFinal, 0.0)
        assertEquals(8.0, review.feeAmount, 0.0)
        assertEquals(3, review.installments)
        assertEquals(36.0, review.installmentValue, 0.0)
    }

    @Test
    fun `direct review keeps confirmed total equal to entered amount`() {
        val review = OrderPresentation.directPaymentReview(25.67)

        assertEquals(25.67, review.amountOriginal, 0.0)
        assertEquals(25.67, review.amountFinal, 0.0)
        assertEquals(0.0, review.feeAmount, 0.0)
        assertEquals(1, review.installments)
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
        isOnlinePayment = paymentType in setOf("credito", "debito", "pix"),
    )
}
