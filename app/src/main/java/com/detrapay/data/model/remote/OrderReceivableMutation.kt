package com.detrapay.data.model.remote

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class AddOrderReceivableRequest(
    @SerializedName("payment_method_id")
    val paymentMethodId: Int,
    @SerializedName("amount_original")
    val amountOriginal: Double,
    @SerializedName("installments")
    val installments: Int = 1,
    @SerializedName("payment_date")
    val paymentDate: String? = null,
)

data class OrderReceivableMutationResponse(
    @SerializedName("data")
    val data: JsonObject? = null,
    @SerializedName("updatedOrder")
    val updatedOrder: CreateOrderResponse? = null,
)
