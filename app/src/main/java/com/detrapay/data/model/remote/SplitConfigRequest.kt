package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName

data class SplitConfigRequest(
    @SerializedName("receivable_id")
    val receivableId: Int? = null,
    val serial: String,
    val description: String? = null,
    @SerializedName("payment_attempt_id")
    val paymentAttemptId: String? = null,
)
