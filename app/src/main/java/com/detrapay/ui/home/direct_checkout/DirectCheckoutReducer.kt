package com.detrapay.ui.home.direct_checkout

import com.detrapay.data.model.Order
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.util.PaymentTypeRules
import kotlin.math.roundToLong

object DirectCheckoutReducer {

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
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT -> state.copy(
                selectedPaymentType = paymentType,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = true,
                feesError = null,
                simulatorRequestActive = false,
                step = DirectCheckoutStep.Credit,
            )
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT -> state.copy(
                selectedPaymentType = paymentType,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = false,
                feesError = null,
                step = DirectCheckoutStep.Debit,
            )
            else -> state.copy(
                selectedPaymentType = paymentType,
                selectedInstallment = 1,
                creditInstallments = emptyList(),
                feesLoading = false,
                feesError = null,
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
        return state.copy(step = nextStep)
    }

    fun openSimulator(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        return state.copy(
            showSimulator = true,
            simulatorRequestActive = false,
            simulatorAmountDigits = "",
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorLoading = false,
            simulatorError = null,
        )
    }

    fun closeSimulator(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        return state.copy(
            showSimulator = false,
            simulatorRequestActive = false,
            simulatorLoading = false,
            simulatorError = null,
        )
    }

    fun updateSimulatorAmount(state: DirectCheckoutLocalState, raw: String): DirectCheckoutLocalState {
        return state.copy(
            simulatorAmountDigits = raw.filter(Char::isDigit),
            simulatorInstallments = emptyList(),
            simulatorSelectedInstallment = null,
            simulatorError = null,
            simulatorRequestActive = false,
        )
    }

    fun startSimulatorLoading(state: DirectCheckoutLocalState): DirectCheckoutLocalState {
        return state.copy(
            simulatorRequestActive = true,
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
        return state.copy(
            simulatorLoading = false,
            simulatorRequestActive = false,
            simulatorInstallments = installments,
            simulatorSelectedInstallment = installments.lastOrNull()?.installmentNumber,
            simulatorError = if (installments.isEmpty()) emptyMessage else null,
        )
    }

    fun simulatorFailed(state: DirectCheckoutLocalState, message: String): DirectCheckoutLocalState {
        return state.copy(
            simulatorLoading = false,
            simulatorRequestActive = false,
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
        return state.copy(
            feesLoading = false,
            creditInstallments = installments,
            selectedInstallment = installments.firstOrNull()?.installmentNumber ?: 1,
            feesError = if (installments.isEmpty()) emptyMessage else null,
        )
    }

    fun feesFailed(state: DirectCheckoutLocalState, message: String): DirectCheckoutLocalState {
        return state.copy(
            feesLoading = false,
            creditInstallments = emptyList(),
            feesError = message,
        )
    }
}
