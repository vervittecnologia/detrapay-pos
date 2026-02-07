package com.detrapay.data.model.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateOrderReceivableRequest(
    @SerializedName("status_in")
    val status: String,
    @SerializedName("payment_date")
    val paymentDate: String,
    @SerializedName("authorization_id")
    val authorizationId: String,
    @SerializedName("authorization_code")
    val authorizationCode: String,
    @SerializedName("card_brand")
    val cardBrand: String?,
    @SerializedName("card_holder")
    val cardHolder: String?,
    @SerializedName("card_last4")
    val cardLast4: String?,
    @SerializedName("pix_tx_id_code")
    val pixTxIdCode: String?,
    @SerializedName("transaction_log")
    val transactionLog: String?,
//    @SerializedName("refund_date")
//    val refundDate: String?
)
@Serializable
data class UpdateOrderReceivableRequestDataWrapper(
    @SerializedName("data")
    val data: UpdateOrderReceivableRequest,
)


@Serializable
data class RefundOrderReceivableRequest(
    @SerializedName("status_in")
    val status: String,
    @SerializedName("refund_date")
    val refundDate: String,
)

@Serializable
data class RefundOrderReceivableRequestDataWrapper(
    @SerializedName("data")
    val data: RefundOrderReceivableRequest,
)
