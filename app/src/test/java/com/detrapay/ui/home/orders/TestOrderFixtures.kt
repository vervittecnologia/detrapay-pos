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

object TestOrderFixtures {
    fun order(
        total: Double = 100.0,
        receivables: List<OrderReceivableItem> = emptyList(),
    ): Order {
        return Order(
            id = 10,
            serviceName = "Servico",
            status = OrderStatus.PENDING,
            creationDate = "2026-07-22T10:00:00",
            vehiclePrice = total,
            billingDate = "2026-07-22",
            originalAmount = total,
            currentAmount = total,
            isVehicleFinanced = false,
            isVehicleSpecialPlate = false,
            customer = OrderCustomer(10, "Cliente", "12345678901", "11999999999", null),
            vehicleType = VehicleType(1, "Carro"),
            items = listOf(OrderItem(1, total, 0.0, null, "Item", total)),
            receivables = receivables,
            salesman = Salesman(1, "Vendedor"),
        )
    }

    fun receivable(paymentType: String): OrderReceivableItem {
        return OrderReceivableItem(
            id = 1,
            documentId = "doc-1",
            amountOriginal = 100.0,
            amountFinal = 100.0,
            installments = 1,
            status = OrderReceivableItemStatus.PENDING,
            paymentMethod = PaymentMethod(1, paymentType, 1, 0.0, paymentType, paymentType in setOf("credito", "debito", "pix")),
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
}
