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
            selectedInstallment = null,
            creditInstallments = emptyList(),
            feesLoading = false,
            feesError = null,
            paymentReview = null,
            showSimulator = false,
            feeRequestTarget = null,
            activePaymentRequest = null,
            paymentSubmissionInFlight = false,
            step = OrderFlowStep.Method,
        )
    }

    fun usePendingAmount(state: OrderFlowLocalState, order: Order): OrderFlowLocalState {
        if (state.feesLoading) return state
        val pendingAmount = OrderPresentation.summary(order).missingAmount
        return state.copy(
            paymentDigits = (pendingAmount * 100).roundToLong().toString(),
            activePaymentRequest = null,
        )
    }

    fun applyPaymentKey(state: OrderFlowLocalState, key: String): OrderFlowLocalState {
        if (state.feesLoading) return state
        return state.copy(
            paymentDigits = OrderPresentation.nextPaymentDigits(state.paymentDigits, key),
            creditInstallments = emptyList(),
            selectedInstallment = null,
            paymentReview = null,
            feesError = null,
            activePaymentRequest = null,
        )
    }

    fun openMethods(state: OrderFlowLocalState): OrderFlowLocalState {
        return state.copy(step = OrderFlowStep.Method)
    }

    fun selectPaymentMethod(state: OrderFlowLocalState, paymentMethod: PaymentMethod): OrderFlowLocalState {
        return state.copy(
            selectedPaymentMethod = paymentMethod,
            paymentDigits = "",
            selectedInstallment = null,
            creditInstallments = emptyList(),
            feesLoading = false,
            feesError = null,
            activePaymentRequest = null,
            paymentReview = null,
            feeRequestTarget = state.feeRequestTarget,
            step = OrderFlowStep.Amount,
        )
    }

    fun startCheckoutQuote(state: OrderFlowLocalState): OrderFlowLocalState {
        if (state.feeRequestInFlight) {
            return state.copy(
                feesLoading = false,
                feesError = CHECKOUT_REQUEST_BLOCKED_MESSAGE,
            )
        }

        return state.copy(
            feesLoading = true,
            feesError = null,
            paymentReview = null,
            creditInstallments = emptyList(),
            selectedInstallment = null,
            feeRequestTarget = OrderFeeRequestTarget.CheckoutCredit,
            feeRequestInFlight = true,
        )
    }

    fun quoteLoaded(
        state: OrderFlowLocalState,
        installments: List<InstallmentFee>,
        emptyMessage: String,
    ): OrderFlowLocalState {
        if (state.feeRequestTarget == null) return discardFeeResponse(state)
        if (state.feeRequestTarget != OrderFeeRequestTarget.CheckoutCredit) return state

        if (installments.isEmpty()) {
            return state.copy(
                feesLoading = false,
                feeRequestTarget = null,
                feeRequestInFlight = false,
                creditInstallments = emptyList(),
                paymentReview = null,
                feesError = emptyMessage,
            )
        }

        val amount = OrderPresentation.paymentAmount(state.paymentDigits)
        val isCredit = PaymentTypeRules.normalize(state.selectedPaymentMethod?.paymentType) ==
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT
        return if (isCredit) {
            state.copy(
                step = OrderFlowStep.Installments,
                feesLoading = false,
                feeRequestTarget = null,
                feeRequestInFlight = false,
                creditInstallments = installments,
                selectedInstallment = null,
                paymentReview = null,
                feesError = null,
            )
        } else {
            state.copy(
                step = OrderFlowStep.Review,
                feesLoading = false,
                feeRequestTarget = null,
                feeRequestInFlight = false,
                creditInstallments = installments,
                selectedInstallment = installments.first().installmentNumber,
                paymentReview = OrderPresentation.paymentReview(amount, installments.first()),
                feesError = null,
            )
        }
    }

    fun openDirectReview(state: OrderFlowLocalState): OrderFlowLocalState {
        val amount = OrderPresentation.paymentAmount(state.paymentDigits)
        return state.copy(
            step = OrderFlowStep.Review,
            selectedInstallment = null,
            paymentReview = OrderPresentation.directPaymentReview(amount),
            feesLoading = false,
            feesError = null,
        )
    }

    fun openInstallmentReview(state: OrderFlowLocalState): OrderFlowLocalState {
        val installment = state.creditInstallments.firstOrNull {
            it.installmentNumber == state.selectedInstallment
        } ?: return state.copy(feesError = "Selecione uma opcao de parcelamento.")
        return state.copy(
            step = OrderFlowStep.Review,
            paymentReview = OrderPresentation.paymentReview(
                OrderPresentation.paymentAmount(state.paymentDigits),
                installment,
            ),
            feesError = null,
        )
    }

    fun selectInstallment(state: OrderFlowLocalState, installment: Int): OrderFlowLocalState {
        return state.copy(selectedInstallment = installment, activePaymentRequest = null)
    }

    fun back(state: OrderFlowLocalState): OrderFlowLocalState {
        val nextStep = when (state.step) {
            OrderFlowStep.Orders -> OrderFlowStep.Orders
            OrderFlowStep.Detail -> OrderFlowStep.Orders
            OrderFlowStep.Method -> OrderFlowStep.Orders
            OrderFlowStep.Amount -> OrderFlowStep.Method
            OrderFlowStep.Installments -> OrderFlowStep.Amount
            OrderFlowStep.Review -> if (
                PaymentTypeRules.normalize(state.selectedPaymentMethod?.paymentType) ==
                OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT
            ) {
                OrderFlowStep.Installments
            } else {
                OrderFlowStep.Amount
            }
            OrderFlowStep.Waiting -> OrderFlowStep.Review
        }
        val cancelCheckoutFeeRequest =
            state.step == OrderFlowStep.Amount &&
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
        return quoteLoaded(state, installments, emptyMessage)
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
