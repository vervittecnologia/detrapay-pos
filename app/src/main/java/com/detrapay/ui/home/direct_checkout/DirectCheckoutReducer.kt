package com.detrapay.ui.home.direct_checkout

import com.detrapay.data.model.Order
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.util.PaymentTypeRules
import kotlin.math.roundToLong

object DirectCheckoutReducer {

    const val SIMULATOR_REQUEST_BLOCKED_MESSAGE = "Aguarde o carregamento das taxas do checkout."
    const val CHECKOUT_REQUEST_BLOCKED_MESSAGE = "Aguarde o carregamento das taxas do simulador."

    fun showDetail(state: DirectCheckoutLocalState, order: Order): DirectCheckoutLocalState {
        return state.copy(selectedOrder = order, step = DirectCheckoutStep.Detail)
    }

    fun startPayment(state: DirectCheckoutLocalState, order: Order): DirectCheckoutLocalState {
        val missingAmount = DirectCheckoutOrderPresentation.summary(order).missingAmount
        return state.copy(
            selectedOrder = order,
            paymentDigits = (missingAmount * 100).roundToLong().toString(),
            selectedPaymentType = "",
            selectedInstallment = 1,
            creditInstallments = emptyList(),
            feesLoading = false,
            feesError = null,
            showSimulator = false,
            feeRequestTarget = null,
            step = DirectCheckoutStep.Keypad,
        )
    }

    fun applyPaymentKey(state: DirectCheckoutLocalState, key: String): DirectCheckoutLocalState {
        return state.copy(
            paymentDigits = DirectCheckoutOrderPresentation.nextPaymentDigits(state.paymentDigits, key),
        )
    }

    fun openMethods(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        return state.copy(step = DirectCheckoutStep.Method)
    }

    fun selectPaymentType(state: DirectCheckoutLocalState, paymentType: String): DirectCheckoutLocalState {
        val normalized = PaymentTypeRules.normalize(paymentType)
        return when (normalized) {
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT -> {
                if (state.feeRequestTarget == DirectCheckoutFeeRequestTarget.Simulator) {
                    state.copy(
                        selectedPaymentType = paymentType,
                        selectedInstallment = 1,
                        creditInstallments = emptyList(),
                        feesLoading = false,
                        feesError = state.feesError ?: CHECKOUT_REQUEST_BLOCKED_MESSAGE,
                        step = DirectCheckoutStep.Credit,
                    )
                } else {
                    state.copy(
                        selectedPaymentType = paymentType,
                        selectedInstallment = 1,
                        creditInstallments = emptyList(),
                        feesLoading = true,
                        feesError = null,
                        feeRequestTarget = DirectCheckoutFeeRequestTarget.CheckoutCredit,
                        step = DirectCheckoutStep.Credit,
                    )
                }
            }
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT -> state.copy(
                selectedPaymentType = paymentType,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = false,
                feesError = null,
                feeRequestTarget = null,
                step = DirectCheckoutStep.Debit,
            )
            else -> state.copy(
                selectedPaymentType = paymentType,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = false,
                feesError = null,
                feeRequestTarget = null,
                step = DirectCheckoutStep.Waiting,
            )
        }
    }

    fun selectInstallment(state: DirectCheckoutLocalState, installment: Int): DirectCheckoutLocalState {
        return state.copy(selectedInstallment = installment)
    }

    fun back(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        val nextStep = when (state.step) {
            DirectCheckoutStep.Orders -> DirectCheckoutStep.Orders
            DirectCheckoutStep.Detail -> DirectCheckoutStep.Orders
            DirectCheckoutStep.Keypad -> DirectCheckoutStep.Orders
            DirectCheckoutStep.Method -> DirectCheckoutStep.Keypad
            DirectCheckoutStep.Credit,
            DirectCheckoutStep.Debit -> DirectCheckoutStep.Method
            DirectCheckoutStep.Waiting -> DirectCheckoutStep.Method
        }
        val cancelCheckoutFeeRequest =
            state.step == DirectCheckoutStep.Credit &&
                state.feeRequestTarget == DirectCheckoutFeeRequestTarget.CheckoutCredit
        return state.copy(
            step = nextStep,
            feesLoading = if (cancelCheckoutFeeRequest) false else state.feesLoading,
            feeRequestTarget = if (cancelCheckoutFeeRequest) null else state.feeRequestTarget,
        )
    }

    fun openSimulator(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        return state.copy(
            showSimulator = true,
            simulatorAmountDigits = "",
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorLoading = false,
            simulatorError = null,
        )
    }

    fun closeSimulator(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        val cancelSimulatorFeeRequest = state.feeRequestTarget == DirectCheckoutFeeRequestTarget.Simulator
        return state.copy(
            showSimulator = false,
            feeRequestTarget = if (cancelSimulatorFeeRequest) null else state.feeRequestTarget,
            simulatorLoading = false,
            simulatorError = null,
        )
    }

    fun updateSimulatorAmount(state: DirectCheckoutLocalState, raw: String): DirectCheckoutLocalState {
        val cancelSimulatorFeeRequest = state.feeRequestTarget == DirectCheckoutFeeRequestTarget.Simulator
        return state.copy(
            simulatorAmountDigits = raw.filter(Char::isDigit),
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorError = null,
            simulatorLoading = false,
            feeRequestTarget = if (cancelSimulatorFeeRequest) null else state.feeRequestTarget,
        )
    }

    fun startSimulatorLoading(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        if (state.feeRequestTarget == DirectCheckoutFeeRequestTarget.CheckoutCredit) {
            return state.copy(
                simulatorLoading = false,
                simulatorError = SIMULATOR_REQUEST_BLOCKED_MESSAGE,
            )
        }
        if (state.feeRequestTarget == DirectCheckoutFeeRequestTarget.Simulator) return state

        return state.copy(
            feeRequestTarget = DirectCheckoutFeeRequestTarget.Simulator,
            simulatorLoading = true,
            simulatorError = null,
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
        )
    }

    fun simulatorLoaded(
        state: DirectCheckoutLocalState,
        installments: List<InstallmentFee>,
        emptyMessage: String,
    ): DirectCheckoutLocalState {
        if (state.feeRequestTarget != DirectCheckoutFeeRequestTarget.Simulator) return state

        return state.copy(
            simulatorLoading = false,
            feeRequestTarget = null,
            simulatorInstallments = installments,
            simulatorSelectedInstallment = installments.lastOrNull()?.installmentNumber,
            simulatorError = if (installments.isEmpty()) emptyMessage else null,
        )
    }

    fun simulatorFailed(state: DirectCheckoutLocalState, message: String): DirectCheckoutLocalState {
        if (state.feeRequestTarget != DirectCheckoutFeeRequestTarget.Simulator) return state

        return state.copy(
            simulatorLoading = false,
            feeRequestTarget = null,
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorError = message,
        )
    }

    fun feesLoaded(
        state: DirectCheckoutLocalState,
        installments: List<InstallmentFee>,
        emptyMessage: String,
    ): DirectCheckoutLocalState {
        if (state.feeRequestTarget != DirectCheckoutFeeRequestTarget.CheckoutCredit) return state

        return state.copy(
            feesLoading = false,
            feeRequestTarget = null,
            creditInstallments = installments,
            selectedInstallment = installments.firstOrNull()?.installmentNumber ?: 1,
            feesError = if (installments.isEmpty()) emptyMessage else null,
        )
    }

    fun feesFailed(state: DirectCheckoutLocalState, message: String): DirectCheckoutLocalState {
        if (state.feeRequestTarget != DirectCheckoutFeeRequestTarget.CheckoutCredit) return state

        return state.copy(
            feesLoading = false,
            feeRequestTarget = null,
            creditInstallments = emptyList(),
            feesError = message,
        )
    }
}
