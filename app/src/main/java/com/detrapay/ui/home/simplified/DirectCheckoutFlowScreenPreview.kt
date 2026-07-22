package com.detrapay.ui.home.simplified

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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
import com.detrapay.ui.home.direct_checkout.DirectCheckoutLocalState
import com.detrapay.ui.home.direct_checkout.DirectCheckoutScreen
import com.detrapay.ui.home.direct_checkout.DirectCheckoutStep
import com.detrapay.ui.home.direct_checkout.DirectCheckoutUiState
import com.detrapay.ui.home.direct_checkout.screens.OrdersScreen

@Preview(name = "Direct checkout - Orders", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutOrdersPreview() {
    DirectCheckoutPreviewContent(step = DirectCheckoutStep.Orders)
}

@Preview(name = "Seller orders - device collapsed", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SellerOrdersDeviceCollapsedPreview() {
    SellerOrdersPreviewContent()
}

@Preview(name = "Seller orders - device search", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SellerOrdersDeviceSearchPreview() {
    SellerOrdersPreviewContent(
        initialShowSearch = true,
        initialQuery = "Antonio",
    )
}

@Preview(name = "Seller orders - device FAB menu", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SellerOrdersDeviceFabMenuPreview() {
    SellerOrdersPreviewContent(initialShowFabMenu = true)
}

@Preview(name = "Seller orders - tall phone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SellerOrdersTallPhonePreview() {
    SellerOrdersPreviewContent()
}

@Preview(name = "Direct checkout - Detail", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutDetailPreview() {
    DirectCheckoutPreviewContent(step = DirectCheckoutStep.Detail)
}

@Preview(name = "Direct checkout - Keypad", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutKeypadPreview() {
    DirectCheckoutPreviewContent(step = DirectCheckoutStep.Keypad, paymentDigits = "12500")
}

@Preview(name = "Direct checkout - Method", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutMethodPreview() {
    DirectCheckoutPreviewContent(step = DirectCheckoutStep.Method, paymentDigits = "12500")
}

@Preview(name = "Direct checkout - Credit", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutCreditPreview() {
    DirectCheckoutPreviewContent(
        step = DirectCheckoutStep.Credit,
        paymentDigits = "12500",
        selectedPaymentType = "credito",
    )
}

@Preview(name = "Direct checkout - Simulator", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutSimulatorPreview() {
    DirectCheckoutPreviewContent(
        step = DirectCheckoutStep.Orders,
        showSimulator = true,
        simulatorAmountDigits = "50000",
        simulatorSelectedInstallment = 3,
    )
}

@Composable
private fun SellerOrdersPreviewContent(
    initialShowSearch: Boolean = false,
    initialQuery: String = "",
    initialShowFabMenu: Boolean = false,
) {
    OrdersScreen(
        orders = previewSellerParityOrders(),
        isLoading = false,
        errorMessage = null,
        onReload = {},
        onNewOrder = {},
        onOpenSimulator = {},
        onOrderPay = {},
        onOrderDetail = {},
        initialShowSearch = initialShowSearch,
        initialQuery = initialQuery,
        initialShowFabMenu = initialShowFabMenu,
    )
}

@Composable
private fun DirectCheckoutPreviewContent(
    step: DirectCheckoutStep,
    paymentDigits: String = "",
    selectedPaymentType: String = "",
    showSimulator: Boolean = false,
    simulatorAmountDigits: String = "",
    simulatorSelectedInstallment: Int? = null,
) {
    val orders = previewOrders()
    val selectedOrder = orders.first()
    val installments = previewInstallments()

    DirectCheckoutScreen(
        state = DirectCheckoutUiState(
            companyName = "Detrapay Motors",
            companyDocument = "12.345.678/0001-90",
            orders = orders,
            isLoading = false,
            errorMessage = null,
            availablePaymentTypes = listOf("pix", "credito", "debito", "dinheiro"),
            local = DirectCheckoutLocalState(
                step = step,
                selectedOrder = selectedOrder,
                paymentDigits = paymentDigits,
                selectedPaymentType = selectedPaymentType,
                selectedInstallment = simulatorSelectedInstallment ?: 1,
                creditInstallments = installments,
                showSimulator = showSimulator,
                simulatorAmountDigits = simulatorAmountDigits,
                simulatorInstallments = installments,
                simulatorSelectedInstallment = simulatorSelectedInstallment,
            ),
        ),
        onAction = {},
    )}

private fun previewOrders(): List<Order> {
    return listOf(
        previewOrder(
            id = 101,
            customerName = "Marina Costa",
            total = 3200.0,
            status = OrderStatus.PENDING,
            paidAmount = 1700.0,
        ),
        previewOrder(
            id = 102,
            customerName = "Rafael Lima",
            total = 1280.0,
            status = OrderStatus.AUTHORIZED,
            paidAmount = 0.0,
        ),
        previewOrder(
            id = 103,
            customerName = "Bianca Souza",
            total = 860.0,
            status = OrderStatus.PAID,
            paidAmount = 860.0,
        ),
    )
}

private fun previewSellerParityOrders(): List<Order> {
    return listOf(
        previewOrder(
            id = 516,
            customerName = "Antonio Gerbson",
            total = 2570.18,
            status = OrderStatus.PENDING,
            paidAmount = 0.0,
        ),
        previewOrder(
            id = 515,
            customerName = "Antonio Gerbson",
            total = 1604.13,
            status = OrderStatus.PENDING,
            paidAmount = 0.0,
        ),
        previewOrder(
            id = 514,
            customerName = "Antonio Gerbson",
            total = 1377.20,
            status = OrderStatus.PENDING,
            paidAmount = 0.0,
        ),
        previewOrder(
            id = 513,
            customerName = "Cliente com Nome Longo para Validar Quebra",
            total = 12604.13,
            status = OrderStatus.PENDING,
            paidAmount = 1300.0,
        ),
    )
}

private fun previewOrder(
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
        customer = OrderCustomer(
            id = id,
            name = customerName,
            cpfCnpj = "12345678901",
            phoneNumber = "11999999999",
            email = "cliente$id@detrapay.test",
        ),
        vehicleType = VehicleType(1, "Carro"),
        items = listOf(
            OrderItem(
                id = id,
                totalPrice = total,
                discount = 0.0,
                salesItemId = null,
                name = "Servico de transferencia",
                price = total,
            ),
        ),
        receivables = previewReceivables(id, paidAmount, pendingAmount),
        salesman = Salesman(1, "Camila Vendas"),
    )
}

private fun previewReceivables(
    orderId: Int,
    paidAmount: Double,
    pendingAmount: Double,
): List<OrderReceivableItem> {
    val paid = if (paidAmount > 0.0) {
        listOf(
            previewReceivable(
                id = orderId * 10,
                amount = paidAmount,
                status = OrderReceivableItemStatus.PAID,
                paymentType = "pix",
            ),
        )
    } else {
        emptyList()
    }

    val pending = if (pendingAmount > 0.0) {
        listOf(
            previewReceivable(
                id = orderId * 10 + 1,
                amount = pendingAmount,
                status = OrderReceivableItemStatus.PENDING,
                paymentType = "credito",
            ),
        )
    } else {
        emptyList()
    }

    return paid + pending
}

private fun previewReceivable(
    id: Int,
    amount: Double,
    status: OrderReceivableItemStatus,
    paymentType: String,
): OrderReceivableItem {
    return OrderReceivableItem(
        id = id,
        documentId = "preview-$id",
        amountOriginal = amount,
        amountFinal = amount,
        installments = if (paymentType == "credito") 3 else 1,
        status = status,
        paymentMethod = PaymentMethod(
            id = id,
            name = paymentType,
            installments = if (paymentType == "credito") 3 else 1,
            interestTax = null,
            paymentType = paymentType,
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
}

private fun previewInstallments(): List<InstallmentFee> {
    return listOf(
        InstallmentFee(1, "125,00", "125,00", "0,00", true),
        InstallmentFee(2, "64,20", "128,40", "3,40", false),
        InstallmentFee(3, "43,50", "130,50", "5,50", false),
        InstallmentFee(4, "33,25", "133,00", "8,00", false),
    )
}
