package com.detrapay.data.model.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ConfirmPaymentRequest(
    @SerializedName("authorization_code")
    val authorizationCode: String,
    @SerializedName("card_brand")
    val cardBrand: String,
    @SerializedName("card_last4")
    val cardLast4: String,
    @SerializedName("card_holder")
    val cardHolder: String,
    @SerializedName("transaction_log")
    val transactionLog: JsonElement?,
    @SerializedName("payment_date")
    val paymentDate: String? = null,
)
