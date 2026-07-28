package com.detrapay.ui.home.orders

import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.util.PaymentTypeRules
import kotlin.math.roundToLong

object OrderFlowReducer {

    const val SIMULATOR_REQUEST_BLOCKED_MESSAGE = "Aguarde o carregamento das taxas do checkout."
    const val CHECKOUT_REQUEST_BLOCKED_MESSAGE = "Aguarde o carregamento das taxas do simulador."

    fun showDetail(state: OrderFlowLocalState, order: Order): OrderFlowLocalState {
        return state.copy(selectedOrder = order, step = OrderFlowStep.Detail)
    }

    fun startPayment(state: OrderFlowLocalState, order: Order): OrderFlowLocalState {
        return state.copy(
            selectedOrder = order,
            paymentDigits = "",
            selectedPaymentMethod = null,
            selectedInstallment = 1,
            creditInstallments = emptyList(),
            feesLoading = false,
            feesError = null,
            showSimulator = false,
            feeRequestTarget = null,
            step = OrderFlowStep.Keypad,
        )
    }

    fun usePendingAmount(state: OrderFlowLocalState, order: Order): OrderFlowLocalState {
        val pendingAmount = OrderPresentation.summary(order).missingAmount
        return state.copy(paymentDigits = (pendingAmount * 100).roundToLong().toString())
    }

    fun applyPaymentKey(state: OrderFlowLocalState, key: String): OrderFlowLocalState {
        return state.copy(
            paymentDigits = OrderPresentation.nextPaymentDigits(state.paymentDigits, key),
        )
    }

    fun openMethods(state: OrderFlowLocalState): OrderFlowLocalState {
        return state.copy(step = OrderFlowStep.Method)
    }

    fun selectPaymentMethod(state: OrderFlowLocalState, paymentMethod: PaymentMethod): OrderFlowLocalState {
        val normalized = PaymentTypeRules.normalize(paymentMethod.paymentType)
        return when (normalized) {
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT -> {
                if (state.feeRequestInFlight) {
                    state.copy(
                        selectedPaymentMethod = paymentMethod,
                        selectedInstallment = 1,
                        creditInstallments = emptyList(),
                        feesLoading = state.feeRequestTarget == OrderFeeRequestTarget.CheckoutCredit,
                        feesError = if (state.feeRequestTarget == OrderFeeRequestTarget.CheckoutCredit) {
                            state.feesError
                        } else {
                            state.feesError ?: CHECKOUT_REQUEST_BLOCKED_MESSAGE
                        },
                        step = OrderFlowStep.Credit,
                    )
                } else {
                    state.copy(
                        selectedPaymentMethod = paymentMethod,
                        selectedInstallment = 1,
                        creditInstallments = emptyList(),
                        feesLoading = true,
                        feesError = null,
                        feeRequestTarget = OrderFeeRequestTarget.CheckoutCredit,
                        feeRequestInFlight = true,
                        step = OrderFlowStep.Credit,
                    )
                }
            }
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT -> state.copy(
                selectedPaymentMethod = paymentMethod,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = false,
                feesError = null,
                feeRequestTarget = null,
                step = OrderFlowStep.Debit,
            )
            else -> state.copy(
                selectedPaymentMethod = paymentMethod,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = false,
                feesError = null,
                feeRequestTarget = null,
                step = if (paymentMethod.isOnlinePayment) OrderFlowStep.Waiting else OrderFlowStep.Method,
            )
        }
    }

    fun selectInstallment(state: OrderFlowLocalState, installment: Int): OrderFlowLocalState {
        return state.copy(selectedInstallment = installment)
    }

    fun back(state: OrderFlowLocalState): OrderFlowLocalState {
        val nextStep = when (state.step) {
            OrderFlowStep.Orders -> OrderFlowStep.Orders
            OrderFlowStep.Detail -> OrderFlowStep.Orders
            OrderFlowStep.Keypad -> OrderFlowStep.Orders
            OrderFlowStep.Method -> OrderFlowStep.Keypad
            OrderFlowStep.Credit,
            OrderFlowStep.Debit -> OrderFlowStep.Method
            OrderFlowStep.Waiting -> OrderFlowStep.Method
        }
        val cancelCheckoutFeeRequest =
            state.step == OrderFlowStep.Credit &&
                state.feeRequestTarget == OrderFeeRequestTarget.CheckoutCredit
        return state.copy(
            step = nextStep,
            feesLoading = if (cancelCheckoutFeeRequest) false else state.feesLoading,
            feeRequestTarget = if (cancelCheckoutFeeRequest) null else state.feeRequestTarget,
        )
    }

    fun openSimulator(state: OrderFlowLocalState): OrderFlowLocalState {
        return state.copy(
            showSimulator = true,
            simulatorAmountDigits = "",
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorLoading = false,
            simulatorError = null,
        )
    }

    fun closeSimulator(state: OrderFlowLocalState): OrderFlowLocalState {
        val cancelSimulatorFeeRequest = state.feeRequestTarget == OrderFeeRequestTarget.Simulator
        return state.copy(
            showSimulator = false,
            feeRequestTarget = if (cancelSimulatorFeeRequest) null else state.feeRequestTarget,
            simulatorLoading = false,
            simulatorError = null,
        )
    }

    fun updateSimulatorAmount(state: OrderFlowLocalState, raw: String): OrderFlowLocalState {
        val cancelSimulatorFeeRequest = state.feeRequestTarget == OrderFeeRequestTarget.Simulator
        return state.copy(
            simulatorAmountDigits = raw.filter(Char::isDigit),
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorError = null,
            simulatorLoading = false,
            feeRequestTarget = if (cancelSimulatorFeeRequest) null else state.feeRequestTarget,
        )
    }

    fun startSimulatorLoading(state: OrderFlowLocalState): OrderFlowLocalState {
        if (state.feeRequestInFlight) {
            return state.copy(
                simulatorLoading = false,
                simulatorError = SIMULATOR_REQUEST_BLOCKED_MESSAGE,
            )
        }

        return state.copy(
            feeRequestTarget = OrderFeeRequestTarget.Simulator,
            feeRequestInFlight = true,
            simulatorLoading = true,
            simulatorError = null,
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
        )
    }

    fun simulatorLoaded(
        state: OrderFlowLocalState,
        installments: List<InstallmentFee>,
        emptyMessage: String,
    ): OrderFlowLocalState {
        if (state.feeRequestTarget == null) return discardFeeResponse(state)
        if (state.feeRequestTarget != OrderFeeRequestTarget.Simulator) return state

        return state.copy(
            simulatorLoading = false,
            feeRequestTarget = null,
            feeRequestInFlight = false,
            simulatorInstallments = installments,
            simulatorSelectedInstallment = installments.lastOrNull()?.installmentNumber,
            simulatorError = if (installments.isEmpty()) emptyMessage else null,
        )
    }

    fun simulatorFailed(state: OrderFlowLocalState, message: String): OrderFlowLocalState {
        if (state.feeRequestTarget == null) return discardFeeResponse(state)
        if (state.feeRequestTarget != OrderFeeRequestTarget.Simulator) return state

        return state.copy(
            simulatorLoading = false,
            feeRequestTarget = null,
            feeRequestInFlight = false,
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorError = message,
        )
    }

    fun feesLoaded(
        state: OrderFlowLocalState,
        installments: List<InstallmentFee>,
        emptyMessage: String,
    ): OrderFlowLocalState {
        if (state.feeRequestTarget == null) return discardFeeResponse(state)
        if (state.feeRequestTarget != OrderFeeRequestTarget.CheckoutCredit) return state

        return state.copy(
            feesLoading = false,
            feeRequestTarget = null,
            feeRequestInFlight = false,
            creditInstallments = installments,
            selectedInstallment = installments.firstOrNull()?.installmentNumber ?: 1,
            feesError = if (installments.isEmpty()) emptyMessage else null,
        )
    }

    fun feesFailed(state: OrderFlowLocalState, message: String): OrderFlowLocalState {
        if (state.feeRequestTarget == null) return discardFeeResponse(state)
        if (state.feeRequestTarget != OrderFeeRequestTarget.CheckoutCredit) return state

        return state.copy(
            feesLoading = false,
            feeRequestTarget = null,
            feeRequestInFlight = false,
            creditInstallments = emptyList(),
            feesError = message,
        )
    }

    fun isStaleFeeResponse(state: OrderFlowLocalState): Boolean {
        return state.feeRequestTarget == null
    }

    fun discardFeeResponse(state: OrderFlowLocalState): OrderFlowLocalState {
        return state.copy(feeRequestInFlight = false)
    }
}
