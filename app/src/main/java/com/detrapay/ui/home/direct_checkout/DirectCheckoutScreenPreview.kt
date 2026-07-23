package com.detrapay.ui.home.direct_checkout

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.detrapay.data.model.Order

@Preview(name = "Direct checkout - Orders loaded", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutOrdersLoadedPreview() = PreviewContent(local = DirectCheckoutLocalState(step = DirectCheckoutStep.Orders))

@Preview(name = "Direct checkout - Orders empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutOrdersEmptyPreview() = PreviewContent(emptyList(), local = DirectCheckoutLocalState(step = DirectCheckoutStep.Orders))

@Preview(name = "Direct checkout - Orders error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutOrdersErrorPreview() = PreviewContent(emptyList(), "Falha ao carregar pedidos.", DirectCheckoutLocalState(step = DirectCheckoutStep.Orders))

@Preview(name = "Direct checkout - Detail", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutDetailPreview() = PreviewForOrder(DirectCheckoutStep.Detail)

@Preview(name = "Direct checkout - Keypad", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutKeypadPreview() = PreviewForOrder(DirectCheckoutStep.Keypad)

@Preview(name = "Direct checkout - Method", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutMethodPreview() = PreviewForOrder(DirectCheckoutStep.Method)

@Preview(name = "Direct checkout - Credit loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutCreditLoadingPreview() = PreviewForOrder(DirectCheckoutStep.Credit, selectedPaymentType = "credito", feesLoading = true)

@Preview(name = "Direct checkout - Credit error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutCreditErrorPreview() = PreviewForOrder(DirectCheckoutStep.Credit, selectedPaymentType = "credito", feesError = "Nao foi possivel consultar parcelas.")

@Preview(name = "Direct checkout - Credit loaded", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutCreditLoadedPreview() = PreviewForOrder(DirectCheckoutStep.Credit, selectedPaymentType = "credito", creditInstallments = previewInstallments(), selectedInstallment = 3)

@Preview(name = "Direct checkout - Debit", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutDebitPreview() = PreviewForOrder(DirectCheckoutStep.Debit, selectedPaymentType = "debito")

@Preview(name = "Direct checkout - Waiting", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutWaitingPreview() = PreviewForOrder(DirectCheckoutStep.Waiting, selectedPaymentType = "pix")

@Preview(name = "Direct checkout - Simulator loaded", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutSimulatorPreview() = PreviewContent(
    local = DirectCheckoutLocalState(
        step = DirectCheckoutStep.Orders,
        showSimulator = true,
        simulatorAmountDigits = "50000",
        simulatorInstallments = previewInstallments(),
        simulatorSelectedInstallment = 3,
    ),
)

@Preview(name = "Direct checkout - Simulator empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutSimulatorEmptyPreview() = PreviewContent(
    local = DirectCheckoutLocalState(
        step = DirectCheckoutStep.Orders,
        showSimulator = true,
        simulatorAmountDigits = "50000",
    ),
)

@Preview(name = "Direct checkout - Simulator loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutSimulatorLoadingPreview() = PreviewContent(
    local = DirectCheckoutLocalState(
        step = DirectCheckoutStep.Orders,
        showSimulator = true,
        simulatorAmountDigits = "50000",
        simulatorLoading = true,
    ),
)

@Preview(name = "Direct checkout - Simulator error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DirectCheckoutSimulatorErrorPreview() = PreviewContent(
    local = DirectCheckoutLocalState(
        step = DirectCheckoutStep.Orders,
        showSimulator = true,
        simulatorAmountDigits = "50000",
        simulatorError = "Nao foi possivel consultar parcelas.",
    ),
)

@Composable
private fun PreviewForOrder(
    step: DirectCheckoutStep,
    selectedPaymentType: String = "",
    feesLoading: Boolean = false,
    feesError: String? = null,
    creditInstallments: List<com.detrapay.data.model.remote.InstallmentFee> = emptyList(),
    selectedInstallment: Int = 1,
) {
    PreviewContent(
        local = DirectCheckoutLocalState(
            step = step,
            selectedOrder = previewOrders().first(),
            paymentDigits = if (step == DirectCheckoutStep.Detail) "" else "12500",
            selectedPaymentType = selectedPaymentType,
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
    local: DirectCheckoutLocalState,
) {
    DirectCheckoutScreen(
        state = DirectCheckoutUiState(
            companyName = "Detrapay Motors",
            companyDocument = "12.345.678/0001-90",
            orders = orders,
            isLoading = false,
            errorMessage = errorMessage,
            availablePaymentTypes = listOf("pix", "credito", "debito", "dinheiro"),
            local = local,
        ),
        onAction = {},
    )
}
