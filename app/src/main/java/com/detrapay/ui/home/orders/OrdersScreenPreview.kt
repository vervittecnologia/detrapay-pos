package com.detrapay.ui.home.orders

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.detrapay.data.model.Order

@Preview(name = "Orders - Orders loaded", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersOrdersLoadedPreview() = PreviewContent(local = OrderFlowLocalState(step = OrderFlowStep.Orders))

@Preview(name = "Orders - Orders empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersOrdersEmptyPreview() = PreviewContent(emptyList(), local = OrderFlowLocalState(step = OrderFlowStep.Orders))

@Preview(name = "Orders - Orders error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersOrdersErrorPreview() = PreviewContent(emptyList(), "Falha ao carregar pedidos.", OrderFlowLocalState(step = OrderFlowStep.Orders))

@Preview(name = "Orders - Detail", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersDetailPreview() = PreviewForOrder(OrderFlowStep.Detail)

@Preview(name = "Orders - Keypad", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersKeypadPreview() = PreviewForOrder(OrderFlowStep.Keypad)

@Preview(name = "Orders - Method", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersMethodPreview() = PreviewForOrder(OrderFlowStep.Method)

@Preview(name = "Orders - Credit loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersCreditLoadingPreview() = PreviewForOrder(OrderFlowStep.Credit, selectedPaymentType = "credito", feesLoading = true)

@Preview(name = "Orders - Credit error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersCreditErrorPreview() = PreviewForOrder(OrderFlowStep.Credit, selectedPaymentType = "credito", feesError = "Nao foi possivel consultar parcelas.")

@Preview(name = "Orders - Credit loaded", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersCreditLoadedPreview() = PreviewForOrder(OrderFlowStep.Credit, selectedPaymentType = "credito", creditInstallments = previewInstallments(), selectedInstallment = 3)

@Preview(name = "Orders - Debit", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersDebitPreview() = PreviewForOrder(OrderFlowStep.Debit, selectedPaymentType = "debito")

@Preview(name = "Orders - Waiting", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersWaitingPreview() = PreviewForOrder(OrderFlowStep.Waiting, selectedPaymentType = "pix")

@Preview(name = "Orders - Simulator loaded", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersSimulatorPreview() = PreviewContent(
    local = OrderFlowLocalState(
        step = OrderFlowStep.Orders,
        showSimulator = true,
        simulatorAmountDigits = "50000",
        simulatorInstallments = previewInstallments(),
        simulatorSelectedInstallment = 3,
    ),
)

@Preview(name = "Orders - Simulator empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersSimulatorEmptyPreview() = PreviewContent(
    local = OrderFlowLocalState(
        step = OrderFlowStep.Orders,
        showSimulator = true,
        simulatorAmountDigits = "50000",
    ),
)

@Preview(name = "Orders - Simulator loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersSimulatorLoadingPreview() = PreviewContent(
    local = OrderFlowLocalState(
        step = OrderFlowStep.Orders,
        showSimulator = true,
        simulatorAmountDigits = "50000",
        simulatorLoading = true,
    ),
)

@Preview(name = "Orders - Simulator error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OrdersSimulatorErrorPreview() = PreviewContent(
    local = OrderFlowLocalState(
        step = OrderFlowStep.Orders,
        showSimulator = true,
        simulatorAmountDigits = "50000",
        simulatorError = "Nao foi possivel consultar parcelas.",
    ),
)

@Composable
private fun PreviewForOrder(
    step: OrderFlowStep,
    selectedPaymentType: String = "",
    feesLoading: Boolean = false,
    feesError: String? = null,
    creditInstallments: List<com.detrapay.data.model.remote.InstallmentFee> = emptyList(),
    selectedInstallment: Int = 1,
) {
    PreviewContent(
        local = OrderFlowLocalState(
            step = step,
            selectedOrder = previewOrders().first(),
            paymentDigits = if (step == OrderFlowStep.Detail) "" else "12500",
            selectedPaymentMethod = previewPaymentMethods().firstOrNull {
                it.paymentType == selectedPaymentType
            },
            feesLoading = feesLoading,
            feesError = feesError,
            creditInstallments = creditInstallments,
            selectedInstallment = selectedInstallment,
        ),
    )
}

@Composable
private fun PreviewContent(
    orders: List<Order> = previewOrders(),
    errorMessage: String? = null,
    local: OrderFlowLocalState,
) {
    OrdersScreen(
        state = OrdersUiState(
            companyName = "Detrapay Motors",
            companyDocument = "12.345.678/0001-90",
            orders = orders,
            isLoading = false,
            errorMessage = errorMessage,
            paymentMethods = previewPaymentMethods(),
            local = local,
        ),
        onAction = {},
    )
}
