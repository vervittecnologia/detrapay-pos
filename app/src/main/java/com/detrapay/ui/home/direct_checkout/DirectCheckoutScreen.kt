package com.detrapay.ui.home.direct_checkout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.direct_checkout.screens.CreditScreen
import com.detrapay.ui.home.direct_checkout.screens.DebitScreen
import com.detrapay.ui.home.direct_checkout.screens.DetailScreen
import com.detrapay.ui.home.direct_checkout.screens.InstallmentSimulatorScreen
import com.detrapay.ui.home.direct_checkout.screens.KeypadScreen
import com.detrapay.ui.home.direct_checkout.screens.MethodScreen
import com.detrapay.ui.home.direct_checkout.screens.OrdersScreen
import com.detrapay.ui.home.direct_checkout.screens.WaitingScreen
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation

@Composable
fun DirectCheckoutScreen(
    state: DirectCheckoutUiState,
    onAction: (DirectCheckoutAction) -> Unit,
) {
    val local = state.local
    val currentOrder = local.selectedOrder
    val pendingAmount = currentOrder?.let { DirectCheckoutOrderPresentation.summary(it).missingAmount } ?: 0.0
    val amount = DirectCheckoutOrderPresentation.paymentAmount(local.paymentDigits, pendingAmount)

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DirectCheckoutColors.Background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (local.step) {
                    DirectCheckoutStep.Orders -> OrdersScreen(
                        companyName = state.companyName,
                        companyDocument = state.companyDocument,
                        orders = state.orders,
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        onLogout = { onAction(DirectCheckoutAction.Logout) },
                        onReload = { onAction(DirectCheckoutAction.Reload) },
                        onNewOrder = { onAction(DirectCheckoutAction.NewOrder) },
                        onOpenSimulator = { onAction(DirectCheckoutAction.OpenSimulator) },
                        onOrderPay = { onAction(DirectCheckoutAction.OrderPay(it)) },
                        onOrderDetail = { onAction(DirectCheckoutAction.OrderDetail(it)) },
                    )
                    DirectCheckoutStep.Detail -> if (currentOrder != null) {
                        DetailScreen(
                            order = currentOrder,
                            onBack = { onAction(DirectCheckoutAction.Back) },
                            onPay = { onAction(DirectCheckoutAction.OrderPay(currentOrder)) },
                        )
                    }
                    DirectCheckoutStep.Keypad -> if (currentOrder != null) {
                        KeypadScreen(
                            order = currentOrder,
                            displayAmount = DirectCheckoutOrderPresentation.paymentDisplayAmount(
                                local.paymentDigits,
                                pendingAmount,
                            ),
                            onBack = { onAction(DirectCheckoutAction.Back) },
                            onKey = { onAction(DirectCheckoutAction.Key(it)) },
                            onPay = { onAction(DirectCheckoutAction.OpenMethods) },
                        )
                    }
                    DirectCheckoutStep.Method -> if (currentOrder != null) {
                        MethodScreen(
                            order = currentOrder,
                            amount = amount,
                            availablePaymentTypes = state.availablePaymentTypes,
                            onBack = { onAction(DirectCheckoutAction.Back) },
                            onSelectPaymentType = { onAction(DirectCheckoutAction.SelectPaymentType(it)) },
                        )
                    }
                    DirectCheckoutStep.Credit -> CreditScreen(
                        amount = amount,
                        installments = local.creditInstallments,
                        selectedInstallment = local.selectedInstallment,
                        isLoading = local.feesLoading,
                        errorMessage = local.feesError,
                        onBack = { onAction(DirectCheckoutAction.Back) },
                        onSelectInstallment = { onAction(DirectCheckoutAction.SelectInstallment(it)) },
                        onContinue = { onAction(DirectCheckoutAction.ContinueCredit) },
                    )
                    DirectCheckoutStep.Debit -> DebitScreen(
                        amount = amount,
                        onBack = { onAction(DirectCheckoutAction.Back) },
                        onContinue = { onAction(DirectCheckoutAction.ContinueDebit) },
                    )
                    DirectCheckoutStep.Waiting -> WaitingScreen(
                        total = amount,
                        paymentType = local.selectedPaymentType,
                        onBack = { onAction(DirectCheckoutAction.Back) },
                    )
                }

                if (local.showSimulator) {
                    InstallmentSimulatorScreen(
                        amountDigits = local.simulatorAmountDigits,
                        installments = local.simulatorInstallments,
                        selectedInstallment = local.simulatorSelectedInstallment,
                        isLoading = local.simulatorLoading,
                        errorMessage = local.simulatorError,
                        onClose = { onAction(DirectCheckoutAction.CloseSimulator) },
                        onAmountChange = { onAction(DirectCheckoutAction.SimulatorAmountChange(it)) },
                        onConsult = { onAction(DirectCheckoutAction.ConsultSimulator) },
                        onSelectInstallment = { onAction(DirectCheckoutAction.SelectSimulatorInstallment(it)) },
                        onCopy = { onAction(DirectCheckoutAction.CopySimulator(it)) },
                        onShare = { onAction(DirectCheckoutAction.ShareSimulator(it)) },
                    )
                }
            }
        }
    }
}
