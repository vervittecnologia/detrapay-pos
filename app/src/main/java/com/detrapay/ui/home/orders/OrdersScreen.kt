package com.detrapay.ui.home.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.detrapay.ui.home.orders.components.OrderFlowColors
import com.detrapay.ui.home.orders.screens.DetailScreen
import com.detrapay.ui.home.orders.screens.InstallmentSimulatorScreen
import com.detrapay.ui.home.orders.screens.InstallmentsScreen
import com.detrapay.ui.home.orders.screens.KeypadScreen
import com.detrapay.ui.home.orders.screens.MethodScreen
import com.detrapay.ui.home.orders.screens.OrdersListScreen
import com.detrapay.ui.home.orders.screens.SellerProfileScreen
import com.detrapay.ui.home.orders.screens.WaitingScreen
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.theme.DetrapayTheme

@Composable
fun OrdersScreen(
    state: OrdersUiState,
    onAction: (OrderFlowAction) -> Unit,
    cameraAvailable: Boolean = true,
    cameraCaptureError: String? = null,
    onTakeOrderPhoto: (Int) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val local = state.local
    val currentOrder = local.selectedOrder
    val amount = OrderPresentation.paymentAmount(local.paymentDigits)

    BackHandler(
        enabled = local.showSimulator || local.step != OrderFlowStep.Orders ||
            state.homeSection == SellerHomeSection.Profile,
    ) {
        onAction(
            when {
                local.showSimulator -> OrderFlowAction.CloseSimulator
                local.step != OrderFlowStep.Orders -> OrderFlowAction.Back
                else -> OrderFlowAction.SelectHomeSection(SellerHomeSection.Orders)
            },
        )
    }

    DetrapayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = OrderFlowColors.Background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (local.step) {
                    OrderFlowStep.Orders -> when (state.homeSection) {
                        SellerHomeSection.Orders -> OrdersListScreen(
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
                            onSectionSelected = {
                                onAction(OrderFlowAction.SelectHomeSection(it))
                            },
                        )
                        SellerHomeSection.Profile -> SellerProfileScreen(
                            companyName = state.companyName,
                            companyDocument = state.companyDocument,
                            dispatcherName = state.dispatcherName,
                            companyLogoKey = state.companyLogoKey,
                            salesmen = state.salesmen,
                            onLogout = { onAction(OrderFlowAction.Logout) },
                            onSectionSelected = {
                                onAction(OrderFlowAction.SelectHomeSection(it))
                            },
                        )
                    }
                    OrderFlowStep.Detail -> if (currentOrder != null) {
                        DetailScreen(
                            order = currentOrder,
                            documentsState = state.orderDocuments,
                            cameraAvailable = cameraAvailable,
                            captureError = cameraCaptureError,
                            onBack = { onAction(OrderFlowAction.Back) },
                            onPay = { onAction(OrderFlowAction.OrderPay(currentOrder)) },
                            onDeletePayment = { onAction(OrderFlowAction.DeletePayment(it)) },
                            onLoadDocuments = { onAction(OrderFlowAction.ReloadDocuments) },
                            onTakePhoto = { onTakeOrderPhoto(currentOrder.id) },
                            onRetryPhotoUpload = { onAction(OrderFlowAction.RetryPhotoUpload) },
                            onDiscardPendingPhoto = { onAction(OrderFlowAction.DiscardPendingPhoto) },
                        )
                    }
                    OrderFlowStep.Amount -> if (
                        currentOrder != null && local.selectedPaymentMethod != null
                    ) {
                        KeypadScreen(
                            displayAmount = OrderPresentation.paymentDisplayAmount(local.paymentDigits),
                            canPay = amount > 0.0,
                            isLoading = local.feesLoading || local.paymentSubmissionInFlight,
                            errorMessage = local.feesError,
                            onBack = { onAction(OrderFlowAction.Back) },
                            onClose = { onAction(OrderFlowAction.ExitPayment) },
                            onKey = { onAction(OrderFlowAction.Key(it)) },
                            onContinue = { onAction(OrderFlowAction.ContinueAmount) },
                        )
                    }
                    OrderFlowStep.Method -> if (currentOrder != null) {
                        MethodScreen(
                            order = currentOrder,
                            paymentMethods = state.paymentMethods,
                            onBack = { onAction(OrderFlowAction.Back) },
                            onClose = { onAction(OrderFlowAction.ExitPayment) },
                            onSelectPaymentMethod = { onAction(OrderFlowAction.SelectPaymentMethod(it)) },
                        )
                    }
                    OrderFlowStep.Installments -> InstallmentsScreen(
                        amount = amount,
                        installments = local.creditInstallments,
                        selectedInstallment = local.selectedInstallment,
                        isLoading = local.feesLoading || local.paymentSubmissionInFlight,
                        errorMessage = local.feesError,
                        onBack = { onAction(OrderFlowAction.Back) },
                        onClose = { onAction(OrderFlowAction.ExitPayment) },
                        onRetry = { onAction(OrderFlowAction.ContinueAmount) },
                        onSelectInstallment = { onAction(OrderFlowAction.SelectInstallment(it)) },
                        onContinue = { onAction(OrderFlowAction.ContinueInstallments) },
                    )
                    OrderFlowStep.Waiting -> WaitingScreen(
                        total = local.paymentReview?.amountFinal ?: amount,
                        installments = local.activePaymentRequest?.installments
                            ?: local.paymentReview?.installments
                            ?: 1,
                        paymentType = local.selectedPaymentMethod?.paymentType.orEmpty(),
                        paymentState = state.inPagePaymentState,
                        onBack = { onAction(OrderFlowAction.Back) },
                        onClose = { onAction(OrderFlowAction.ExitPayment) },
                        onRetry = { onAction(OrderFlowAction.RetryInPagePayment) },
                        onDone = { onAction(OrderFlowAction.FinishInPagePayment) },
                        onCopyPixCode = { onAction(OrderFlowAction.CopyPaymentCode(it)) },
                    )
                }

                if (local.paymentSubmissionInFlight && local.step != OrderFlowStep.Waiting) {
                    Surface(modifier = Modifier.align(Alignment.Center)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Text("Registrando pagamento…", modifier = Modifier.padding(top = 16.dp))
                        }
                    }
                }

                if (local.showSimulator) {
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
