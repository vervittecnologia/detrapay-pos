package com.detrapay.ui.home.direct_checkout

import com.detrapay.ui.home.simplified.DirectCheckoutPendingPayment
import com.detrapay.ui.util.PaymentTypeRules

sealed interface DirectCheckoutPaymentRoute {
    data class StartInPagePayment(val pendingPayment: DirectCheckoutPendingPayment) : DirectCheckoutPaymentRoute
    data class ConfirmManually(val pendingPayment: DirectCheckoutPendingPayment) : DirectCheckoutPaymentRoute
}

object DirectCheckoutPaymentRouter {
    fun routeFor(pendingPayment: DirectCheckoutPendingPayment): DirectCheckoutPaymentRoute {
        return if (PaymentTypeRules.requiresTerminalApproval(pendingPayment.paymentType)) {
            DirectCheckoutPaymentRoute.StartInPagePayment(pendingPayment)
        } else {
            DirectCheckoutPaymentRoute.ConfirmManually(pendingPayment)
        }
    }
}
