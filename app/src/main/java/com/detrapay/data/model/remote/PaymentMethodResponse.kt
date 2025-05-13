package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("max_installments")
    val maxInstallments: Int,
    @SerializedName("interest_tax")
    val interestRate: Double?,
)