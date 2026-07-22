package com.detrapay.ui.home.direct_checkout

import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.simplified.DirectCheckoutPendingPayment

enum class DirectCheckoutStep {
    Orders,
    Detail,
    Keypad,
    Method,
    Credit,
    Debit,
    Waiting,
}

enum class DirectCheckoutFeeRequestTarget {
    CheckoutCredit,
    Simulator,
}

data class DirectCheckoutLocalState(
    val step: DirectCheckoutStep = DirectCheckoutStep.Orders,
    val selectedOrder: Order? = null,
    val paymentDigits: String = "",
    val selectedPaymentType: String = "",
    val selectedInstallment: Int = 1,
    val creditInstallments: List<InstallmentFee> = emptyList(),
    val feesLoading: Boolean = false,
    val feesError: String? = null,
    val showSimulator: Boolean = false,
    val simulatorAmountDigits: String = "",
    val simulatorInstallments: List<InstallmentFee> = emptyList(),
    val simulatorSelectedInstallment: Int? = null,
    val simulatorLoading: Boolean = false,
    val simulatorError: String? = null,
    val feeRequestTarget: DirectCheckoutFeeRequestTarget? = null,
)

data class DirectCheckoutUiState(
    val companyName: String,
    val companyDocument: String,
    val orders: List<Order>,
    val isLoading: Boolean,
    val errorMessage: String?,
    val availablePaymentTypes: List<String>,
    val local: DirectCheckoutLocalState,
)

sealed interface DirectCheckoutAction {
    data object Logout : DirectCheckoutAction
    data object Reload : DirectCheckoutAction
    data object NewOrder : DirectCheckoutAction
    data class OrderPay(val order: Order) : DirectCheckoutAction
    data class OrderDetail(val order: Order) : DirectCheckoutAction
    data object Back : DirectCheckoutAction
    data class Key(val value: String) : DirectCheckoutAction
    data object OpenMethods : DirectCheckoutAction
    data class SelectPaymentType(val paymentType: String) : DirectCheckoutAction
    data class SelectInstallment(val installment: Int) : DirectCheckoutAction
    data object ContinueCredit : DirectCheckoutAction
    data object ContinueDebit : DirectCheckoutAction
    data object OpenSimulator : DirectCheckoutAction
    data object CloseSimulator : DirectCheckoutAction
    data class SimulatorAmountChange(val raw: String) : DirectCheckoutAction
    data object ConsultSimulator : DirectCheckoutAction
    data class SelectSimulatorInstallment(val installment: Int) : DirectCheckoutAction
    data class CopySimulator(val text: String) : DirectCheckoutAction
    data class ShareSimulator(val text: String) : DirectCheckoutAction
}

sealed interface DirectCheckoutEffect {
    data object ShowLogoutConfirmation : DirectCheckoutEffect
    data object NavigateToRegistration : DirectCheckoutEffect
    data class OpenPaymentDialog(
        val pendingPayment: DirectCheckoutPendingPayment,
        val onResult: (PaymentData?) -> Unit,
    ) : DirectCheckoutEffect
    data class ConfirmManualPayment(
        val pendingPayment: DirectCheckoutPendingPayment,
        val paymentData: PaymentData,
    ) : DirectCheckoutEffect
    data class CopySimulatorText(val text: String) : DirectCheckoutEffect
    data class ShareSimulatorText(val text: String) : DirectCheckoutEffect
    data class ShowToast(val message: String, val long: Boolean = true) : DirectCheckoutEffect
    data class ShowSessionExpired(val exception: Exception) : DirectCheckoutEffect
}
