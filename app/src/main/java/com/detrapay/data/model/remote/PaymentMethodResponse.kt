package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class PaymentMethodResponse(
    val id: Int,
    val documentId: String,
    val name: String,
    @SerializedName("is_online_payment")
    val isOnlinePayment: Boolean,
    @SerializedName("max_installments")
    val maxInstallments: Int,
    @SerializedName("interest_tax")
    val interestTax: Double,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("transaction_fee_rate")
    val transactionFeeRate: Double,
    @SerializedName("transaction_fixed_fee")
    val transactionFixedFee: Double,
    val paymentType: String,
    val createdAt: String,
    val updatedAt: String,
    val publishedAt: String,
    val locale: String?
)
