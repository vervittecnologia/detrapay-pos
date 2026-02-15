package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class RefundOrderReceivableRequest(
    @SerializedName("refund_date")
    val refundDate: String,
    @SerializedName("status")
    val status: String
)
