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

internal fun previewOrders(): List<Order> = listOf(
    previewOrder(101, "Marina Costa", 3200.0, OrderStatus.PENDING, 1700.0),
    previewOrder(102, "Rafael Lima", 1280.0, OrderStatus.AUTHORIZED, 0.0),
    previewOrder(103, "Bianca Souza", 860.0, OrderStatus.PAID, 860.0),
)

internal fun previewSellerParityOrders(): List<Order> = listOf(
    previewOrder(516, "Antonio Gerbson", 2570.18, OrderStatus.PENDING, 0.0),
    previewOrder(515, "Antonio Gerbson", 1604.13, OrderStatus.PENDING, 0.0),
    previewOrder(514, "Antonio Gerbson", 1377.20, OrderStatus.PENDING, 0.0),
    previewOrder(513, "Cliente com Nome Longo para Validar Quebra", 12604.13, OrderStatus.PENDING, 1300.0),
)

internal fun previewOrder(
    id: Int,
    customerName: String,
    total: Double,
    status: OrderStatus,
    paidAmount: Double,
): Order {
    val pendingAmount = (total - paidAmount).coerceAtLeast(0.0)
    return Order(
        id = id,
        serviceName = "Venda direta",
        status = status,
        creationDate = "2026-07-22T10:30:00",
        vehiclePrice = total,
        billingDate = "2026-07-22",
        originalAmount = total,
        currentAmount = pendingAmount,
        isVehicleFinanced = false,
        isVehicleSpecialPlate = false,
        customer = OrderCustomer(id, customerName, "12345678901", "11999999999", "cliente$id@detrapay.test"),
        vehicleType = VehicleType(1, "Carro"),
        items = listOf(OrderItem(id, total, 0.0, null, "Servico de transferencia", total)),
        receivables = previewReceivables(id, paidAmount),
        salesman = Salesman(1, "Camila Vendas"),
    )
}

internal fun previewReceivables(
    orderId: Int,
    paidAmount: Double,
): List<OrderReceivableItem> {
    val paid = if (paidAmount > 0.0) listOf(
        previewReceivable(orderId * 10, paidAmount, OrderReceivableItemStatus.PAID, "pix"),
    ) else emptyList()
    return paid
}

internal fun previewPaymentMethods(): List<PaymentMethod> = listOf(
    PaymentMethod(1, "Credito", 1, 0.0, "credito", true),
    PaymentMethod(2, "Debito", 1, 0.0, "debito", true),
    PaymentMethod(3, "Pix", 1, 0.0, "pix", true),
    PaymentMethod(4, "Transferencia Pix", 1, 0.0, "pix_manual", false),
    PaymentMethod(5, "Credito Loja", 1, 0.0, "store_credit", false),
    PaymentMethod(6, "Dinheiro", 1, 0.0, "dinheiro", false),
)

internal fun previewReceivable(
    id: Int,
    amount: Double,
    status: OrderReceivableItemStatus,
    paymentType: String,
): OrderReceivableItem = OrderReceivableItem(
    id = id,
    documentId = "preview-$id",
    amountOriginal = amount,
    amountFinal = amount,
    installments = if (paymentType == "credito") 3 else 1,
    status = status,
    paymentMethod = PaymentMethod(
        id,
        paymentType,
        if (paymentType == "credito") 3 else 1,
        null,
        paymentType,
        paymentType in setOf("credito", "debito", "pix"),
    ),
    paymentDate = if (status == OrderReceivableItemStatus.PAID) "2026-07-22" else null,
    refundDate = null,
    cardLast4 = if (paymentType == "credito") "1234" else null,
    cardHolder = null,
    tax = null,
    cardBrand = if (paymentType == "credito") "Visa" else null,
    authorizationId = null,
    authorizationCode = null,
    pixTxIdCode = null,
)

internal fun previewInstallments(): List<InstallmentFee> = listOf(
    InstallmentFee(1, "125,00", "125,00", "0,00", true),
    InstallmentFee(2, "64,20", "128,40", "3,40", false),
    InstallmentFee(3, "43,50", "130,50", "5,50", false),
    InstallmentFee(4, "33,25", "133,00", "8,00", false),
)
