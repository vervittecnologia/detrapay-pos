package com.detrapay.data.model

import java.io.Serializable

data class PaymentMethod(
    val id: Int,
    val name: String,
    val installments: Int,
    val interestTax: Double?,
    val paymentType: String? = null,
    val isOnlinePayment: Boolean,
) : Serializable
