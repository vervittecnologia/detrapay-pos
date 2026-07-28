package com.detrapay.ui.home.orders

sealed interface OrderPaymentRoute {
    data class Online(val request: OrderPaymentRequest) : OrderPaymentRoute
    data class RecordOnly(val request: OrderPaymentRequest) : OrderPaymentRoute
}

object OrderPaymentRouter {
    fun routeFor(request: OrderPaymentRequest): OrderPaymentRoute {
        return if (request.paymentMethod.isOnlinePayment) {
            OrderPaymentRoute.Online(request)
        } else {
            OrderPaymentRoute.RecordOnly(request)
        }
    }
}
