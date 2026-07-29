package com.detrapay.ui.home.orders

import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.orders.OrderPaymentRequest
import com.detrapay.ui.state.UIState

enum class OrderFlowStep {
    Orders,
    Detail,
    Method,
    Amount,
    Installments,
    Review,
    Waiting,
}

data class OrderPaymentReview(
    val amountOriginal: Double,
    val amountFinal: Double,
    val feeAmount: Double,
    val installments: Int,
    val installmentValue: Double,
)

enum class OrderFeeRequestTarget {
    CheckoutCredit,
    Simulator,
}

data class OrderFlowLocalState(
    val step: OrderFlowStep = OrderFlowStep.Orders,
    val selectedOrder: Order? = null,
    val paymentDigits: String = "",
    val selectedPaymentMethod: PaymentMethod? = null,
    val selectedInstallment: Int? = null,
    val creditInstallments: List<InstallmentFee> = emptyList(),
    val feesLoading: Boolean = false,
    val feesError: String? = null,
    val paymentReview: OrderPaymentReview? = null,
    val showSimulator: Boolean = false,
    val simulatorAmountDigits: String = "",
    val simulatorInstallments: List<InstallmentFee> = emptyList(),
    val simulatorSelectedInstallment: Int? = null,
    val simulatorLoading: Boolean = false,
    val simulatorError: String? = null,
    val feeRequestTarget: OrderFeeRequestTarget? = null,
    val feeRequestInFlight: Boolean = false,
    val activePaymentRequest: OrderPaymentRequest? = null,
    val paymentSubmissionInFlight: Boolean = false,
)

data class OrdersUiState(
    val companyName: String,
    val companyDocument: String,
    val orders: List<Order>,
    val isLoading: Boolean,
    val isRefreshing: Boolean = false,
    val errorMessage: String?,
    val paymentMethods: List<PaymentMethod>,
    val local: OrderFlowLocalState,
    val inPagePaymentState: UIState<PaymentData> = UIState.Idle(),
    val orderDocuments: OrderDocumentsUiState = OrderDocumentsUiState(),
)

sealed interface OrderFlowAction {
    data object Logout : OrderFlowAction
    data object Reload : OrderFlowAction
    data object NewOrder : OrderFlowAction
    data class OrderPay(val order: Order) : OrderFlowAction
    data class OrderDetail(val order: Order) : OrderFlowAction
    data class DeletePayment(val receivable: OrderReceivableItem) : OrderFlowAction
    data object ReloadDocuments : OrderFlowAction
    data object RetryPhotoUpload : OrderFlowAction
    data object DiscardPendingPhoto : OrderFlowAction
    data object Back : OrderFlowAction
    data object ExitPayment : OrderFlowAction
    data class Key(val value: String) : OrderFlowAction
    data object UsePendingAmount : OrderFlowAction
    data object OpenMethods : OrderFlowAction
    data class SelectPaymentMethod(val paymentMethod: PaymentMethod) : OrderFlowAction
    data object ContinueAmount : OrderFlowAction
    data class SelectInstallment(val installment: Int) : OrderFlowAction
    data object ContinueInstallments : OrderFlowAction
    data object ConfirmPayment : OrderFlowAction
    data object RetryInPagePayment : OrderFlowAction
    data object FinishInPagePayment : OrderFlowAction
    data class CopyPaymentCode(val text: String) : OrderFlowAction
    data object OpenSimulator : OrderFlowAction
    data object CloseSimulator : OrderFlowAction
    data class SimulatorAmountChange(val raw: String) : OrderFlowAction
    data object ConsultSimulator : OrderFlowAction
    data class SelectSimulatorInstallment(val installment: Int) : OrderFlowAction
    data class CopySimulator(val text: String) : OrderFlowAction
    data class ShareSimulator(val text: String) : OrderFlowAction
}

sealed interface OrderFlowEffect {
    data object ShowLogoutConfirmation : OrderFlowEffect
    data object NavigateToRegistration : OrderFlowEffect
    data class CopyPaymentText(val text: String) : OrderFlowEffect
    data class CopySimulatorText(val text: String) : OrderFlowEffect
    data class ShareSimulatorText(val text: String) : OrderFlowEffect
    data class ShowToast(val message: String, val long: Boolean = true) : OrderFlowEffect
    data class ShowSessionExpired(val exception: Exception) : OrderFlowEffect
}
