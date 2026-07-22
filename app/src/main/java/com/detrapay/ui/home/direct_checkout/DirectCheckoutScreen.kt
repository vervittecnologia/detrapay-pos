package com.detrapay.ui.home.direct_checkout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.detrapay.ui.home.direct_checkout.components.DirectCheckoutColors
import com.detrapay.ui.home.simplified.DirectCheckoutFlowScreen

@Composable
fun DirectCheckoutScreen(
    state: DirectCheckoutUiState,
    onAction: (DirectCheckoutAction) -> Unit,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DirectCheckoutColors.Background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DirectCheckoutFlowScreen(
                    companyName = state.companyName,
                    companyDocument = state.companyDocument,
                    orders = state.orders,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    availablePaymentTypes = state.availablePaymentTypes,
                    step = com.detrapay.ui.home.simplified.DirectCheckoutStep.valueOf(state.local.step.name),
                    selectedOrder = state.local.selectedOrder,
                    paymentDigits = state.local.paymentDigits,
                    selectedPaymentType = state.local.selectedPaymentType,
                    creditInstallments = state.local.creditInstallments,
                    selectedInstallment = state.local.selectedInstallment,
                    isFeesLoading = state.local.feesLoading,
                    feesError = state.local.feesError,
                    showSimulator = state.local.showSimulator,
                    simulatorAmountDigits = state.local.simulatorAmountDigits,
                    simulatorInstallments = state.local.simulatorInstallments,
                    simulatorSelectedInstallment = state.local.simulatorSelectedInstallment,
                    isSimulatorLoading = state.local.simulatorLoading,
                    simulatorError = state.local.simulatorError,
                    onLogout = { onAction(DirectCheckoutAction.Logout) },
                    onReload = { onAction(DirectCheckoutAction.Reload) },
                    onNewOrder = { onAction(DirectCheckoutAction.NewOrder) },
                    onOrderPay = { onAction(DirectCheckoutAction.OrderPay(it)) },
                    onOrderDetail = { onAction(DirectCheckoutAction.OrderDetail(it)) },
                    onBack = { onAction(DirectCheckoutAction.Back) },
                    onKey = { onAction(DirectCheckoutAction.Key(it)) },
                    onOpenMethods = { onAction(DirectCheckoutAction.OpenMethods) },
                    onSelectPaymentType = { onAction(DirectCheckoutAction.SelectPaymentType(it)) },
                    onSelectInstallment = { onAction(DirectCheckoutAction.SelectInstallment(it)) },
                    onContinueCredit = { onAction(DirectCheckoutAction.ContinueCredit) },
                    onContinueDebit = { onAction(DirectCheckoutAction.ContinueDebit) },
                    onOpenSimulator = { onAction(DirectCheckoutAction.OpenSimulator) },
                    onCloseSimulator = { onAction(DirectCheckoutAction.CloseSimulator) },
                    onSimulatorAmountChange = { onAction(DirectCheckoutAction.SimulatorAmountChange(it)) },
                    onConsultSimulator = { onAction(DirectCheckoutAction.ConsultSimulator) },
                    onSelectSimulatorInstallment = { onAction(DirectCheckoutAction.SelectSimulatorInstallment(it)) },
                    onCopySimulator = { onAction(DirectCheckoutAction.CopySimulator(it)) },
                    onShareSimulator = { onAction(DirectCheckoutAction.ShareSimulator(it)) },
                )
            }
        }
    }
}
