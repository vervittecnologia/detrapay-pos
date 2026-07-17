package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class PaymentMethodListResponse(
    val data: List<PaymentMethodResponse>
)

data class PaymentMethodResponse(
    val id: Int,
    val documentId: String? = null,
    val name: String,
    @SerializedName("is_online_payment")
    val isOnlinePayment: Boolean? = null,
    @SerializedName(value = "installments", alternate = ["max_installments", "maxInstallments"])
    val installments: Int? = null,
    @SerializedName(value = "interest_tax", alternate = ["interestTax"])
    val interestTax: Double? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
    @SerializedName("transaction_fee_rate")
    val transactionFeeRate: Double? = null,
    @SerializedName("transaction_fixed_fee")
    val transactionFixedFee: Double? = null,
    @SerializedName(value = "paymentType", alternate = ["payment_type"])
    val paymentType: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val publishedAt: String? = null,
    val locale: String? = null
)
