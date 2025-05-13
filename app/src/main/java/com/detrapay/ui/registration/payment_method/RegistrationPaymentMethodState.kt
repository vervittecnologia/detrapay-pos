package com.detrapay.ui.registration.payment_method

import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.SimulationPayment

data class RegistrationPaymentMethodInitialState(
    val paymentMethods: List<PaymentMethod>,
    val payments: List<SimulationPayment>
)

data class RegistrationPaymentMethodCreateOrderState(
    val order: Order
)