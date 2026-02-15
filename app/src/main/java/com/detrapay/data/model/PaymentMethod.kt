package com.detrapay.data.model

import java.io.Serializable

data class PaymentMethod(
    val id: Int,
    val name: String,
    val maxInstallments: Int,
    val interestTax: Double?,
    val paymentType: String? = null
): Serializable
