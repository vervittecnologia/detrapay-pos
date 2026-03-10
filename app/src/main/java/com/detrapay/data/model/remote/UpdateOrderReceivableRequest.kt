package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class UpdateOrderReceivableRequest(
    @SerializedName("refund_date")
    val refundDate: String? = null,
    @SerializedName("status")
    val status: String
)
