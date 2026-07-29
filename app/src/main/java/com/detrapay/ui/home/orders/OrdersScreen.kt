package com.detrapay.ui.home.orders

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.screens.DetailScreen
import com.detrapay.ui.home.orders.screens.InstallmentSimulatorScreen
import com.detrapay.ui.home.orders.screens.InstallmentsScreen
import com.detrapay.ui.home.orders.screens.KeypadScreen
import com.detrapay.ui.home.orders.screens.MethodScreen
import com.detrapay.ui.home.orders.screens.OrdersListScreen
import com.detrapay.ui.home.orders.screens.PaymentResultScreen
import com.detrapay.ui.home.orders.screens.ReviewScreen
import com.detrapay.ui.home.orders.screens.WaitingScreen
import com.detrapay.ui.home.orders.OrderPresentation

@Composable
fun OrdersScreen(
    state: OrdersUiState,
    onAction: (OrderFlowAction) -> Unit,
    onRefresh: () -> Unit = {},
) {
    val local = state.local
    val currentOrder = local.selectedOrder
    val pendingAmount = currentOrder?.let { OrderPresentation.summary(it).missingAmount } ?: 0.0
    val amount = OrderPresentation.paymentAmount(local.paymentDigits)
    val amountError = if (local.paymentDigits.isNotEmpty()) {
        OrderPresentation.paymentAmountError(amount, pendingAmount)
    } else {
        null
    }

    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(activity, local.step) {
        val callback = object : OnBackPressedCallback(local.step != OrderFlowStep.Orders) {
            override fun handleOnBackPressed() {
                if (local.step == OrderFlowStep.Waiting) {
                    onAction(OrderFlowAction.RequestPaymentCancel)
                } else {
                    onAction(OrderFlowAction.Back)
                }
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = OrderFlowColors.Background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (local.step) {
                    OrderFlowStep.Orders -> OrdersListScreen(
                        companyName = state.companyName,
                        companyDocument = state.companyDocument,
                        orders = state.orders,
                        isLoading = state.isLoading,
                        isRefreshing = state.isRefreshing,
                        errorMessage = state.errorMessage,
                        onLogout = { onAction(OrderFlowAction.Logout) },
                        onReload = { onAction(OrderFlowAction.Reload) },
                        onRefresh = onRefresh,
                        onNewOrder = { onAction(OrderFlowAction.NewOrder) },
                        onOpenSimulator = { onAction(OrderFlowAction.OpenSimulator) },
                        onOrderPay = { onAction(OrderFlowAction.OrderPay(it)) },
                        onOrderDetail = { onAction(OrderFlowAction.OrderDetail(it)) },
                    )
                    OrderFlowStep.Detail -> if (currentOrder != null) {
                        DetailScreen(
                            order = currentOrder,
                            onBack = { onAction(OrderFlowAction.Back) },
                            onPay = { onAction(OrderFlowAction.OrderPay(currentOrder)) },
                            onDeletePayment = { onAction(OrderFlowAction.DeletePayment(it)) },
                        )
                    }
                    OrderFlowStep.Amount -> if (
                        currentOrder != null && local.selectedPaymentMethod != null
                    ) {
                        KeypadScreen(
                            order = currentOrder,
                            paymentMethodName = local.selectedPaymentMethod.name,
                            displayAmount = OrderPresentation.paymentDisplayAmount(local.paymentDigits),
                            pendingAmountLabel = OrderPresentation.formatCurrency(pendingAmount),
                            canPay = amountError == null && amount > 0.0,
                            isLoading = local.feesLoading,
                            errorMessage = local.feesError ?: amountError,
                            onBack = { onAction(OrderFlowAction.Back) },
                            onKey = { onAction(OrderFlowAction.Key(it)) },
                            onUsePendingAmount = { onAction(OrderFlowAction.UsePendingAmount) },
                            onContinue = { onAction(OrderFlowAction.ContinueAmount) },
                        )
                    }
                    OrderFlowStep.Method -> if (currentOrder != null) {
                        MethodScreen(
                            order = currentOrder,
                            paymentMethods = state.paymentMethods,
                            onBack = { onAction(OrderFlowAction.Back) },
                            onSelectPaymentMethod = { onAction(OrderFlowAction.SelectPaymentMethod(it)) },
                        )
                    }
                    OrderFlowStep.Installments -> InstallmentsScreen(
                        amount = amount,
                        installments = local.creditInstallments,
                        selectedInstallment = local.selectedInstallment,
                        onBack = { onAction(OrderFlowAction.Back) },
                        onSelectInstallment = { onAction(OrderFlowAction.SelectInstallment(it)) },
                        onContinue = { onAction(OrderFlowAction.ContinueInstallments) },
                    )
                    OrderFlowStep.Review -> if (
                        local.selectedPaymentMethod != null && local.paymentReview != null
                    ) {
                        ReviewScreen(
                            paymentMethod = local.selectedPaymentMethod,
                            review = local.paymentReview,
                            isSubmitting = local.paymentSubmissionInFlight,
                            onBack = { onAction(OrderFlowAction.Back) },
                            onConfirm = { onAction(OrderFlowAction.ConfirmPayment) },
                        )
                    }
                    OrderFlowStep.Waiting -> WaitingScreen(
                        total = local.paymentReview?.amountFinal ?: amount,
                        paymentType = local.selectedPaymentMethod?.paymentType.orEmpty(),
                        paymentState = state.inPagePaymentState,
                        onBack = { onAction(OrderFlowAction.RequestPaymentCancel) },
                        showCancelConfirmation = local.showPaymentCancelConfirmation,
                        onCancelDismiss = { onAction(OrderFlowAction.DismissPaymentCancel) },
                        onCancelConfirm = { onAction(OrderFlowAction.ConfirmPaymentCancel) },
                        onRetry = { onAction(OrderFlowAction.RetryInPagePayment) },
                        onDone = { onAction(OrderFlowAction.FinishInPagePayment) },
                        onCopyPixCode = { onAction(OrderFlowAction.CopyPaymentCode(it)) },
                    )
                    OrderFlowStep.Result -> PaymentResultScreen(
                        total = local.paymentReview?.amountFinal ?: amount,
                        paymentMethod = local.selectedPaymentMethod?.name.orEmpty(),
                        installments = local.paymentReview?.installments ?: 1,
                        transactionId = local.completedPaymentData?.transactionId,
                        onDone = { onAction(OrderFlowAction.FinishInPagePayment) },
                    )
                }

                if (local.showSimulator) {
                    Dialog(
                        onDismissRequest = { onAction(OrderFlowAction.CloseSimulator) },
                        properties = DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false,
                        ),
                    ) {
                        InstallmentSimulatorScreen(
                            amountDigits = local.simulatorAmountDigits,
                            installments = local.simulatorInstallments,
                            selectedInstallment = local.simulatorSelectedInstallment,
                            isLoading = local.simulatorLoading,
                            errorMessage = local.simulatorError,
                            onClose = { onAction(OrderFlowAction.CloseSimulator) },
                            onAmountChange = { onAction(OrderFlowAction.SimulatorAmountChange(it)) },
                            onConsult = { onAction(OrderFlowAction.ConsultSimulator) },
                            onSelectInstallment = { onAction(OrderFlowAction.SelectSimulatorInstallment(it)) },
                            onCopy = { onAction(OrderFlowAction.CopySimulator(it)) },
                            onShare = { onAction(OrderFlowAction.ShareSimulator(it)) },
                        )
                    }
                }
            }
        }
    }
}
